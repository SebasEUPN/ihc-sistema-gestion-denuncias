package com.proyecto.denuncias.config;

import com.proyecto.denuncias.model.ActividadProgramada;
import com.proyecto.denuncias.model.Actuacion;
import com.proyecto.denuncias.model.Ciudadano;
import com.proyecto.denuncias.model.Conversacion;
import com.proyecto.denuncias.model.CopiaSeguridad;
import com.proyecto.denuncias.model.Denuncia;
import com.proyecto.denuncias.model.DisposicionFiscal;
import com.proyecto.denuncias.model.Documento;
import com.proyecto.denuncias.model.Mensaje;
import com.proyecto.denuncias.model.RegistroActividad;
import com.proyecto.denuncias.model.RemisionFiscal;
import com.proyecto.denuncias.model.Rol;
import com.proyecto.denuncias.model.Usuario;
import com.proyecto.denuncias.repository.ActividadProgramadaRepository;
import com.proyecto.denuncias.repository.ActuacionRepository;
import com.proyecto.denuncias.repository.CiudadanoRepository;
import com.proyecto.denuncias.repository.ConversacionRepository;
import com.proyecto.denuncias.repository.CopiaSeguridadRepository;
import com.proyecto.denuncias.repository.DenunciaRepository;
import com.proyecto.denuncias.repository.DisposicionFiscalRepository;
import com.proyecto.denuncias.repository.DocumentoRepository;
import com.proyecto.denuncias.repository.MensajeRepository;
import com.proyecto.denuncias.repository.RegistroActividadRepository;
import com.proyecto.denuncias.repository.RemisionFiscalRepository;
import com.proyecto.denuncias.repository.UsuarioRepository;
import com.proyecto.denuncias.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Carga los datos de demostración la PRIMERA vez que arranca la aplicación
 * (cuando la tabla "usuario" está vacía). Son los mismos datos ficticios que
 * tenía el prototipo HTML, ahora guardados en SQL Server.
 *
 * Se hace desde Java y no desde el script SQL porque las contraseñas deben
 * cifrarse con BCrypt antes de guardarse.
 */
@Component
@Order(1) // después de MigracionBD, que agrega las columnas nuevas
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CiudadanoRepository ciudadanoRepository;
    private final DenunciaRepository denunciaRepository;
    private final ActuacionRepository actuacionRepository;
    private final DocumentoRepository documentoRepository;
    private final RemisionFiscalRepository remisionRepository;
    private final DisposicionFiscalRepository disposicionRepository;
    private final ActividadProgramadaRepository actividadRepository;
    private final RegistroActividadRepository registroRepository;
    private final CopiaSeguridadRepository copiaRepository;
    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final PasswordEncoder passwordEncoder;

    /** Contraseña de los usuarios de demostración (propiedad app.demo.password). */
    private final String passwordDemo;

    public DatosIniciales(UsuarioRepository usuarioRepository, CiudadanoRepository ciudadanoRepository,
                          DenunciaRepository denunciaRepository, ActuacionRepository actuacionRepository,
                          DocumentoRepository documentoRepository, RemisionFiscalRepository remisionRepository,
                          DisposicionFiscalRepository disposicionRepository,
                          ActividadProgramadaRepository actividadRepository,
                          RegistroActividadRepository registroRepository, CopiaSeguridadRepository copiaRepository,
                          ConversacionRepository conversacionRepository, MensajeRepository mensajeRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.demo.password:Demo1234}") String passwordDemo) {
        this.usuarioRepository = usuarioRepository;
        this.ciudadanoRepository = ciudadanoRepository;
        this.denunciaRepository = denunciaRepository;
        this.actuacionRepository = actuacionRepository;
        this.documentoRepository = documentoRepository;
        this.remisionRepository = remisionRepository;
        this.disposicionRepository = disposicionRepository;
        this.actividadRepository = actividadRepository;
        this.registroRepository = registroRepository;
        this.copiaRepository = copiaRepository;
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordDemo = passwordDemo;
    }

    /** Spring ejecuta este método automáticamente al terminar de arrancar. */
    @Override
    @Transactional // si algo falla no queda ningún dato a medias
    public void run(String... args) {
        // Si ya existen usuarios, los datos fueron cargados antes: no se hace nada
        if (usuarioRepository.count() > 0) {
            return;
        }

        // ---------------------------------------------------------------
        // 1. USUARIOS (todos con la contraseña de demostración cifrada)
        // ---------------------------------------------------------------
        String hash = passwordEncoder.encode(passwordDemo);
        Usuario carlos = crearUsuario("PNP-2045", hash, "Carlos", "Mendoza Ruiz", "45821763", Rol.POLICIA, "PNP",
                "Suboficial de Primera", "Comisaría PNP San Isidro", "987654321", "cmendoza@pnp.gob.pe", true);
        carlos.setDireccion("Av. Los Próceres 245, Lima");
        carlos.setContactoEmergencia("945120883");
        Usuario maria = crearUsuario("FN-1023", hash, "María Fernanda", "Salas", "42156987", Rol.FISCAL, "Fiscalía",
                "Fiscal Provincial", "Segunda Fiscalía Provincial Penal", "986321457", "msalas@mpfn.gob.pe", true);
        Usuario jorge = crearUsuario("PNP-2081", hash, "Jorge", "Ramírez Soto", "44718203", Rol.POLICIA, "PNP",
                "Suboficial de Segunda", "Comisaría PNP Lima Centro", "986321456", "jramirez@pnp.gob.pe", true);
        Usuario ana = crearUsuario("PNP-2094", hash, "Ana Belén", "Vargas", "46120987", Rol.POLICIA, "PNP",
                "Suboficial de Tercera", "Comisaría PNP Lima Centro", "985210334", "avargas@pnp.gob.pe", true);
        crearUsuario("FN-1044", hash, "Lucía", "Torres Vega", "43310562", Rol.FISCAL, "Fiscalía",
                "Fiscal Adjunta Provincial", "Primera Fiscalía Provincial Penal", "984557120", "ltorres@mpfn.gob.pe", false);
        Usuario admin = crearUsuario("ADM-0001", hash, "Administrador", "Principal", "40000001", Rol.ADMIN, "Administración",
                "Administrador del sistema", "Oficina de Tecnologías de la Información", "980000001", "admin@sistema.gob.pe", true);

        // ---------------------------------------------------------------
        // 2. CIUDADANOS Y DENUNCIAS (4 detalladas + 16 generadas = 20)
        // ---------------------------------------------------------------
        String[] despachos = {"Primera Fiscalía Provincial Penal", "Segunda Fiscalía Provincial Penal", "Tercera Fiscalía Provincial Penal"};
        String[] fiscales = {"Dra. Carla Mendoza", "Dr. Miguel Salazar", "Dra. Patricia Rojas"};
        // Estado fiscal de cada una de las 20 denuncias (mismo orden que el prototipo)
        String[] estadosFiscales = {"En investigación", "En investigación", "Diligencias", "Con disposición", "En investigación",
                "Archivado", "Diligencias", "En investigación", "Con disposición", "En investigación", "Diligencias", "Archivado",
                "En investigación", "Con disposición", "Diligencias", "En investigación", "En investigación", "Diligencias",
                "Con disposición", "En investigación"};

        List<Denuncia> denuncias = new ArrayList<>();
        denuncias.add(crearDenuncia("D-000584", new Ciudadano("Juan Carlos", "Pérez Ramírez", "45678912", "juan.perez@email.com", "987654321"),
                "Hurto", "En proceso", LocalDateTime.of(2026, 11, 2, 9, 12), LocalDateTime.of(2026, 11, 5, 14, 35),
                "Av. Los Próceres 145, Lima Centro", despachos[0], "Dra. Carla Mendoza",
                "El ciudadano reporta la sustracción de sus pertenencias en la vía pública. Se encuentra pendiente la recopilación de información y las diligencias correspondientes."));
        denuncias.add(crearDenuncia("D-000585", new Ciudadano("Ana Lucía", "Torres Díaz", "47891236", "ana.torres@email.com", "986321456"),
                "Robo", "En proceso", LocalDateTime.of(2026, 11, 2, 10, 20), LocalDateTime.of(2026, 11, 5, 11, 10),
                "Jr. Unión 320, Cercado de Lima", despachos[1], "Dr. Miguel Salazar",
                "Denuncia por robo de pertenencias. Se registraron los datos iniciales y se encuentra en evaluación."));
        denuncias.add(crearDenuncia("D-000586", new Ciudadano("Luis Alberto", "Rojas Vega", "41235678", "luis.rojas@email.com", "985456123"),
                "Estafa", "Por resolver", LocalDateTime.of(2026, 11, 1, 15, 40), LocalDateTime.of(2026, 11, 4, 16, 25),
                "Av. Javier Prado 850, San Isidro", despachos[2], "Dra. Patricia Rojas",
                "Presunta estafa mediante una operación comercial. Se requiere completar la documentación."));
        denuncias.add(crearDenuncia("D-000587", new Ciudadano("María Elena", "Díaz Castro", "43987651", "maria.diaz@email.com", "984123765"),
                "Daños", "Resuelto", LocalDateTime.of(2026, 10, 31, 8, 30), LocalDateTime.of(2026, 11, 3, 12, 0),
                "Calle Las Flores 215, Pueblo Libre", despachos[0], "Dr. Roberto Díaz",
                "Incidente de daños materiales. Se completaron las diligencias policiales."));

        // Datos de las 16 denuncias generadas: {nombres, apellidos, delito}
        String[][] generados = {
                {"Carlos", "Ruiz", "Estafa"}, {"María", "Torres", "Lesiones"}, {"Pedro", "Sánchez", "Robo"}, {"Valeria", "Gómez", "Hurto"},
                {"Jorge", "Castillo", "Amenazas"}, {"Lucía", "Herrera", "Daños"}, {"Andrés", "Flores", "Extorsión"}, {"Camila", "Vargas", "Robo"},
                {"Diego", "Navarro", "Estafa"}, {"Elena", "Campos", "Lesiones"}, {"Marco", "Silva", "Hurto"}, {"Paola", "Medina", "Amenazas"},
                {"Raúl", "Cárdenas", "Daños"}, {"Rosa", "Gutiérrez", "Robo"}, {"Daniel", "León", "Estafa"}, {"Sofía", "Rivas", "Hurto"}};
        String[] estadosPoliciales = {"En investigación", "En proceso", "Por resolver", "Con disposición", "Resuelto", "Archivado",
                "En proceso", "En investigación", "Por resolver", "Con disposición", "En proceso", "Resuelto", "En investigación",
                "Archivado", "Por resolver", "En proceso"};
        String[] lugares = {"Lima Centro", "San Miguel", "San Isidro", "Cercado de Lima"};
        for (int i = 0; i < generados.length; i++) {
            String nombres = generados[i][0];
            String apellidos = generados[i][1];
            String delito = generados[i][2];
            String codigo = String.format("D-%06d", 588 + i);               // D-000588, D-000589...
            String dni = String.valueOf(40000000 + i * 17321);              // DNI ficticio distinto para cada uno
            String correo = (nombres + "." + apellidos).toLowerCase() + "@email.com";
            String telefono = "98765" + String.format("%04d", 432 + i);
            // Cada denuncia se registra un día antes que la anterior (30/10, 29/10, 28/10...)
            LocalDateTime registro = LocalDateTime.of(2026, 10, 30, 10, 30).minusDays(i);
            LocalDateTime actualizacion = LocalDateTime.of(2026, 11, 1 + (i % 6), 14, 20);
            denuncias.add(crearDenuncia(codigo, new Ciudadano(nombres, apellidos, dni, correo, telefono),
                    delito, estadosPoliciales[i], registro, actualizacion, lugares[i % 4], despachos[i % 3], fiscales[i % 3],
                    "Registro demostrativo de " + delito.toLowerCase() + " reportado por " + nombres + " " + apellidos
                            + ". Se encuentra en seguimiento según el estado asignado."));
        }
        // Estado fiscal y número de carpeta fiscal de las 20 denuncias
        for (int i = 0; i < denuncias.size(); i++) {
            denuncias.get(i).setEstadoFiscal(estadosFiscales[i]);
            denuncias.get(i).setCarpetaFiscal(String.format("CF-2026-%05d", 125 + i)); // CF-2026-00125...
        }
        denunciaRepository.saveAll(denuncias);

        // ---------------------------------------------------------------
        // 3. ACTUACIONES POLICIALES Y FISCALES
        // ---------------------------------------------------------------
        actuacionRepository.saveAll(List.of(
                new Actuacion("ACT-001", "D-000584", Rol.POLICIA, "Entrevista al denunciante", LocalDate.of(2026, 11, 5), "Completada"),
                new Actuacion("ACT-002", "D-000585", Rol.POLICIA, "Inspección técnico policial", LocalDate.of(2026, 11, 6), "En proceso"),
                new Actuacion("ACT-003", "D-000586", Rol.POLICIA, "Recopilación de documentos", LocalDate.of(2026, 11, 7), "Programada"),
                new Actuacion("ACT-004", "D-000588", Rol.POLICIA, "Verificación de cámaras", LocalDate.of(2026, 11, 7), "Pendiente"),
                new Actuacion("FIS-001", "D-000584", Rol.FISCAL, "Evaluación de carpeta fiscal", LocalDate.of(2026, 11, 5), "En proceso"),
                new Actuacion("FIS-002", "D-000586", Rol.FISCAL, "Revisión de elementos de convicción", LocalDate.of(2026, 11, 6), "Completada"),
                new Actuacion("FIS-003", "D-000590", Rol.FISCAL, "Programación de diligencia", LocalDate.of(2026, 11, 7), "Programada")));

        // ---------------------------------------------------------------
        // 4. DOCUMENTOS Y EVIDENCIAS
        // ---------------------------------------------------------------
        documentoRepository.saveAll(List.of(
                new Documento("DOC-001", "Acta de intervención", "D-000584", "PNP-2045", LocalDate.of(2026, 11, 5), "Validado"),
                new Documento("DOC-002", "Declaración testimonial", "D-000585", "PNP-2045", LocalDate.of(2026, 11, 5), "Pendiente"),
                new Documento("DOC-003", "Informe policial", "D-000586", "PNP-2081", LocalDate.of(2026, 11, 6), "Remitido"),
                new Documento("DOC-004", "Disposición fiscal", "D-000590", "FN-1023", LocalDate.of(2026, 11, 6), "Validado")));

        // ---------------------------------------------------------------
        // 5. REMISIONES A FISCALÍA Y DISPOSICIONES FISCALES
        // ---------------------------------------------------------------
        remisionRepository.saveAll(List.of(
                new RemisionFiscal("D-000584", "CF-2026-00125", LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5), "Recibido"),
                new RemisionFiscal("D-000585", "CF-2026-00126", LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6), "Requerimiento"),
                new RemisionFiscal("D-000586", "CF-2026-00127", LocalDate.of(2026, 11, 4), null, "En espera"),
                new RemisionFiscal("D-000587", null, LocalDate.of(2026, 11, 5), null, "Pendiente")));

        disposicionRepository.saveAll(List.of(
                new DisposicionFiscal("DISP-2026-0012", "CF-2026-00125", "Inicio de diligencias preliminares", LocalDate.of(2026, 11, 4), "Dra. Carla Mendoza", "Emitida"),
                new DisposicionFiscal("DISP-2026-0013", "CF-2026-00126", "Ampliación de investigación", LocalDate.of(2026, 11, 5), "Dr. Miguel Salazar", "Emitida"),
                new DisposicionFiscal("DISP-2026-0014", "CF-2026-00127", "Archivo preliminar", LocalDate.of(2026, 11, 6), "Dra. Patricia Rojas", "Notificada")));

        // ---------------------------------------------------------------
        // 6. AGENDA: actividades policiales y diligencias fiscales
        // ---------------------------------------------------------------
        actividadRepository.saveAll(List.of(
                new ActividadProgramada(Rol.POLICIA, "Entrevista al denunciante", "D-000584", "Comisaría Lima Centro", LocalDateTime.of(2026, 11, 10, 8, 0)),
                new ActividadProgramada(Rol.POLICIA, "Verificación de cámaras", "D-000585", "Av. Arequipa", LocalDateTime.of(2026, 11, 12, 9, 30)),
                new ActividadProgramada(Rol.POLICIA, "Inspección técnico policial", "D-000586", "San Isidro", LocalDateTime.of(2026, 11, 16, 10, 0)),
                new ActividadProgramada(Rol.POLICIA, "Entrega de informe", "D-000588", "Fiscalía Provincial", LocalDateTime.of(2026, 11, 19, 15, 0)),
                new ActividadProgramada(Rol.FISCAL, "Declaración testimonial", "CF-2026-00125", "Despacho fiscal 2", LocalDateTime.of(2026, 11, 11, 9, 0)),
                new ActividadProgramada(Rol.FISCAL, "Inspección fiscal", "CF-2026-00126", "San Miguel", LocalDateTime.of(2026, 11, 13, 10, 30)),
                new ActividadProgramada(Rol.FISCAL, "Audiencia de control", "CF-2026-00127", "Sala 3", LocalDateTime.of(2026, 11, 17, 8, 30)),
                new ActividadProgramada(Rol.FISCAL, "Revisión de pericias", "CF-2026-00125", "Despacho fiscal 2", LocalDateTime.of(2026, 11, 20, 11, 0))));

        // ---------------------------------------------------------------
        // 7. AUDITORÍA Y COPIA DE SEGURIDAD DE EJEMPLO
        // ---------------------------------------------------------------
        registroRepository.saveAll(List.of(
                new RegistroActividad(LocalDateTime.of(2026, 11, 6, 15, 10), "ADM-0001", "Consultó reporte mensual", "Reportes", "Correcto"),
                new RegistroActividad(LocalDateTime.of(2026, 11, 6, 16, 25), "PNP-2045", "Adjuntó documento a D-000584", "Documentos", "Correcto"),
                new RegistroActividad(LocalDateTime.of(2026, 11, 7, 9, 12), "FN-1023", "Registró disposición fiscal", "Fiscalía", "Correcto"),
                new RegistroActividad(LocalDateTime.of(2026, 11, 7, 9, 30), "PNP-2045", "Actualizó actuación D-000585", "Actuaciones", "Correcto"),
                new RegistroActividad(LocalDateTime.of(2026, 11, 7, 9, 42), "ADM-0001", "Inicio de sesión del administrador", "Acceso", "Correcto")));

        copiaRepository.save(new CopiaSeguridad(LocalDateTime.of(2026, 11, 7, 2, 0), 248, "Correcto", "ADM-0001"));

        // ---------------------------------------------------------------
        // 8. CONVERSACIONES DEL CHAT
        // ---------------------------------------------------------------
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);

        // Policía (Carlos) con ciudadanos
        Conversacion c1 = conversacion(Conversacion.TIPO_CIUDADANO, "D-000584", carlos, null, denuncias.get(0).getCiudadano());
        mensaje(c1, null, "Buenos días, quisiera saber si ya se revisaron las cámaras de seguridad.", hoy.atTime(9, 12), true);
        mensaje(c1, carlos, "Buenos días, señor Pérez. La solicitud de imágenes ya fue remitida al establecimiento.", hoy.atTime(9, 18), true);
        mensaje(c1, null, "Gracias. ¿Necesitan que me acerque nuevamente a la comisaría?", hoy.atTime(9, 24), false);

        Conversacion c2 = conversacion(Conversacion.TIPO_CIUDADANO, "D-000585", carlos, null, denuncias.get(1).getCiudadano());
        mensaje(c2, null, "Hola, deseo consultar si debo presentar algún documento adicional para mi denuncia.", hoy.atTime(8, 47), false);

        Conversacion c3 = conversacion(Conversacion.TIPO_CIUDADANO, "D-000586", carlos, null, denuncias.get(2).getCiudadano());
        mensaje(c3, null, "Buenas tardes, envié los comprobantes solicitados por mesa de partes.", ayer.atTime(16, 5), true);
        mensaje(c3, carlos, "Documentación recibida. Será incorporada al expediente.", ayer.atTime(16, 30), true);

        // Policías con la fiscal (María Fernanda)
        Conversacion c4 = conversacion(Conversacion.TIPO_INTERINSTITUCIONAL, "D-000584", carlos, maria, null);
        mensaje(c4, carlos, "Buenos días, doctora. Remito el parte policial actualizado y las fotografías de la intervención de la denuncia D-000584.", hoy.atTime(9, 5), true);
        mensaje(c4, maria, "Recibido. Por favor, confirme si se cuenta con la declaración del testigo y el acta de intervención firmada.", hoy.atTime(9, 14), true);
        mensaje(c4, carlos, "Sí, ambos documentos están incorporados. También se adjuntó el registro fotográfico.", hoy.atTime(9, 20), false);

        Conversacion c5 = conversacion(Conversacion.TIPO_INTERINSTITUCIONAL, "D-000585", jorge, maria, null);
        mensaje(c5, maria, "Buenos días. Se requiere ampliar la información sobre la inspección técnico policial realizada.", hoy.atTime(8, 42), true);
        mensaje(c5, jorge, "De acuerdo, estamos coordinando la ampliación y remitiremos el informe complementario durante el día.", hoy.atTime(8, 51), false);

        Conversacion c6 = conversacion(Conversacion.TIPO_INTERINSTITUCIONAL, "D-000586", ana, maria, null);
        mensaje(c6, ana, "Se informa que la documentación solicitada para la carpeta fiscal ya fue remitida.", ayer.atTime(15, 10), true);
        mensaje(c6, maria, "Confirmamos recepción. Se revisará la documentación y comunicaremos cualquier observación.", ayer.atTime(15, 40), true);

        // Funcionarios con el administrador (soporte)
        Conversacion c7 = conversacion(Conversacion.TIPO_SOPORTE, "Soporte PNP-2045", carlos, admin, null);
        mensaje(c7, carlos, "Tengo una consulta sobre la actualización de una actuación policial.", hoy.atTime(9, 12), true);
        mensaje(c7, admin, "Hola, indícame el código de la denuncia para revisar el caso.", hoy.atTime(9, 15), false);

        Conversacion c8 = conversacion(Conversacion.TIPO_SOPORTE, "Soporte FN-1023", maria, admin, null);
        mensaje(c8, maria, "Necesito apoyo para revisar los permisos de mi perfil.", hoy.atTime(10, 20), false);

        Conversacion c9 = conversacion(Conversacion.TIPO_SOPORTE, "Soporte PNP-2081", jorge, admin, null);
        mensaje(c9, jorge, "No puedo visualizar un documento adjunto.", ayer.atTime(11, 0), true);
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo para crear los registros
    // ------------------------------------------------------------------

    /** Crea y guarda un usuario; los funcionarios reciben todos los módulos habilitados. */
    private Usuario crearUsuario(String codigo, String hash, String nombres, String apellidos, String dni, Rol rol,
                                 String institucion, String rango, String dependencia, String telefono, String correo,
                                 boolean habilitado) {
        Usuario u = new Usuario();
        u.setCodigo(codigo);
        u.setPasswordHash(hash);
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setDni(dni);
        u.setRol(rol);
        u.setInstitucion(institucion);
        u.setRango(rango);
        u.setDependencia(dependencia);
        u.setTelefono(telefono);
        u.setCorreo(correo);
        u.setHabilitado(habilitado);
        if (rol != Rol.ADMIN) {
            u.setPermisos(new HashSet<>(UsuarioService.MODULOS));
        }
        return usuarioRepository.save(u);
    }

    /** Guarda al ciudadano y arma la denuncia (se guarda después con saveAll). */
    private Denuncia crearDenuncia(String codigo, Ciudadano ciudadano, String delito, String estadoPolicial,
                                   LocalDateTime registro, LocalDateTime actualizacion, String lugar,
                                   String despacho, String fiscal, String descripcion) {
        Denuncia d = new Denuncia();
        d.setCodigo(codigo);
        d.setCiudadano(ciudadanoRepository.save(ciudadano));
        d.setTipoDelito(delito);
        d.setEstadoPolicial(estadoPolicial);
        d.setEstadoFiscal("En investigación"); // valor provisional; se ajusta más arriba
        d.setFechaRegistro(registro);
        d.setUltimaActualizacion(actualizacion);
        d.setLugarHechos(lugar);
        d.setDespachoFiscal(despacho);
        d.setFiscalResponsable(fiscal);
        d.setDescripcion(descripcion);
        return d;
    }

    private Conversacion conversacion(String tipo, String referencia, Usuario a, Usuario b, Ciudadano ciudadano) {
        return conversacionRepository.save(new Conversacion(tipo, referencia, a, b, ciudadano));
    }

    /** remitente null = mensaje enviado por el ciudadano. */
    private void mensaje(Conversacion c, Usuario remitente, String texto, LocalDateTime fecha, boolean leido) {
        mensajeRepository.save(new Mensaje(c, remitente, texto, fecha, leido));
    }
}
