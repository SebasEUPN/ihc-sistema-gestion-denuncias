/* =====================================================================
   Copias de seguridad (solo administrador)
   Cuatro secciones: 1. crear, 2. copias automáticas, 3. historial y
   4. restaurar. Todo se guarda en la base de datos por medio de
   /api/admin/respaldos (CopiaSeguridadService en el backend).
   ===================================================================== */

const TIPOS_COPIA = [["COMPLETA", "Completa"], ["DENUNCIAS", "Denuncias y seguimiento"], ["USUARIOS", "Usuarios y permisos"]];
const FRECUENCIAS_COPIA = [["DIARIA", "Diariamente"], ["SEMANAL", "Semanalmente"], ["MENSUAL", "Mensualmente"]];
const CONSERVACION_COPIA = [7, 15, 30, 60, 90];

function tamanoCopia(bytes) {
  if (!bytes) return "—";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1048576).toFixed(1) + " MB";
}
function hora12(hhmm) {
  const [h, m] = hhmm.split(":").map(Number);
  return `${String(h % 12 || 12).padStart(2, "0")}:${String(m).padStart(2, "0")} ${h < 12 ? "a. m." : "p. m."}`;
}
function estadoCopia(c) {
  return `<span class="status ${c.estado === "Completada" ? "green" : "red"}">${escapeHtml(c.estado)}</span>`;
}
function nombreSugeridoCopia() {
  const hoy = new Date(), p = n => String(n).padStart(2, "0");
  return `Backup_Denuncias_${hoy.getFullYear()}${p(hoy.getMonth() + 1)}${p(hoy.getDate())}_${p(hoy.getHours())}${p(hoy.getMinutes())}`;
}

/** Ventana emergente genérica de este módulo. Devuelve el elemento para agregarle eventos. */
function ventanaCopia(titulo, subtitulo, cuerpo) {
  document.querySelector("#bkModal")?.remove();
  const fondo = document.createElement("div");
  fondo.id = "bkModal"; fondo.className = "modal-backdrop"; fondo.setAttribute("role", "dialog"); fondo.setAttribute("aria-modal", "true");
  fondo.innerHTML = `<section class="preview-modal bk-modal"><div class="modal-header"><div><p class="eyebrow">COPIAS DE SEGURIDAD</p><h2>${titulo}</h2><p class="muted">${subtitulo}</p></div><button class="modal-close" type="button" data-bk-cerrar aria-label="Cerrar">×</button></div><div class="modal-body">${cuerpo}</div></section>`;
  const cerrar = () => { fondo.remove(); document.body.style.overflow = ""; };
  fondo.addEventListener("click", ev => { if (ev.target === fondo || ev.target.closest("[data-bk-cerrar]")) cerrar(); });
  fondo.addEventListener("keydown", ev => { if (ev.key === "Escape") cerrar(); });
  document.body.appendChild(fondo); document.body.style.overflow = "hidden";
  fondo.cerrar = cerrar;
  return fondo;
}

async function renderCopias(mount) {
  const e = escapeHtml;
  const [copias, config] = await Promise.all([API.get("/admin/respaldos"), API.get("/admin/respaldos/configuracion")]);
  const disponibles = copias.filter(c => c.disponible);
  const opciones = (lista, actual) => lista.map(([v, t]) => `<option value="${v}"${v === actual ? " selected" : ""}>${t}</option>`).join("");
  const filaHistorial = c => `<li><div><b>${e(c.nombre)}</b><small>${fechaHora(c.fechaHora)} · ${e(c.tipoNombre)} · ${e(c.origen)}</small></div>${estadoCopia(c)}</li>`;

  mount.innerHTML = `
  <div class="bk-card">
    <header class="bk-head"><h2>Copia de seguridad</h2><p class="muted">Gestiona, programa y restaura las copias de seguridad del sistema.</p></header>

    <form class="bk-section" id="bkCrear" novalidate>
      <h3>1. Crear copia de seguridad</h3><p class="bk-help">Genera un respaldo de la base de datos.</p>
      <label class="bk-label" for="bkNombre">Nombre de la copia</label>
      <input class="bk-input" id="bkNombre" type="text" maxlength="80" placeholder="Backup_Denuncias_2026" value="${nombreSugeridoCopia()}" required>
      <label class="bk-label" for="bkTipo">Tipo de copia de seguridad</label>
      <select class="bk-input" id="bkTipo">${opciones(TIPOS_COPIA, "COMPLETA")}</select>
      <label class="bk-label" for="bkDescripcion">Descripción (opcional)</label>
      <textarea class="bk-input" id="bkDescripcion" rows="2" maxlength="300" placeholder="Descripción del respaldo..."></textarea>
      <button class="bk-btn bk-primary" type="submit">Crear copia de seguridad</button>
    </form>

    <form class="bk-section" id="bkAuto" novalidate>
      <h3>2. Copias de seguridad automáticas</h3>
      <div class="bk-row"><label for="bkActivo" id="bkActivoLabel">Activar copias automáticas</label>
        <button class="bk-toggle${config.activo ? " on" : ""}" id="bkActivo" type="button" role="switch" aria-checked="${config.activo}" aria-labelledby="bkActivoLabel">${config.activo ? "Activado" : "Desactivado"}</button></div>
      <label class="bk-label" for="bkFrecuencia">Frecuencia</label>
      <select class="bk-input" id="bkFrecuencia">${opciones(FRECUENCIAS_COPIA, config.frecuencia)}</select>
      <label class="bk-label" for="bkHora">Hora programada</label>
      <input class="bk-input" id="bkHora" type="time" value="${e(config.hora)}" required>
      <label class="bk-label" for="bkDias">Conservar copias durante</label>
      <select class="bk-input" id="bkDias">${opciones([...new Set([...CONSERVACION_COPIA, config.diasConservacion])].sort((a, b) => a - b).map(d => [d, d + " días"]), config.diasConservacion)}</select>
      <p class="bk-help" id="bkAutoInfo">${config.activo ? `Programada: ${e(FRECUENCIAS_COPIA.find(f => f[0] === config.frecuencia)[1].toLowerCase())} a las ${hora12(config.hora).replace(/\.$/, "")}.` : "Las copias automáticas están desactivadas."}${config.ultimaAutomatica ? ` Última copia automática: ${fechaHora(config.ultimaAutomatica)}.` : ""}${config.actualizadoPor ? ` Configurado por ${e(config.actualizadoPor)}.` : ""}</p>
      <button class="bk-btn bk-secondary" type="submit">Guardar configuración</button>
    </form>

    <section class="bk-section">
      <h3>3. Historial de copias de seguridad</h3>
      <div class="bk-list-head"><span>Nombre</span><span>Estado</span></div>
      <ul class="bk-list">${copias.length ? copias.slice(0, 3).map(filaHistorial).join("") : `<li class="bk-empty">Todavía no hay copias de seguridad. Crea la primera en la sección 1.</li>`}</ul>
      <button class="bk-btn bk-secondary" id="bkHistorial" type="button"${copias.length ? "" : " disabled"}>Ver historial completo${copias.length ? ` (${copias.length})` : ""}</button>
    </section>

    <form class="bk-section" id="bkRestaurar" novalidate>
      <h3>4. Restaurar copia de seguridad</h3>
      <label class="bk-label" for="bkCopia">Seleccionar copia disponible</label>
      <select class="bk-input" id="bkCopia"><option value="">${disponibles.length ? "Seleccionar archivo de respaldo" : "No hay copias disponibles para restaurar"}</option>${disponibles.map(c => `<option value="${c.id}">${e(c.nombre)} — ${fechaHora(c.fechaHora)} (${e(c.tipoNombre)}, ${tamanoCopia(c.tamanoBytes)})</option>`).join("")}</select>
      <button class="bk-btn bk-danger" type="submit"${disponibles.length ? "" : " disabled"}>Restaurar copia de seguridad</button>
      <p class="bk-help">La restauración requiere confirmación del administrador.</p>
    </form>

    <footer class="bk-foot">Usuario: ${e(state.user ? state.user.nombreCompleto || state.user.codigo : "Administrador")}</footer>
  </div>`;

  const recargar = () => renderCopias(mount);
  const ocupado = (boton, texto) => { boton.disabled = true; boton.dataset.texto = boton.textContent; boton.textContent = texto; };
  const libre = boton => { boton.disabled = false; boton.textContent = boton.dataset.texto; };

  // ----- 1. Crear -----
  mount.querySelector("#bkCrear").addEventListener("submit", async ev => {
    ev.preventDefault();
    const nombre = mount.querySelector("#bkNombre").value.trim();
    if (nombre.length < 3) { toast("Escribe un nombre de al menos 3 caracteres para la copia."); mount.querySelector("#bkNombre").focus(); return; }
    const boton = ev.target.querySelector("button[type=submit]"); ocupado(boton, "Generando copia...");
    try {
      const copia = await API.post("/admin/respaldos", { nombre, tipo: mount.querySelector("#bkTipo").value, descripcion: mount.querySelector("#bkDescripcion").value.trim() });
      toast(`Copia "${copia.nombre}" creada: ${copia.registros} registros (${tamanoCopia(copia.tamanoBytes)}).`);
      recargar();
    } catch (error) { toast(error.message); libre(boton); if (/fallida/i.test(error.message)) recargar(); }
  });

  // ----- 2. Automáticas -----
  const interruptor = mount.querySelector("#bkActivo");
  interruptor.addEventListener("click", () => {
    const activo = !interruptor.classList.contains("on");
    interruptor.classList.toggle("on", activo); interruptor.setAttribute("aria-checked", activo); interruptor.textContent = activo ? "Activado" : "Desactivado";
  });
  mount.querySelector("#bkAuto").addEventListener("submit", async ev => {
    ev.preventDefault();
    const hora = mount.querySelector("#bkHora").value;
    if (!hora) { toast("Indica la hora programada."); return; }
    const boton = ev.target.querySelector("button[type=submit]"); ocupado(boton, "Guardando...");
    try {
      await API.put("/admin/respaldos/configuracion", { activo: interruptor.classList.contains("on"), frecuencia: mount.querySelector("#bkFrecuencia").value, hora, diasConservacion: Number(mount.querySelector("#bkDias").value) });
      toast("Configuración de copias automáticas guardada.");
      recargar();
    } catch (error) { toast(error.message); libre(boton); }
  });

  // ----- 3. Historial completo -----
  mount.querySelector("#bkHistorial").addEventListener("click", () => {
    const filas = copias.map(c => `<tr><td><b>${e(c.nombre)}</b>${c.descripcion ? `<small>${e(c.descripcion)}</small>` : ""}</td><td>${fechaHora(c.fechaHora)}</td><td>${e(c.tipoNombre)}</td><td>${e(c.origen)}</td><td>${tamanoCopia(c.tamanoBytes)}</td><td>${e(c.administrador)}</td><td>${estadoCopia(c)}</td><td>${c.disponible ? `<a class="outline-btn" href="/api/admin/respaldos/${c.id}/archivo" download>Descargar</a>` : "—"}</td></tr>`).join("");
    ventanaCopia("Historial completo", `${copias.length} copias registradas en la base de datos.`, `<div class="bk-table-wrap"><table class="bk-table"><thead><tr><th>Nombre</th><th>Fecha y hora</th><th>Tipo</th><th>Origen</th><th>Tamaño</th><th>Administrador</th><th>Estado</th><th>Archivo</th></tr></thead><tbody>${filas}</tbody></table></div>`);
  });

  // ----- 4. Restaurar (pide la contraseña del administrador) -----
  mount.querySelector("#bkRestaurar").addEventListener("submit", ev => {
    ev.preventDefault();
    const id = mount.querySelector("#bkCopia").value, copia = disponibles.find(c => String(c.id) === id);
    if (!copia) { toast("Selecciona la copia que deseas restaurar."); mount.querySelector("#bkCopia").focus(); return; }
    const ventana = ventanaCopia("Confirmar restauración", `Copia "${e(copia.nombre)}" del ${fechaHora(copia.fechaHora)} (${e(copia.tipoNombre)}).`,
      `<p class="bk-warning"><b>¿Qué hará la restauración?</b> Los ${copia.registros} registros de esta copia volverán a tener los datos que tenían cuando se generó; los que se hayan borrado se crearán de nuevo. Los registros creados después de la copia se conservan.</p>
       <form id="bkConfirmar" novalidate><label class="bk-label" for="bkPassword">Para confirmar, escribe tu contraseña de administrador</label>
       <input class="bk-input" id="bkPassword" type="password" autocomplete="current-password" required>
       <div class="bk-actions"><button class="secondary-btn" type="button" data-bk-cerrar>Cancelar</button><button class="danger-btn" type="submit">Restaurar</button></div></form>`);
    ventana.querySelector("#bkPassword").focus();
    ventana.querySelector("#bkConfirmar").addEventListener("submit", async ev2 => {
      ev2.preventDefault();
      const password = ventana.querySelector("#bkPassword").value;
      if (!password) { toast("Escribe tu contraseña para confirmar."); return; }
      const boton = ev2.target.querySelector("button[type=submit]"); ocupado(boton, "Restaurando...");
      try {
        const resultado = await API.post(`/admin/respaldos/${copia.id}/restaurar`, { password });
        ventana.cerrar(); toast(resultado.mensaje); recargar();
      } catch (error) { toast(error.message); libre(boton); }
    });
  });
}
