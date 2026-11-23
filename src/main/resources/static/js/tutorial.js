/* ---------------------------------------------------------------------
   Tutorial del sistema de gestión de denuncias (usa js/tour.js).
   - Se abre solo la primera vez que cada funcionario entra (por código).
   - El botón "Tutorial" de la barra superior pregunta si se quiere repetir.
   Los pasos se adaptan al perfil: administrador, policía o fiscal.
   --------------------------------------------------------------------- */

const claveTutorial = () => `sgd-tutorial-visto-${(state.code || "").toUpperCase()}`;

// Módulos del menú que se explican en cada perfil (solo los que el funcionario tiene habilitados)
const MODULOS_TUTORIAL = {
  admin: [
    ["functions", "Funcionarios", "Consulta a todos los policías, fiscales y administradores registrados."],
    ["registerFunction", "Registrar funcionario", "Crea la cuenta institucional de un nuevo policía, fiscal o administrador."],
    ["users", "Usuarios y perfiles", "Habilita o deshabilita cuentas y asigna los módulos que cada funcionario puede usar."],
    ["reports", "Reportes", "Tablero de control: denuncias <b>por vencer</b> (15 días o más sin actividad), avance de casos por policía y por fiscal, y la consulta de denuncias con exportación a PDF y Excel."],
    ["activity", "Registros de actividades", "Auditoría: quién hizo cada operación en el sistema y cuándo."],
    ["backup", "Copias de seguridad", "Genera copias de la base de datos y restaura una copia anterior cuando sea necesario."],
    ["chat", "Chat de soporte", "Atiende las consultas de los funcionarios."]
  ],
  policia: [
    ["denuncias", "Denuncias registradas", "Tus denuncias asignadas. Al abrir una puedes actualizar su estado <b>escribiendo la justificación</b> de la actuación."],
    ["tracking", "Actuaciones policiales", "Registra y revisa las diligencias que realizas en cada caso."],
    ["documents", "Documentos y evidencias", "Adjunta actas, fotos o informes a la denuncia."],
    ["reports", "Informes a Fiscalía", "Remite tus informes policiales a la Fiscalía y consulta sus respuestas."],
    ["schedule", "Programación de actividades", "Tu agenda de diligencias y citaciones."],
    ["chat", "Chat institucional", "Conversa con el ciudadano denunciante y con el fiscal a cargo."]
  ],
  fiscal: [
    ["denuncias", "Carpetas fiscales", "Las carpetas que tienes asignadas y su estado en la Fiscalía."],
    ["diligences", "Diligencias", "Programa y revisa las diligencias fiscales."],
    ["provisions", "Disposiciones fiscales", "Registra tus decisiones (formalizar, archivar, ampliar la investigación…)."],
    ["reports", "Reportes", "Consulta resultados y exporta reportes en PDF o Excel."],
    ["chat", "Chat con funcionario policial", "Coordina el caso con el policía responsable."]
  ]
};

function pasosTutorial() {
  const p = profile();
  const nombre = (state.name || "").split(" ")[0] || "";
  const inicio = () => { if (state.page !== "dashboard") goPage("dashboard"); };
  const pasos = [
    {
      antes: inicio,
      titulo: `¡Hola${nombre ? ", " + escapeHtml(nombre) : ""}! Bienvenido al Sistema de gestión de denuncias`,
      texto: `Este recorrido rápido te muestra las partes principales de la pantalla para tu perfil de <b>${escapeHtml(p.role.toLowerCase())}</b>. Puedes avanzar, retroceder o salir cuando quieras (también con las flechas del teclado y Esc).`
    },
    { el: ".profile-chip", titulo: "Tu perfil", texto: `Aquí ves tu nombre y tu perfil: <b>${escapeHtml(p.role)}</b> · ${escapeHtml(p.institution)}.` },
    { el: "#mainNav", titulo: "Menú principal", texto: "Desde aquí entras a cada módulo. Solo aparecen los módulos que tienes habilitados." }
  ];
  (MODULOS_TUTORIAL[state.role] || []).forEach(([pagina, titulo, texto]) =>
    pasos.push({ el: `#mainNav [data-page="${pagina}"]`, titulo, texto }));
  pasos.push(
    { el: ".topbar > div:first-child", titulo: "Dónde estás", texto: "El título y la ruta te indican en qué pantalla te encuentras." },
    { el: "#statCards", titulo: "Resumen general", texto: "Los indicadores principales de tu trabajo, actualizados al momento." },
    { el: () => document.querySelector("#roleDashboardExtras .role-quick-grid") || document.querySelector("#quickActions") || document.querySelector(".quick-panel"),
      titulo: "Accesos rápidos", texto: "Atajos a las tareas que más usarás. Un clic y vas directo." },
    { el: "#notificationBtn", titulo: "Deberes pendientes", texto: "La campana muestra tus pendientes. El número rojo indica cuántos tienes." },
    { el: "#userMenu", titulo: "Tu cuenta", texto: "Abre este menú para ver tu perfil, la configuración de la cuenta o cerrar sesión." },
    { el: "#colorMode", titulo: "Modos de color", texto: "Cambia los colores de la pantalla: modo oscuro, alto contraste y modos para distintos tipos de daltonismo." },
    { el: "#voiceReader", titulo: "Escuchar", texto: "Lee en voz alta el contenido de la pantalla." },
    { el: ["#textSmaller", "#textLarger"], titulo: "Tamaño del texto", texto: "Reduce (A−) o aumenta (A+) el tamaño del contenido." },
    { el: "#helpBtn", titulo: "Ayuda y glosario", texto: "Términos del proceso explicados en lenguaje sencillo." },
    { el: "#tutorialBtn", titulo: "Repetir el tutorial", texto: "Si quieres ver este recorrido otra vez, pulsa <b>Tutorial</b>." },
    { el: "#logoutBtn", titulo: "Cerrar sesión", texto: "Al terminar tu jornada, cierra sesión desde aquí." },
    { antes: inicio, titulo: "¡Listo!", texto: "Ya conoces la pantalla principal. Puedes empezar a trabajar." }
  );
  return pasos;
}

function iniciarTutorial() {
  // Durante el tour se cierran los menús desplegables que pudieran tapar los elementos
  try { closeSessionMenu(); closeNotifications(); } catch (_) { /* menús ya cerrados */ }
  Tour.iniciar(pasosTutorial(), { alTerminar: () => Tour.marcarVisto(claveTutorial()) });
}

// Primera vez que el funcionario entra: el tutorial se abre solo
function tutorialAlIngresar() {
  if (!state.code || Tour.visto(claveTutorial())) return;
  setTimeout(() => { if (state.user && !Tour.activo) iniciarTutorial(); }, 700);
}

document.getElementById("tutorialBtn")?.addEventListener("click", async () => {
  if (Tour.activo) return;
  if (await Tour.confirmar("¿Quieres volver a repetir el tutorial?", "Se mostrará otra vez el recorrido por la pantalla principal.")) iniciarTutorial();
});
