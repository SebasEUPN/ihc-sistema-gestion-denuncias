#!/usr/bin/env bash
# Pruebas de extremo a extremo contra los dos prototipos en marcha.
set -u
SD=http://localhost:8080; PC=http://localhost:8081
FALLAS=0
SQL() { docker run --rm --network host -e PGPASSWORD=postgres postgres:18 psql -h localhost -U postgres bd_denuncias -At -c "$1"; }
ok()   { echo "  [OK] $1"; }
mal()  { echo "  [FALLA] $1"; FALLAS=$((FALLAS+1)); }
igual() { if [ "$2" = "$3" ]; then ok "$1 = $2"; else mal "$1: esperado '$3', obtenido '$2'"; fi; }
login() { curl -s -o /tmp/login.json -w '%{http_code}' -c "$1" -H 'Content-Type: application/json' -d "{\"codigo\":\"$2\",\"password\":\"Demo1234\",\"rol\":\"$3\"}" $SD/api/auth/login; }
J='Content-Type: application/json'

echo "== 1. Estructura de la base ($ESCENARIO)"
SQL "select table_name || ': ' || string_agg(column_name, ', ' order by ordinal_position) from information_schema.columns where table_schema='public' and table_name in ('usuario','policia','fiscal','admin','copia_seguridad','configuracion_copia','actividad_programada','registro_actividad','disposicion_fiscal') group by table_name order by table_name"
echo "-- Claves foráneas hacia policia, fiscal y admin:"
SQL "select conrelid::regclass || ' -> ' || confrelid::regclass || '  (' || conname || ')' from pg_constraint where contype='f' and confrelid::regclass::text in ('policia','fiscal','admin') order by 1"
igual "columnas de perfil que quedan en usuario" "$(SQL "select count(*) from information_schema.columns where table_name='usuario' and column_name in ('rango','dependencia','institucion')")" 0
igual "policías (usuario vs tabla policia)" "$(SQL "select count(*) from policia")" "$(SQL "select count(*) from usuario where rol='POLICIA'")"
igual "fiscales (usuario vs tabla fiscal)" "$(SQL "select count(*) from fiscal")" "$(SQL "select count(*) from usuario where rol='FISCAL'")"
igual "administradores (usuario vs tabla admin)" "$(SQL "select count(*) from admin")" "$(SQL "select count(*) from usuario where rol='ADMIN'")"
igual "total por perfil" "$(SQL "select (select count(*) from policia)||'/'||(select count(*) from fiscal)||'/'||(select count(*) from admin)")" "73/25/5"
igual "actividades sin responsable" "$(SQL "select count(*) from actividad_programada where (id_policia is null) = (id_fiscal is null)")" 0
igual "actividades policiales/fiscales" "$(SQL "select count(id_policia)||'/'||count(id_fiscal) from actividad_programada")" "4/4"
igual "disposiciones sin fiscal o sin policía" "$(SQL "select count(*) from disposicion_fiscal where id_fiscal is null or id_policia is null")" 0
igual "registros de auditoría de usuarios existentes sin enlazar" "$(SQL "select count(*) from registro_actividad r join usuario u on upper(u.codigo)=upper(r.codigo_usuario) where r.id_policia is null and r.id_fiscal is null and r.id_admin is null")" 0
if [ "$ESCENARIO" = "migrada" ]; then
  igual "copia anterior migrada (admin, estado)" "$(SQL "select u.codigo||'/'||c.estado||'/'||c.nombre from copia_seguridad c join admin a on a.id_usuario=c.id_admin join usuario u on u.id_usuario=a.id_usuario where c.archivo is null order by c.id_copia limit 1")" "ADM-0001/Completada/Backup_001"
fi

echo "== 2. Perfiles desde la aplicación"
igual "login administrador" "$(login /tmp/adm.txt ADM-0001 admin)" 200
curl -s -b /tmp/adm.txt $SD/api/usuarios > /tmp/usuarios.json
igual "usuarios listados" "$(jq length /tmp/usuarios.json)" 103
igual "PNP-2045" "$(jq -r '.[]|select(.codigo=="PNP-2045")|.institucion+" | "+.rango+" | "+.dependencia' /tmp/usuarios.json)" "PNP | Suboficial de Primera | Comisaría PNP San Isidro"
igual "PNP-3001" "$(jq -r '.[]|select(.codigo=="PNP-3001")|.institucion+" | "+.rango+" | "+.dependencia' /tmp/usuarios.json)" "PNP | Alférez | Policía Nacional del Perú"
igual "FN-1023" "$(jq -r '.[]|select(.codigo=="FN-1023")|.institucion+" | "+.rango+" | "+.dependencia' /tmp/usuarios.json)" "Fiscalía | Fiscal Provincial | Segunda Fiscalía Provincial Penal"
igual "FN-1072" "$(jq -r '.[]|select(.codigo=="FN-1072")|.institucion+" | "+.rango+" | "+.dependencia' /tmp/usuarios.json)" "Fiscalía | Fiscal Superior | Fiscalía Superior Penal de Apelaciones"
igual "ADM-0001" "$(jq -r '.[]|select(.codigo=="ADM-0001")|.institucion+" | "+.rango+" | "+.dependencia' /tmp/usuarios.json)" "Administración | Administrador del sistema | Oficina de Tecnologías de la Información"
igual "ADM-0003" "$(jq -r '.[]|select(.codigo=="ADM-0003")|.institucion+" | "+.rango' /tmp/usuarios.json)" "Administración | Administrador de base de datos"
igual "registrar funcionario nuevo" "$(curl -s -o /tmp/nuevo.json -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"rol":"policia","nombres":"Prueba","apellidos":"Automática","dni":"70000001","fechaNacimiento":"1990-05-10","telefono":"999888777","correo":"prueba@pnp.gob.pe","institucion":"PNP","dependencia":"Comisaría de Prueba","codigo":"PNP-9001","password":"Demo1234","habilitado":true}' $SD/api/usuarios)" 200
cat /tmp/nuevo.json | head -c 300; echo
igual "fila en policia del nuevo funcionario" "$(SQL "select p.dependencia from policia p join usuario u on u.id_usuario=p.id_usuario where u.codigo='PNP-9001'")" "Comisaría de Prueba"
igual "deshabilitar funcionario" "$(curl -s -o /tmp/e.json -w '%{http_code}' -b /tmp/adm.txt -X PATCH -H "$J" -d '{"habilitado":false}' $SD/api/usuarios/PNP-9001/estado)" 200
igual "sigue con su fila en policia" "$(SQL "select count(*) from policia p join usuario u on u.id_usuario=p.id_usuario where u.codigo='PNP-9001' and not u.habilitado")" 1
igual "auditoría" "$(curl -s -o /tmp/aud.json -w '%{http_code}' -b /tmp/adm.txt $SD/api/admin/auditoria)" 200
igual "último registro enlazado con admin" "$(SQL "select (id_admin is not null) from registro_actividad where codigo_usuario='ADM-0001' order by id_registro desc limit 1")" t
igual "estadísticas del administrador" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/adm.txt $SD/api/admin/estadisticas)" 200

igual "login policía" "$(login /tmp/pnp.txt PNP-2045 policia)" 200
igual "agenda policial" "$(curl -s -b /tmp/pnp.txt $SD/api/agenda | jq -r 'length|tostring' )/$(curl -s -b /tmp/pnp.txt $SD/api/agenda | jq -r '.[0].perfil')" "4/POLICIA"
igual "denuncias (policía)" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/pnp.txt $SD/api/denuncias)" 200
igual "panel (policía)" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/pnp.txt $SD/api/panel/indicadores)" 200
igual "login fiscal" "$(login /tmp/fn.txt FN-1023 fiscal)" 200
igual "agenda fiscal" "$(curl -s -b /tmp/fn.txt $SD/api/agenda | jq -r 'length|tostring')/$(curl -s -b /tmp/fn.txt $SD/api/agenda | jq -r '.[0].perfil')" "4/FISCAL"
igual "disposiciones" "$(curl -s -b /tmp/fn.txt $SD/api/disposiciones | jq length)" 3
igual "panel (fiscal)" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/fn.txt $SD/api/panel/indicadores)" 200
igual "fiscal nuevo FN-1060 puede entrar" "$(login /tmp/fn2.txt FN-1060 fiscal)" 200
igual "policía no puede ver copias" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/pnp.txt $SD/api/admin/respaldos)" 403

echo "== 3. Copias de seguridad"
igual "configuración inicial" "$(curl -s -b /tmp/adm.txt $SD/api/admin/respaldos/configuracion | jq -r '(.activo|tostring)+"/"+.frecuencia+"/"+.hora+"/"+(.diasConservacion|tostring)')" "false/DIARIA/02:00/30"
igual "crear copia completa" "$(curl -s -o /tmp/copia.json -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"nombre":"Backup_Prueba_001","tipo":"COMPLETA","descripcion":"Copia de la prueba automática"}' $SD/api/admin/respaldos)" 200
cat /tmp/copia.json; echo
ID=$(jq -r .id /tmp/copia.json)
igual "estado de la copia" "$(jq -r '.estado+"/"+(.disponible|tostring)+"/"+.administrador' /tmp/copia.json)" "Completada/true/ADM-0001 · Administrador Principal"
igual "nombre repetido rechazado" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"nombre":"Backup_Prueba_001","tipo":"COMPLETA"}' $SD/api/admin/respaldos)" 409
igual "tipo inválido rechazado" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"nombre":"Otra_copia","tipo":"XYZ"}' $SD/api/admin/respaldos)" 400
igual "copia solo de usuarios" "$(curl -s -o /tmp/copia2.json -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"nombre":"Backup_Usuarios","tipo":"USUARIOS","descripcion":""}' $SD/api/admin/respaldos)" 200
igual "descargar archivo" "$(curl -s -o /tmp/copia.json.gz -w '%{http_code}' -b /tmp/adm.txt $SD/api/admin/respaldos/$ID/archivo)" 200
echo "  tablas y filas del archivo: $(gunzip -c /tmp/copia.json.gz | jq -c '.tablas|to_entries|map({(.key):(.value|length)})|add')"
igual "copia enlazada con admin en la base" "$(SQL "select count(*) from copia_seguridad c join admin a on a.id_usuario=c.id_admin where c.nombre='Backup_Prueba_001'")" 1

echo "-- Se alteran datos y luego se restaura la copia"
SQL "update denuncia set tipo_delito='ALTERADO', estado_policial='ALTERADO' where codigo='D-000584'" >/dev/null
SQL "delete from usuario_permiso where id_usuario=(select id_usuario from usuario where codigo='PNP-2081')" >/dev/null
SQL "delete from actividad_programada where id_actividad=(select min(id_actividad) from actividad_programada)" >/dev/null
SQL "update policia set rango='ALTERADO' where id_usuario=(select id_usuario from usuario where codigo='PNP-2045')" >/dev/null
igual "restaurar con contraseña incorrecta" "$(curl -s -o /tmp/r.json -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"password":"incorrecta"}' $SD/api/admin/respaldos/$ID/restaurar)" 403
igual "nada cambió tras el rechazo" "$(SQL "select tipo_delito from denuncia where codigo='D-000584'")" "ALTERADO"
igual "restaurar con contraseña correcta" "$(curl -s -o /tmp/r.json -w '%{http_code}' -b /tmp/adm.txt -H "$J" -d '{"password":"Demo1234"}' $SD/api/admin/respaldos/$ID/restaurar)" 200
cat /tmp/r.json; echo
igual "denuncia restaurada" "$(SQL "select tipo_delito from denuncia where codigo='D-000584'")" "Hurto"
igual "permisos restaurados" "$(SQL "select count(*) from usuario_permiso where id_usuario=(select id_usuario from usuario where codigo='PNP-2081')")" 6
igual "actividad borrada restaurada" "$(SQL "select count(*) from actividad_programada")" 8
igual "rango restaurado" "$(SQL "select rango from policia where id_usuario=(select id_usuario from usuario where codigo='PNP-2045')")" "Suboficial de Primera"
igual "funcionario creado después de la copia se conserva... (PNP-9001 es anterior)" "$(SQL "select count(*) from usuario where codigo='PNP-9001'")" 1
igual "login sigue funcionando tras restaurar" "$(login /tmp/pnp.txt PNP-2045 policia)" 200

echo "-- Copias automáticas"
igual "guardar configuración" "$(curl -s -o /tmp/cfg.json -w '%{http_code}' -b /tmp/adm.txt -X PUT -H "$J" -d '{"activo":true,"frecuencia":"DIARIA","hora":"00:00","diasConservacion":30}' $SD/api/admin/respaldos/configuracion)" 200
cat /tmp/cfg.json; echo
igual "hora inválida rechazada" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/adm.txt -X PUT -H "$J" -d '{"activo":true,"frecuencia":"DIARIA","hora":"25:00","diasConservacion":30}' $SD/api/admin/respaldos/configuracion)" 400
for i in $(seq 1 12); do [ "$(SQL "select count(*) from copia_seguridad where origen='Automática'")" != "0" ] && break; sleep 5; done
igual "copia automática generada" "$(SQL "select count(*)||'/'||min(estado) from copia_seguridad where origen='Automática'")" "1/Completada"
sleep 20
igual "no se repite el mismo día" "$(SQL "select count(*) from copia_seguridad where origen='Automática'")" 1
igual "historial" "$(curl -s -b /tmp/adm.txt $SD/api/admin/respaldos | jq -r '[.[]|select(.disponible)]|length')" 3

echo "== 4. Portal ciudadano"
curl -s -c /tmp/p.txt -H "$J" -d '{"codigo":"D-000584"}' $PC/api/portal/acceso/consulta >/dev/null
igual "PIN del ciudadano" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/p.txt -c /tmp/p.txt -H "$J" -d '{"pin":"123456"}' $PC/api/portal/acceso/verificar)" 200
igual "mis denuncias" "$(curl -s -b /tmp/p.txt $PC/api/portal/denuncias | jq length)" 3
curl -s -b /tmp/p.txt $PC/api/portal/denuncias/D-000584 > /tmp/det.json
echo "  responsable: $(jq -c '.responsable' /tmp/det.json)"
igual "responsable con cargo y dependencia" "$(jq -r '(.responsable.cargo|length>0|tostring)+"/"+(.responsable.dependencia|length>0|tostring)' /tmp/det.json)" "true/true"
igual "chat del ciudadano" "$(curl -s -o /tmp/chat.json -w '%{http_code}' -b /tmp/p.txt $PC/api/portal/denuncias/D-000584/chat)" 200
echo "  chat: $(head -c 240 /tmp/chat.json)"
curl -s -c /tmp/q.txt -H "$J" -d '{"codigo":"DEN-2026-000001"}' $PC/api/portal/acceso/consulta >/dev/null
igual "denuncia del Excel" "$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/q.txt -c /tmp/q.txt -H "$J" -d '{"pin":"123456"}' $PC/api/portal/acceso/verificar)" 200
echo "  responsable: $(curl -s -b /tmp/q.txt $PC/api/portal/denuncias/DEN-2026-000001 | jq -c '.responsable')"

echo "== RESULTADO: $FALLAS fallas"
[ "$FALLAS" = "0" ]
