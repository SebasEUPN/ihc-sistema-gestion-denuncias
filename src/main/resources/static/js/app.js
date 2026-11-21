/* =====================================================================
   app.js — Lógica de las pantallas del Sistema de Gestión y Seguimiento
   de Denuncias. Es el mismo comportamiento del prototipo, pero los datos
   ya no están escritos en el código: se leen y se guardan en SQL Server
   por medio del backend (ver api.js).
   ===================================================================== */

// Atajos para buscar elementos en la página
const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];

// Estado de la sesión actual en el navegador
const state = {
  role: "policia",      // perfil: "admin", "policia" o "fiscal"
  code: "",             // código institucional
  name: "",             // nombre completo del funcionario
  user: null,           // datos completos del usuario (vienen del backend)
  page: "dashboard",    // página visible
  selectedId: null,     // denuncia abierta en el detalle
  selectedChat: null,   // conversación abierta en el chat
  calendar: null        // mes que muestra el calendario {year, month}
};

// Datos cargados desde el backend
let casos = [];           // denuncias / carpetas fiscales
let usuarios = [];        // usuarios (solo el administrador)
let conversaciones = [];  // chats del funcionario
let indicadores = [];     // tarjetas del panel principal

// Página del menú -> permiso que la habilita (las demás páginas siempre se muestran)
const PERMISO_POR_PAGINA = { denuncias: "denuncias", tracking: "tracking", documents: "documents", reports: "reports", chat: "chat", schedule: "schedule", diligences: "schedule" };

// Menú y textos de cada perfil
function profile() {
  if (state.role === "admin") return { institution: "Administración", role: "Administrador del sistema", short: "Administrador", nav: [["dashboard", "dashboard", "Inicio"], ["functions", "adminPerson", "Funcionarios"], ["registerFunction", "personPlus", "Registrar funcionario"], ["users", "administration", "Usuarios y perfiles"], ["settings", "adminGear", "Configuración"], ["reports", "adminBar", "Reportes"], ["activity", "adminActivity", "Registros de actividades"], ["backup", "adminDatabase", "Copias de seguridad"], ["chat", "chat", "Chat de soporte"]] };
  if (state.role === "policia") return { institution: "PNP", role: "Funcionario policial", short: "Policía", nav: [["dashboard", "dashboard", "Inicio"], ["denuncias", "policeFolder", "Denuncias registradas"], ["tracking", "tracking", "Actuaciones policiales"], ["documents", "denuncias", "Documentos y evidencias"], ["reports", "policeSend", "Informes a Fiscalía"], ["fiscalTracking", "policeArrows", "Seguimiento de Fiscalía"], ["schedule", "policeCalendar", "Programación de actividades"], ["chat", "chat", "Chat institucional"]] };
  return { institution: "Fiscalía", role: "Funcionario fiscal", short: "Fiscal", nav: [["dashboard", "dashboard", "Inicio"], ["denuncias", "fiscalFolder", "Carpetas fiscales"], ["tracking", "fiscalClipboard", "Actuaciones"], ["diligences", "fiscalCalendar", "Diligencias"], ["documents", "denuncias", "Documentos y evidencias"], ["provisions", "reports", "Disposiciones fiscales"], ["reports", "reports", "Reportes"], ["chat", "chat", "Chat con funcionario policial"]] };
}

/* ---------------------------------------------------------------------
   Utilidades generales
   --------------------------------------------------------------------- */

// Notificación flotante
function toast(message) { const el = $("#toast"); el.textContent = message; el.classList.add("show"); setTimeout(() => el.classList.remove("show"), 2800); }

// Evita que un texto guardado en la base de datos se interprete como HTML (protección contra XSS)
function escapeHtml(value) { return String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" }[char])); }

// Iniciales para los avatares: "Carlos Mendoza Ruiz" -> "CM"
function iniciales(nombre) { return String(nombre || "").split(" ").filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join(""); }

// "2026-11-05" o "2026-11-05T09:30:00" -> "05/11/2026"
function fechaCorta(iso) { return iso ? iso.slice(0, 10).split("-").reverse().join("/") : "—"; }

// "2026-11-07T09:42:10" -> "07/11/2026 09:42"
function fechaHora(iso) { return iso ? fechaCorta(iso) + " " + iso.slice(11, 16) : "—"; }

// ¿El usuario tiene habilitada la página? (el administrador siempre)
function tienePermiso(page) { const permiso = PERMISO_POR_PAGINA[page]; return state.role === "admin" || !permiso || (state.user?.permisos || []).includes(permiso); }

// Carga algo del backend; si falla (por ejemplo, módulo no habilitado) devuelve una lista vacía
async function cargarSeguro(ruta) { try { return await API.get(ruta); } catch (error) { return []; } }

/* ---------------------------------------------------------------------
   Pantallas de acceso: inicio de sesión, recuperación y cambio de contraseña
   --------------------------------------------------------------------- */

function showAuthScreen(id) { $$(".auth-screen").forEach(s => s.classList.add("hidden")); $("#" + id).classList.remove("hidden"); }
$$("[data-screen]").forEach(b => b.addEventListener("click", () => showAuthScreen(b.dataset.screen)));
$$("[data-toggle]").forEach(b => b.addEventListener("click", () => { const input = $("#" + b.dataset.toggle); input.type = input.type === "password" ? "text" : "password"; }));

// Inicio de sesión: el backend valida código, contraseña y perfil
$("#loginForm").addEventListener("submit", async e => {
  e.preventDefault();
  const codigo = $("#institutionCode").value.trim(), password = $("#password").value;
  if (!codigo || !password) return toast("Completa tu código y contraseña.");
  try {
    const usuario = await API.post("/auth/login", { codigo, password, rol: $("#role").value });
    await iniciarAplicacion(usuario);
  } catch (error) { toast(error.message); }
});

// Recuperación de contraseña, paso 1: verificar identidad con código, perfil y DNI
$("#recoverForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    await API.post("/auth/recuperar", { codigo: $("#recoverCode").value.trim(), rol: $("#recoverRole").value, dni: $("#recoverDni").value.trim() });
    $("#recoverForm").reset();
    toast("Identidad verificada. Define tu nueva contraseña.");
    showAuthScreen("changeScreen");
  } catch (error) { toast(error.message); }
});

// Paso 2 (o cambio desde Configuración): guardar la nueva contraseña
$("#changeForm").addEventListener("submit", async e => {
  e.preventDefault();
  if ($("#newPassword").value !== $("#confirmPassword").value) return toast("Las contraseñas no coinciden.");
  try {
    await API.post("/auth/cambiar-password", { nuevaPassword: $("#newPassword").value });
    $("#changeForm").reset();
    state.user = null; // el backend cerró la sesión: hay que ingresar con la nueva contraseña
    showAuthScreen("successScreen");
  } catch (error) { toast(error.message); }
});

// Cierre de sesión (con ventana de confirmación)
$("#logoutBtn").addEventListener("click", requestLogout);
$("#menuLogout").addEventListener("click", requestLogout);
$("#confirmLogout").addEventListener("click", confirmLogout);
$("#cancelLogout").addEventListener("click", cancelLogout);
$("#closeLogoutConfirm").addEventListener("click", cancelLogout);
$("#logoutConfirmModal").addEventListener("click", e => { if (e.target.id === "logoutConfirmModal") cancelLogout(); });
$("#changeFromSettings").addEventListener("click", () => { showAuthScreen("changeScreen"); $("#appView").classList.add("hidden"); $("#authView").classList.remove("hidden"); });

function requestLogout() { closeSessionMenu(); $("#logoutConfirmModal").classList.remove("hidden"); $("#cancelLogout").focus(); }
function cancelLogout() { $("#logoutConfirmModal").classList.add("hidden"); }
async function confirmLogout() {
  $("#logoutConfirmModal").classList.add("hidden");
  try { await API.post("/auth/logout"); } catch (error) { /* aunque falle, se vuelve al acceso */ }
  mostrarAcceso();
}

// Vuelve a la pantalla de acceso y limpia los datos del navegador
function mostrarAcceso() {
  state.user = null; state.page = "dashboard"; state.selectedId = null; state.selectedChat = null;
  casos = []; usuarios = []; conversaciones = []; indicadores = [];
  $("#appView").classList.add("hidden"); $("#authView").classList.remove("hidden");
  $("#loginForm").reset(); showAuthScreen("loginScreen");
}

// api.js llama a esta función cuando el servidor responde que la sesión expiró
function alExpirarSesion() { if (!state.user) return; mostrarAcceso(); toast("Tu sesión ha expirado. Inicia sesión nuevamente."); }

/* ---------------------------------------------------------------------
   Menús de la barra superior
   --------------------------------------------------------------------- */

const userMenu = $("#userMenu"), sessionMenu = $("#sessionMenu");
function closeSessionMenu() { sessionMenu.classList.add("hidden"); userMenu.setAttribute("aria-expanded", "false"); }
userMenu.addEventListener("click", e => { e.stopPropagation(); const opening = sessionMenu.classList.contains("hidden"); sessionMenu.classList.toggle("hidden", !opening); userMenu.setAttribute("aria-expanded", String(opening)); });
const notificationBtn = $("#notificationBtn"), notificationPanel = $("#notificationPanel");
function closeNotifications() { notificationPanel.classList.add("hidden"); notificationBtn.setAttribute("aria-expanded", "false"); }
notificationBtn.addEventListener("click", e => { e.stopPropagation(); const opening = notificationPanel.classList.contains("hidden"); closeSessionMenu(); notificationPanel.classList.toggle("hidden", !opening); notificationBtn.setAttribute("aria-expanded", String(opening)); });
document.addEventListener("click", e => { if (!e.target.closest(".user-menu-wrap")) closeSessionMenu(); if (!e.target.closest(".notification-wrap")) closeNotifications(); });
document.addEventListener("keydown", e => { if (e.key === "Escape") { closeSessionMenu(); closeNotifications(); closeCasePreview(); cancelLogout(); } });

/* ---------------------------------------------------------------------
   Arranque de la aplicación después de iniciar sesión
   --------------------------------------------------------------------- */

// Guarda al usuario, carga sus datos desde el backend y muestra el panel
async function iniciarAplicacion(usuario) {
  state.user = usuario; state.role = usuario.rol; state.code = usuario.codigo; state.name = usuario.nombreCompleto;
  await cargarDatos();
  state.selectedChat = conversaciones[0]?.id ?? null;
  openApp();
}

// Trae del backend todo lo que necesitan las pantallas principales (en paralelo)
async function cargarDatos() {
  const [listaDenuncias, listaIndicadores, listaChats, listaUsuarios] = await Promise.all([
    cargarSeguro("/denuncias"),
    cargarSeguro("/panel/indicadores"),
    cargarSeguro("/chat/conversaciones"),
    state.role === "admin" ? cargarSeguro("/usuarios") : Promise.resolve([])
  ]);
  casos = listaDenuncias.map(aCaso); indicadores = listaIndicadores; conversaciones = listaChats.map(aChat); usuarios = listaUsuarios;
}

// Vuelve a leer denuncias e indicadores después de guardar un cambio
async function recargarCasos() {
  const [listaDenuncias, listaIndicadores] = await Promise.all([cargarSeguro("/denuncias"), cargarSeguro("/panel/indicadores")]);
  casos = listaDenuncias.map(aCaso); indicadores = listaIndicadores;
  renderCases(); renderDashboard(); renderDuties();
}

function openApp() {
  $("#authView").classList.add("hidden"); $("#appView").classList.remove("hidden");
  const p = profile(), sigla = iniciales(state.name);
  // Insignia del menú lateral según el perfil: PNP para el policía, Ministerio Público para el fiscal
  // y el símbolo de la balanza para el administrador
  const insignia = { policia: document.querySelector('img[data-logo="pnp"]'), fiscal: document.querySelector('img[data-logo="mp"]') }[state.role];
  $("#sidebarLogo").classList.toggle("with-logo", Boolean(insignia));
  // Alto de la insignia del menú en píxeles (cámbialo aquí si la quieres más grande o más pequeña)
  $("#sidebarLogo").innerHTML = insignia ? `<img src="${insignia.src}" alt="${insignia.alt}" height="46">` : "⚖";
  $("#sidebarInstitution").textContent = p.institution; $("#sidebarName").textContent = state.name; $("#sidebarRole").textContent = p.role;
  $("#topCode").textContent = state.code; $("#topRole").textContent = p.role; $("#avatar").textContent = sigla; $("#topAvatar").textContent = sigla;
  $("#settingsRole").textContent = p.role + " — " + p.institution; $("#settingsCode").textContent = state.code;
  // Solo se dibujan en el menú los módulos que el funcionario tiene habilitados
  $("#mainNav").innerHTML = p.nav.filter(([page]) => tienePermiso(page)).map(([page, icon, label], i) => `<button class="nav-item ${i === 0 ? "active" : ""}" data-page="${page}"><span class="nav-icon">${SIDEBAR_ICONS[icon] || icon}</span>${label}${page === "chat" ? `<span class="nav-badge" id="chatBadge">${totalUnread()}</span>` : ""}</button>`).join("");
  $("#denunciasTitle").textContent = state.role === "fiscal" ? "Carpetas fiscales asignadas" : "Denuncias asignadas";
  updateDateTime(); renderDashboard(); renderCases(); renderUsers(); renderDuties(); renderChat(); goPage("dashboard");
}

// Navegación: cualquier elemento con data-page cambia de página
document.addEventListener("click", e => { const b = e.target.closest("[data-page]"); if (b) goPage(b.dataset.page); });

function goPage(page) {
  detenerLectura(); // al cambiar de página se detiene la lectura por voz
  if (page === "detail" && !state.selectedId) page = "denuncias";
  state.page = page; closeSessionMenu(); closeNotifications();
  const names = { dashboard: state.role === "admin" ? "Panel del Administrador" : state.role === "fiscal" ? "Panel del Fiscal" : "Panel del Funcionario Policial", denuncias: state.role === "fiscal" ? "Carpetas fiscales" : "Denuncias registradas", detail: "Detalle de denuncia", tracking: state.role === "policia" ? "Actuaciones policiales" : "Actuaciones", reports: state.role === "policia" ? "Informes a Fiscalía" : "Reportes", users: "Usuarios y perfiles", settings: "Configuración", profile: "Mi perfil", chat: state.role === "admin" ? "Chat de soporte" : state.role === "fiscal" ? "Chat con funcionario policial" : "Chat institucional", documents: "Documentos y evidencias", fiscalTracking: "Seguimiento de Fiscalía", schedule: "Programación de actividades", diligences: "Diligencias", provisions: "Disposiciones fiscales", help: "Ayuda y glosario", functions: "Funcionarios", registerFunction: "Registrar funcionario", roles: "Roles y permisos", activity: "Registros de actividades", backup: "Copias de seguridad" };
  $("#pageTitle").textContent = names[page] || "Panel principal"; $("#breadcrumbCurrent").textContent = names[page] || "Inicio";
  $$(".page").forEach(p => p.classList.add("hidden")); $("#" + page + "Page").classList.remove("hidden");
  $$(".nav-item").forEach(b => b.classList.toggle("active", b.dataset.page === page));
  if (page === "detail") renderDetail();
  if (page === "reports") renderReports();
  if (["tracking", "documents", "fiscalTracking", "schedule", "diligences", "provisions", "functions", "roles", "activity", "backup"].includes(page)) renderRoleModule(page);
  if (page === "help") { $("#glossarySearch").value = ""; filtrarGlosario(); }
  if (page === "profile") renderProfile();
  if (page === "chat") renderChat();
}

/* ---------------------------------------------------------------------
   Panel principal (Inicio)
   --------------------------------------------------------------------- */

function getCases() { return casos; }
function statusClass(s) { if (s === "En proceso") return "amber"; if (s === "En investigación") return "blue"; if (["Por resolver", "Con disposición"].includes(s)) return "yellow"; if (s === "Resuelto") return "green"; return "gray"; }
function statusTag(s) { return `<span class="status ${statusClass(s)}">${escapeHtml(s)}</span>`; }
// Etiqueta con un color elegido (para los estados de los módulos de apoyo)
function etiqueta(texto, color) { return `<span class="status ${color}">${escapeHtml(texto)}</span>`; }

// Botones de acción de cada denuncia: el lápiz abre el detalle y el ojo la vista rápida.
// El administrador solo consulta, por eso no ve el lápiz.
function caseActions(c) {
  const editar = state.role === "admin" ? "" : `<button class="action-icon edit" data-detail="${escapeHtml(c.id)}" aria-label="Abrir ${escapeHtml(c.id)}">${SIDEBAR_ICONS.pencil}</button>`;
  return `<div class="action-group">${editar}<button class="action-icon view" data-preview="${escapeHtml(c.id)}" aria-label="Vista rápida ${escapeHtml(c.id)}">${SIDEBAR_ICONS.eye}</button></div>`;
}

// Cuenta las denuncias registradas en cada uno de los últimos N meses (para los gráficos)
function denunciasPorMes(cantidad) {
  const nombres = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"], hoy = new Date(), meses = [];
  for (let i = cantidad - 1; i >= 0; i--) { const f = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1); meses.push({ m: nombres[f.getMonth()], anio: f.getFullYear(), mes: f.getMonth(), v: 0 }); }
  casos.forEach(c => { const f = new Date(c.fechaRegistro), mes = meses.find(x => x.anio === f.getFullYear() && x.mes === f.getMonth()); if (mes) mes.v++; });
  return meses;
}

function renderDashboard() {
  const cases = getCases();
  $("#roleDashboardExtras").innerHTML = "";
  // Las cuatro tarjetas llegan calculadas desde el backend; aquí solo se elige el icono
  const iconos = state.role === "admin" ? [SIDEBAR_ICONS.adminPerson, SIDEBAR_ICONS.checkCircle, SIDEBAR_ICONS.fiscalFolder, SIDEBAR_ICONS.adminBar]
    : state.role === "fiscal" ? [SIDEBAR_ICONS.fiscalFolder, SIDEBAR_ICONS.hourglass, SIDEBAR_ICONS.search, SIDEBAR_ICONS.checkCircle]
    : [SIDEBAR_ICONS.fileFill, SIDEBAR_ICONS.clipboardCheck, SIDEBAR_ICONS.send, SIDEBAR_ICONS.bell];
  $("#statCards").innerHTML = indicadores.map((s, i) => `<article class="stat-card"><div class="stat-top">${escapeHtml(s.titulo)}<span class="stat-icon">${iconos[i] || ""}</span></div><div class="stat-value">${escapeHtml(s.valor)}</div><div class="stat-caption">${escapeHtml(s.detalle)}</div></article>`).join("");
  $("#quickActions").innerHTML = ""; $(".quick-panel")?.classList.add("admin-quick-hidden");

  if (state.role === "admin") {
    $("#welcomeTitle").textContent = "Panel del Administrador";
    $("#welcomeSubtitle").textContent = "Supervisión general de la plataforma y sus indicadores institucionales.";
    // Denuncias por estado
    const states = ["En proceso", "En investigación", "Por resolver", "Con disposición", "Resuelto", "Archivado"], colors = ["#e6a23c", "#397db7", "#e8c34a", "#279b70", "#176b50", "#8996a5"];
    const stateLegend = states.map((name, i) => ({ name, count: cases.filter(c => c.status === name).length, color: colors[i] })).filter(x => x.count).map(x => `<div class="chart-legend-row"><span class="chart-dot" style="background:${x.color}"></span><span>${x.name}</span><b>${x.count}</b></div>`).join("");
    // Evolución mensual calculada con las fechas de registro reales
    const monthly = denunciasPorMes(7), max = Math.max(1, ...monthly.map(x => x.v));
    const bars = monthly.map(x => `<div class="admin-chart-col"><div class="admin-chart-bar" style="height:${Math.max(3, x.v / max * 120)}px" title="${x.v} denuncias"></div><small>${x.m}</small></div>`).join("");
    // Denuncias por categoría (tipo de delito)
    const cats = {}; cases.forEach(c => cats[c.crime] = (cats[c.crime] || 0) + 1);
    const catColors = ["#397db7", "#25a27a", "#e6a23c", "#8a78c5", "#d16c78", "#7994a8"];
    const catLegend = Object.entries(cats).sort((a, b) => b[1] - a[1]).slice(0, 6).map(([name, n], i) => `<div class="chart-legend-row"><span class="chart-dot" style="background:${catColors[i]}"></span><span>${escapeHtml(name)}</span><b>${n}</b></div>`).join("");
    $("#roleDashboardExtras").innerHTML = `<section class="role-dashboard-card admin-quick-top"><h3>Accesos rápidos</h3><p>Herramientas de administración y soporte</p><div class="role-quick-grid"><button class="role-quick-card" data-page="registerFunction"><span class="quick-symbol">${SIDEBAR_ICONS.personPlus}</span><span><b>Registrar funcionario</b><small>Crear cuenta institucional</small></span></button><button class="role-quick-card" data-page="users"><span class="quick-symbol">${SIDEBAR_ICONS.shieldCheck}</span><span><b>Asignar permisos</b><small>Administrar accesos por perfil</small></span></button><button class="role-quick-card" data-page="settings"><span class="quick-symbol">${SIDEBAR_ICONS.adminGear}</span><span><b>Configurar sistema</b><small>Preferencias generales</small></span></button><button class="role-quick-card" data-page="reports"><span class="quick-symbol">${SIDEBAR_ICONS.adminBar}</span><span><b>Ver reportes</b><small>Consultar indicadores</small></span></button></div></section><div class="role-dashboard-grid admin-chart-grid"><section class="role-dashboard-card"><h3>Denuncias por estados</h3><p>Distribución de las ${cases.length} denuncias registradas</p><div class="chart-legend">${stateLegend}</div></section><section class="role-dashboard-card"><h3>Evolución mensual de trámites</h3><p>Denuncias registradas en los últimos 7 meses</p><div class="admin-chart-bars">${bars}</div></section><section class="role-dashboard-card"><h3>Denuncias por categoría</h3><p>Clasificación por tipo de delito</p><div class="chart-legend">${catLegend}</div></section></div>`;
  } else if (state.role === "fiscal") {
    $("#welcomeTitle").textContent = "Panel del Fiscal"; $("#welcomeSubtitle").textContent = "Resumen de carpetas fiscales, diligencias e investigaciones asignadas.";
    $("#roleDashboardExtras").innerHTML = `<section class="role-dashboard-card quick-access-top"><h3>Accesos rápidos</h3><p>Gestión de la actividad fiscal</p><div class="role-quick-grid"><button class="role-quick-card" data-page="denuncias"><span class="quick-symbol">${SIDEBAR_ICONS.quickFiscalFolder}</span><span><b>Carpeta fiscal</b><small>Consultar carpetas asignadas</small></span></button><button class="role-quick-card" data-page="diligences"><span class="quick-symbol">${SIDEBAR_ICONS.quickFiscalDiligence}</span><span><b>Diligencias</b><small>Programar y revisar diligencias</small></span></button><button class="role-quick-card" data-page="provisions"><span class="quick-symbol">${SIDEBAR_ICONS.quickFiscalDispositions}</span><span><b>Disposiciones</b><small>Consultar decisiones fiscales</small></span></button><button class="role-quick-card" data-page="reports"><span class="quick-symbol">${SIDEBAR_ICONS.quickFiscalReports}</span><span><b>Reportes</b><small>Consultar resultados</small></span></button><button class="role-quick-card" data-page="chat"><span class="quick-symbol">${SIDEBAR_ICONS.quickFiscalChat}</span><span><b>Chat policial</b><small>Coordinar con funcionarios PNP</small></span></button></div></section><section class="role-dashboard-card recent-folders-card"><h3>Carpetas fiscales recientes</h3><p>Últimas carpetas asignadas para revisión</p><div class="table-scroll"><table><thead><tr><th>N.º de carpeta</th><th>Delito</th><th>Estado</th><th>Actualización</th></tr></thead><tbody>${cases.slice(0, 4).map(c => `<tr><td>${escapeHtml(c.carpetaFiscal || "Por asignar")}</td><td>${escapeHtml(c.crime)}</td><td>${statusTag(c.status)}</td><td>${fechaCorta(c.ultimaActualizacion)}</td></tr>`).join("")}</tbody></table></div></section>`;
  } else {
    $("#welcomeTitle").textContent = "Panel del Funcionario Policial"; $("#welcomeSubtitle").textContent = "Resumen de denuncias registradas, actuaciones y requerimientos fiscales.";
    $("#roleDashboardExtras").innerHTML = `<section class="role-dashboard-card quick-access-top"><h3>Accesos rápidos</h3><p>Herramientas frecuentes del funcionario policial</p><div class="role-quick-grid"><button class="role-quick-card" data-page="denuncias"><span class="quick-symbol">${SIDEBAR_ICONS.policeFolder}</span><span><b>Denuncias registradas</b><small>Consultar denuncias asignadas</small></span></button><button class="role-quick-card" data-page="tracking"><span class="quick-symbol">${SIDEBAR_ICONS.fiscalClipboard}</span><span><b>Actuaciones policiales</b><small>Registrar y revisar actuaciones</small></span></button><button class="role-quick-card" data-page="reports"><span class="quick-symbol">${SIDEBAR_ICONS.policeSend}</span><span><b>Informes a Fiscalía</b><small>Consultar informes remitidos</small></span></button><button class="role-quick-card" data-page="chat"><span class="quick-symbol">${SIDEBAR_ICONS.quickFiscalChat}</span><span><b>Chat institucional</b><small>Comunicarse con ciudadano y fiscal</small></span></button></div></section>`;
  }
  $("#recentTitle").textContent = state.role === "fiscal" ? "Carpetas fiscales recientes" : "Denuncias recientes";
  $("#recentRows").innerHTML = cases.slice(0, 5).map(c => `<tr><td><b>${escapeHtml(c.id)}</b></td><td>${escapeHtml(c.citizen)}</td><td>${escapeHtml(c.crime)}</td><td>${statusTag(c.status)}</td><td>${escapeHtml(c.date)}</td><td>${caseActions(c)}</td></tr>`).join("") || '<tr><td colspan="6">No hay denuncias registradas.</td></tr>';
}

/* ---------------------------------------------------------------------
   Módulos de apoyo: cada uno consulta su información al backend
   --------------------------------------------------------------------- */

// Encabezado común de los módulos
const shell = (eyebrow, title, subtitle, body) => `<div class="welcome-row"><div><p class="eyebrow">${eyebrow}</p><h2>${title}</h2><p class="muted">${subtitle}</p></div><span class="date-pill">Datos de la base de datos</span></div>${body}`;
// Tabla genérica: los valores ya deben venir escapados o como etiquetas HTML
const table = (headers, rows) => `<div class="panel"><div class="table-scroll"><table><thead><tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows.map(r => `<tr>${r.map(v => `<td>${v}</td>`).join("")}</tr>`).join("") || `<tr><td colspan="${headers.length}">No hay registros.</td></tr>`}</tbody></table></div></div>`;
// Color de la etiqueta según el estado
const colorEstado = estado => ["Completada", "Validado", "Emitida", "Recibido"].includes(estado) ? "green" : ["En proceso", "Requerimiento", "Remitido", "Notificada"].includes(estado) ? "amber" : ["Programada", "En espera"].includes(estado) ? "yellow" : "gray";

async function renderRoleModule(page) {
  const mount = document.getElementById(page + "Page"); if (!mount) return;
  const e = escapeHtml;
  try {
    if (page === "functions") {
      renderFuncionarios(mount); // modulos.js: incluye las acciones de habilitar/deshabilitar
    } else if (page === "roles") {
      const rows = [["Administrador", "Gestión integral de la plataforma, reportes y supervisión", etiqueta("Activo", "green")], ["Funcionario policial", "Denuncias, actuaciones, documentos, informes y seguimiento fiscal", etiqueta("Activo", "green")], ["Funcionario fiscal", "Carpetas fiscales, actuaciones, diligencias y disposiciones", etiqueta("Activo", "green")]];
      mount.innerHTML = shell("SEGURIDAD", "Roles y permisos", "Responsabilidades y permisos por perfil.", table(["Perfil", "Alcance de funciones", "Estado"], rows));
    } else if (page === "activity") {
      const rows = (await API.get("/admin/auditoria")).map(r => [fechaHora(r.fechaHora), e(r.codigoUsuario), e(r.actividad), e(r.modulo), e(r.resultado)]);
      mount.innerHTML = shell("AUDITORÍA", "Registros de actividades", "Trazabilidad de las operaciones realizadas por los usuarios.", table(["Fecha y hora", "Usuario", "Actividad", "Módulo", "Resultado"], rows));
    } else if (page === "backup") {
      await renderCopias(mount);             // copias.js: crear, programar, historial y restaurar
    } else if (page === "tracking") {
      await renderActuaciones(mount);        // modulos.js: actuaciones con ventana de detalle
    } else if (page === "documents") {
      await renderDocumentos(mount);         // modulos.js: ver, descargar y subir archivos
    } else if (page === "fiscalTracking") {
      await renderSeguimientoFiscal(mount);  // modulos.js: etapa de la Fiscalía en cada denuncia
    } else if (page === "schedule") {
      await renderCalendarModule(mount, "Programación de actividades", "Calendario de actividades policiales, intervenciones y coordinaciones.");
    } else if (page === "diligences") {
      await renderCalendarModule(mount, "Diligencias", "Calendario de diligencias fiscales, audiencias y citaciones.");
    } else if (page === "provisions") {
      await renderDisposiciones(mount);      // modulos.js: disposiciones con ventana de detalle
    }
  } catch (error) {
    // Por ejemplo: el módulo no está habilitado para el funcionario
    mount.innerHTML = `<div class="empty-report">${e(error.message)}</div>`;
  }
}

// Calendario mensual con las actividades guardadas en la tabla actividad_programada
async function renderCalendarModule(mount, title, description) {
  const eventos = (await API.get("/agenda")).map(a => ({ fecha: new Date(a.fechaHora), time: a.fechaHora.slice(11, 16), name: a.nombre, caseId: a.referencia, place: a.lugar || "" }));
  const hoy = new Date();
  if (!state.calendar) state.calendar = { year: hoy.getFullYear(), month: hoy.getMonth() };

  const dibujar = () => {
    const { year, month } = state.calendar;
    const days = new Date(year, month + 1, 0).getDate();            // cantidad de días del mes
    const offset = (new Date(year, month, 1).getDay() + 6) % 7;     // columnas vacías antes del día 1 (semana de lunes a domingo)
    const delMes = eventos.filter(ev => ev.fecha.getFullYear() === year && ev.fecha.getMonth() === month);
    const nombreMes = new Date(year, month, 1).toLocaleDateString("es-PE", { month: "long", year: "numeric" });
    const cells = Array.from({ length: offset }, () => '<div class="calendar-cell empty"></div>');
    for (let day = 1; day <= days; day++) {
      const esHoy = day === hoy.getDate() && month === hoy.getMonth() && year === hoy.getFullYear();
      cells.push(`<div class="calendar-cell ${esHoy ? "today" : ""}"><b>${day}</b>${delMes.filter(ev => ev.fecha.getDate() === day).map(ev => `<button class="calendar-event" type="button" data-calendar-event="${escapeHtml(ev.caseId)}" data-calendar-place="${escapeHtml(ev.place)}" title="${escapeHtml(ev.name)} · ${ev.time}">${ev.time} · ${escapeHtml(ev.name)}</button>`).join("")}</div>`);
    }
    mount.innerHTML = shell("AGENDA Y PLANIFICACIÓN", title, description, `<section class="panel calendar-panel"><div class="calendar-heading"><button class="outline-btn" type="button" id="calendarPrev" aria-label="Mes anterior">‹</button><h3>${nombreMes.charAt(0).toUpperCase() + nombreMes.slice(1)}</h3><button class="outline-btn" type="button" id="calendarNext" aria-label="Mes siguiente">›</button></div><div class="calendar-grid calendar-weekdays">${["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"].map(d => `<b>${d}</b>`).join("")}</div><div class="calendar-grid">${cells.join("")}</div></section><section class="panel calendar-agenda"><h3>Actividades del mes</h3>${delMes.map(ev => `<div class="agenda-row"><span class="agenda-date"><b>${String(ev.fecha.getDate()).padStart(2, "0")}</b><small>${nombreMes.slice(0, 3).toUpperCase()}</small></span><span><b>${escapeHtml(ev.name)}</b><small>${escapeHtml(ev.caseId)} · ${escapeHtml(ev.place)}</small></span><strong>${ev.time}</strong></div>`).join("") || '<p class="muted">No hay actividades programadas para este mes.</p>'}</section>`);
    // Botones para cambiar de mes
    const mover = pasos => { const f = new Date(year, month + pasos, 1); state.calendar = { year: f.getFullYear(), month: f.getMonth() }; dibujar(); };
    mount.querySelector("#calendarPrev").addEventListener("click", () => mover(-1));
    mount.querySelector("#calendarNext").addEventListener("click", () => mover(1));
    mount.querySelectorAll("[data-calendar-event]").forEach(btn => btn.addEventListener("click", () => toast(`${btn.title} · ${btn.dataset.calendarEvent} · ${btn.dataset.calendarPlace}`)));
  };
  dibujar();
}

/* ---------------------------------------------------------------------
   Listado de denuncias / carpetas fiscales
   --------------------------------------------------------------------- */

function renderCases() {
  const cases = getCases(), police = state.role === "policia";
  $("#caseCount").textContent = cases.length + " registros";
  $("#denunciasDescription").textContent = police ? "Consulta todos los datos de las denuncias asignadas y accede al detalle para registrar actualizaciones." : "Consulta y administra los expedientes de tu competencia.";
  const q = ($("#searchInput")?.value || "").toLowerCase(), filter = $("#statusFilter")?.value || "";
  const headers = police
    ? ["Código de denuncia", "Tipo de denuncia", "Despacho fiscal", "Fiscal responsable", "Nombre completo del ciudadano", "DNI", "Correo electrónico", "Teléfono", "Fecha de registro", "Última actualización", "Lugar de los hechos", "Descripción de los hechos", "Estado", "Acciones"]
    : ["N.º denuncia", "Ciudadano", "Delito", "Estado", "Fecha", "Acciones"];
  $("#caseRows").closest("table").querySelector("thead").innerHTML = `<tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr>`;
  // Filtro por estado y búsqueda de texto en todos los campos
  const filtered = cases.filter(c => (!filter || c.status === filter) && [c.id, c.citizen, c.crime, c.despachoFiscal, c.fiscalResponsable, c.ciudadanoCompleto, c.dniCiudadano, c.correoCiudadano, c.telefonoCiudadano, c.lugarHechos, c.description].join(" ").toLowerCase().includes(q));
  $("#caseRows").innerHTML = filtered.map(c => police
    ? `<tr>
      <td><b>${escapeHtml(c.id)}</b></td><td>${escapeHtml(c.tipoDenuncia)}</td>
      <td class="case-cell-wrap">${escapeHtml(c.despachoFiscal || "No registrado")}</td>
      <td>${escapeHtml(c.fiscalResponsable || "No registrado")}</td>
      <td>${escapeHtml(c.ciudadanoCompleto)}</td><td>${escapeHtml(c.dniCiudadano || "No registrado")}</td>
      <td>${escapeHtml(c.correoCiudadano || "No registrado")}</td><td>${escapeHtml(c.telefonoCiudadano || "No registrado")}</td>
      <td>${escapeHtml(formatCaseDate(c.fechaRegistro))}</td><td>${escapeHtml(formatCaseDate(c.ultimaActualizacion))}</td>
      <td class="case-cell-wrap">${escapeHtml(c.lugarHechos || "No registrado")}</td>
      <td class="case-cell-description">${escapeHtml(c.description)}</td><td>${statusTag(c.status)}</td><td>${caseActions(c)}</td></tr>`
    : `<tr><td><b>${escapeHtml(c.id)}</b></td><td>${escapeHtml(c.citizen)}</td><td>${escapeHtml(c.crime)}</td><td>${statusTag(c.status)}</td><td>${escapeHtml(c.date)}</td><td>${caseActions(c)}</td></tr>`
  ).join("") || `<tr><td colspan="${headers.length}">No se encontraron denuncias.</td></tr>`;
}
$("#searchInput").addEventListener("input", renderCases); $("#statusFilter").addEventListener("change", renderCases);
document.addEventListener("click", e => { const b = e.target.closest("[data-detail]"); if (b) { state.selectedId = b.dataset.detail; goPage("detail"); } });
document.addEventListener("click", e => { const b = e.target.closest("[data-preview]"); if (b) openCasePreview(b.dataset.preview); });

function formatCaseDate(value) {
  if (!value) return "No registrado";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-PE", { dateStyle: "medium", timeStyle: "short" });
}
function policeCaseField(label, value, wide = false) {
  return `<div class="form-field police-case-field ${wide ? "field-wide" : ""}"><label>${label}</label><input type="text" value="${escapeHtml(value || "No registrado")}" readonly></div>`;
}

/* ---------------------------------------------------------------------
   Detalle de una denuncia y registro de actualizaciones
   --------------------------------------------------------------------- */

// Historial de cambios guardado en la tabla seguimiento_denuncia
function historialHtml(c) {
  const filas = c.historial.map(h => `<div class="detail-list"><span>${fechaHora(h.fechaHora)}</span><b>${statusTag(h.estado)} ${escapeHtml(h.usuario)} · ${escapeHtml(h.perfil)}${h.observacion ? "<br>" + escapeHtml(h.observacion) : ""}</b></div>`).join("");
  return `<h3>Historial de seguimiento</h3>${filas || '<p class="muted">Aún no se registran actualizaciones para esta denuncia.</p>'}`;
}

async function renderDetail() {
  let c;
  try { c = aCaso(await API.get("/denuncias/" + encodeURIComponent(state.selectedId))); }
  catch (error) { toast(error.message); goPage("denuncias"); return; }
  const fiscal = state.role === "fiscal", police = state.role === "policia", admin = state.role === "admin";
  if (police) {
    const fields = [
      policeCaseField("Código de denuncia", c.id),
      policeCaseField("Tipo de denuncia", c.tipoDenuncia),
      policeCaseField("Despacho fiscal", c.despachoFiscal),
      policeCaseField("Fiscal responsable", c.fiscalResponsable),
      policeCaseField("Nombre completo del ciudadano", c.ciudadanoCompleto),
      policeCaseField("DNI del ciudadano", c.dniCiudadano),
      policeCaseField("Correo electrónico del ciudadano", c.correoCiudadano),
      policeCaseField("Teléfono del ciudadano", c.telefonoCiudadano),
      policeCaseField("Fecha de registro", formatCaseDate(c.fechaRegistro)),
      policeCaseField("Última actualización", formatCaseDate(c.ultimaActualizacion)),
      policeCaseField("Lugar de los hechos", c.lugarHechos, true),
      `<div class="form-field police-case-field field-wide"><label>Descripción de los hechos</label><textarea readonly rows="4">${escapeHtml(c.description)}</textarea></div>`
    ].join("");
    $("#detailContent").innerHTML = `<section class="panel police-case-panel">
      <div class="panel-heading"><div><p class="eyebrow">EXPEDIENTE POLICIAL</p><h2>Denuncia asignada — ${escapeHtml(c.id)}</h2><p class="muted">Datos registrados y remitidos para la atención policial.</p></div>${statusTag(c.status)}</div>
      <div class="police-case-section-title">Información de la denuncia</div>
      <div class="police-case-fields">${fields}</div>
      <div class="police-case-note"><svg viewBox="0 0 24 24"><path d="M12 3 4 6v5c0 5 3.4 8.5 8 10 4.6-1.5 8-5 8-10V6l-8-3Z"/><path d="M12 8v4m0 4h.01"/></svg>Los datos de identificación, contacto y derivación se muestran como información de referencia. Para corregirlos, debe actualizarse el registro de origen.</div>
    </section>
    <section class="panel police-update-panel"><h2>Actualizar atención policial</h2><p class="muted">Registra el avance de la denuncia y agrega observaciones al expediente.</p>
      <form id="updateForm"><div class="form-field"><label for="newStatus">Actualizar estado</label><select id="newStatus">${["En proceso", "Por resolver", "Resuelto"].map(status => `<option ${status === c.status ? "selected" : ""}>${status}</option>`).join("")}</select></div>
      <div class="form-field"><label for="comments">Justificación de la actuación <span class="req-note">(obligatoria)</span></label><textarea id="comments" maxlength="1000" minlength="10" required placeholder="Describe la diligencia o el motivo del cambio de estado..."></textarea><small class="field-help">Registrar una actuación justificada reinicia el contador de días sin actividad de la denuncia.</small></div>
      <button class="primary-btn" type="submit">Guardar cambios</button></form>
    </section>
    <section class="panel police-update-panel">${historialHtml(c)}</section>`;
  } else {
    // Vista del fiscal (con formulario de disposición) y del administrador (solo lectura)
    const formulario = admin ? `<section class="panel"><h2>Consulta del administrador</h2><p class="muted">El administrador supervisa los expedientes en modo de solo lectura.</p>${historialHtml(c)}</section>`
      : `<section class="panel"><h2>Registrar disposición fiscal</h2><p class="muted">Registra la actuación o disposición correspondiente.</p><form id="updateForm"><div class="form-field"><label for="newStatus">Tipo de disposición</label><select id="newStatus">${["En investigación", "Diligencias", "Con disposición", "Archivado"].map(s => `<option ${s === c.status ? "selected" : ""}>${s}</option>`).join("")}</select></div><div class="form-field"><label for="comments">Descripción de la disposición <span class="req-note">(obligatoria)</span></label><textarea id="comments" maxlength="1000" minlength="10" required placeholder="Describe la actuación o disposición fiscal..."></textarea><small class="field-help">Registrar una actuación justificada reinicia el contador de días sin actividad de la denuncia.</small></div><button class="primary-btn full" type="submit">Registrar disposición</button></form>${historialHtml(c)}</section>`;
    $("#detailContent").innerHTML = `<div class="detail-grid"><section class="panel"><div class="panel-heading"><div><h2>Detalle de denuncia — ${escapeHtml(c.id)}</h2><p class="muted">Información del expediente seleccionado</p></div>${statusTag(c.status)}</div><div class="detail-list"><span>Ciudadano</span><b>${escapeHtml(c.citizen)}</b></div><div class="detail-list"><span>Delito</span><b>${escapeHtml(c.crime)}</b></div><div class="detail-list"><span>Carpeta fiscal</span><b>${escapeHtml(c.carpetaFiscal || "Por asignar")}</b></div><div class="detail-list"><span>Fecha de registro</span><b>${escapeHtml(c.date)}</b></div><div class="detail-list"><span>Estado actual</span><b>${statusTag(c.status)}</b></div><h3>Descripción</h3><div class="description-box">${escapeHtml(c.description)}</div></section>${formulario}</div>`;
  }
  // Guardar: el backend cambia el estado y agrega el registro al historial
  $("#updateForm")?.addEventListener("submit", async e => {
    e.preventDefault();
    if ($("#comments").value.trim().length < 10) { toast("Escribe la justificación de la actuación (al menos 10 caracteres)."); $("#comments").focus(); return; }
    try {
      await API.put("/denuncias/" + encodeURIComponent(c.id), { estado: $("#newStatus").value, comentario: $("#comments").value.trim() });
      toast(fiscal ? "Disposición registrada correctamente." : "Cambios guardados correctamente.");
      await recargarCasos(); renderDetail();
    } catch (error) { toast(error.message); }
  });
}

/* ---------------------------------------------------------------------
   Reportes
   --------------------------------------------------------------------- */

function renderReports() {
  // El administrador ve el tablero completo de estadísticas en lugar de los dos gráficos básicos
  const esAdmin = state.role === "admin";
  $("#adminDashboard").classList.toggle("hidden", !esAdmin);
  $("#reportChartsTitle").classList.toggle("hidden", esAdmin); $("#reportChartsGrid").classList.toggle("hidden", esAdmin);
  $("#inactividadDashboard").classList.toggle("hidden", !esAdmin);
  if (esAdmin) { cargarEstadisticasAdmin(); cargarInactividad(); }
  const cases = getCases(), total = cases.length;
  const resolved = cases.filter(c => ["Resuelto", "Archivado", "Con disposición"].includes(c.status)).length;
  const inProgress = cases.filter(c => ["En proceso", "En investigación", "Diligencias"].includes(c.status)).length;
  const overdue = Math.max(0, total - resolved - inProgress);
  // Gráfico de barras: denuncias registradas por mes
  const monthly = denunciasPorMes(7), maxMonthly = Math.max(1, ...monthly.map(x => x.v));
  $("#reportMonthlyChart").innerHTML = monthly.map(x => `<div class="monthly-column"><div class="monthly-bar-wrap"><div class="monthly-bar" style="height:${x.v / maxMonthly * 100}%" title="${x.v} denuncias"></div></div><span class="monthly-label">${x.m}</span></div>`).join("");
  // Variación del mes actual respecto al anterior (solo si hay dato para comparar)
  const actual = monthly[6].v, anterior = monthly[5].v, trend = $("#reportTrend");
  trend.classList.toggle("hidden", anterior === 0);
  if (anterior > 0) { const variacion = (actual - anterior) / anterior * 100; trend.textContent = (variacion >= 0 ? "+" : "") + variacion.toFixed(1) + "%"; }
  // Gráfico circular por estado
  const denominator = total || 1, p1 = inProgress / denominator * 100, p2 = resolved / denominator * 100;
  $("#reportDonut").style.background = `conic-gradient(#3989bd 0% ${p1}%,#16a477 ${p1}% ${p1 + p2}%,#f4a51c ${p1 + p2}% 100%)`;
  $("#reportDonutTotal").textContent = total;
  $("#reportDonutLegend").innerHTML = `<div class="donut-legend-item"><span class="legend-dot" style="background:#3989bd"></span><span>En proceso</span><b>${inProgress}</b></div><div class="donut-legend-item"><span class="legend-dot" style="background:#16a477"></span><span>Resueltas</span><b>${resolved}</b></div><div class="donut-legend-item"><span class="legend-dot" style="background:#f4a51c"></span><span>Por vencer</span><b>${overdue}</b></div>`;
  // Policía: informes remitidos y respuesta de la Fiscalía. Todos: consulta de reportes con filtros (modulos.js)
  renderInformesFiscalia();
  iniciarReportes();
}

/* ---------------------------------------------------------------------
   Administración: registro de funcionarios, cuentas y permisos
   --------------------------------------------------------------------- */

// Registrar funcionario: el backend valida, cifra la contraseña y guarda en la tabla usuario
$("#registerFunctionForm").addEventListener("submit", async e => {
  e.preventDefault();
  const feedback = $("#registerFunctionFeedback"), password = $("#newFunctionPassword").value;
  if (password !== $("#newFunctionPasswordConfirm").value) { feedback.textContent = "Las contraseñas no coinciden."; return; }
  try {
    const creado = await API.post("/usuarios", {
      rol: $("#newFunctionRole").value, nombres: $("#newFunctionNames").value.trim(), apellidos: $("#newFunctionLastnames").value.trim(),
      dni: $("#newFunctionDni").value.trim(), fechaNacimiento: $("#newFunctionBirth").value || null, telefono: $("#newFunctionPhone").value.trim(),
      correo: $("#newFunctionEmail").value.trim(), institucion: $("#newFunctionInstitution").value, dependencia: $("#newFunctionOffice").value.trim(),
      codigo: $("#newFunctionCode").value.trim(), password, habilitado: $("#newFunctionStatus").value === "true"
    });
    usuarios = await API.get("/usuarios"); renderUsers(); indicadores = await cargarSeguro("/panel/indicadores"); renderDashboard();
    feedback.textContent = `Funcionario ${creado.codigo} registrado correctamente.`;
    e.target.reset(); toast("Registro completado.");
  } catch (error) { feedback.textContent = error.message; }
});

function renderUsers() {
  const admin = state.role === "admin";
  $("#userRows").innerHTML = usuarios.map(u => `<tr><td><b>${escapeHtml(u.codigo)}</b></td><td>${escapeHtml(u.nombreCompleto)}</td><td>${escapeHtml(u.institucion)}</td><td>${escapeHtml(u.perfil)}</td><td>${etiqueta(u.habilitado ? "Activo" : "Deshabilitado", u.habilitado ? "green" : "gray")}</td><td>${accionesUsuario(u)}</td></tr>`).join("");
  $("#userAdminTools")?.classList.toggle("hidden", !admin); $("#adminPermissions")?.classList.toggle("hidden", !admin);
  // El selector de permisos se llena con los funcionarios de la base de datos
  const select = $("#permissionUser"), anterior = select.value;
  select.innerHTML = usuarios.filter(u => u.rol !== "admin").map(u => `<option value="${escapeHtml(u.codigo)}">${escapeHtml(u.nombreCompleto)} · ${escapeHtml(u.perfil)}</option>`).join("");
  if ([...select.options].some(o => o.value === anterior)) select.value = anterior;
  mostrarPermisosSeleccionados();
}

// Marca las casillas con los permisos que el funcionario elegido tiene guardados
function mostrarPermisosSeleccionados() {
  const usuario = usuarios.find(u => u.codigo === $("#permissionUser").value);
  $$("#permissionCheckGrid input").forEach(casilla => casilla.checked = Boolean(usuario?.permisos.includes(casilla.value)));
}
$("#permissionUser").addEventListener("change", mostrarPermisosSeleccionados);

$("#saveUserPermissions").addEventListener("click", async () => {
  const code = $("#permissionUser").value; if (!code) return;
  const modulos = $$("#permissionCheckGrid input:checked").map(x => x.value);
  try {
    const actualizado = await API.put(`/usuarios/${encodeURIComponent(code)}/permisos`, { modulos });
    usuarios = usuarios.map(u => u.codigo === actualizado.codigo ? actualizado : u);
    $("#permissionFeedback").textContent = `${modulos.length} permisos habilitados para ${code}.`;
    toast("Permisos actualizados.");
  } catch (error) { toast(error.message); }
});

// Habilitar o deshabilitar una cuenta
$("#userRows").addEventListener("click", async e => {
  const btn = e.target.closest("[data-user-toggle]"); if (!btn) return;
  try {
    const actualizado = await API.patch(`/usuarios/${encodeURIComponent(btn.dataset.userToggle)}/estado`);
    usuarios = usuarios.map(u => u.codigo === actualizado.codigo ? actualizado : u);
    renderUsers(); indicadores = await cargarSeguro("/panel/indicadores"); renderDashboard();
    toast(`Cuenta ${actualizado.habilitado ? "habilitada" : "deshabilitada"}: ${actualizado.codigo}`);
  } catch (error) { toast(error.message); }
});

/* ---------------------------------------------------------------------
   Deberes pendientes (campana de notificaciones)
   --------------------------------------------------------------------- */

function renderDuties() {
  // Se muestran las tres primeras denuncias que aún no están cerradas
  const pending = getCases().filter(c => !["Resuelto", "Archivado"].includes(c.status)).slice(0, 3), admin = state.role === "admin";
  $("#notificationCount").textContent = pending.length;
  $("#notificationCount").classList.toggle("hidden", pending.length === 0);
  $("#dutyList").innerHTML = pending.map((c, i) => `<button class="duty-item" ${admin ? "data-preview" : "data-detail"}="${escapeHtml(c.id)}"><span class="duty-icon"><svg viewBox="0 0 24 24"><path d="M8 4h8M9 3v3m6-3v3M5 8h14v12H5z"/><path d="M8 12h8M8 16h5"/></svg></span><span class="duty-copy"><b>${i === 0 ? "Revisar diligencias pendientes" : "Actualizar seguimiento"} · ${escapeHtml(c.id)}</b><small>${escapeHtml(c.citizen)} — ${escapeHtml(c.crime)}</small></span><span class="duty-time">${i === 0 ? "Hoy" : "Pendiente"}</span></button>`).join("") || '<div class="notification-footer">No tienes deberes pendientes.</div>';
}

/* ---------------------------------------------------------------------
   Chat: los mensajes se guardan en las tablas conversacion y mensaje
   --------------------------------------------------------------------- */

function activeChats() { return conversaciones; }
function chatActual() { return conversaciones.find(item => item.id === state.selectedChat); }
function totalUnread() { return conversaciones.reduce((sum, chat) => sum + chat.unread, 0); }
function updateChatBadge() { const badge = $("#chatBadge"); if (!badge) return; const count = totalUnread(); badge.textContent = count; badge.classList.toggle("hidden", count === 0); }

function renderChat() {
  const query = ($("#chatSearch")?.value || "").toLowerCase();
  $("#chatHeading").textContent = state.role === "admin" ? "Consultas de soporte" : state.role === "fiscal" ? "Coordinación policial" : "Consultas ciudadanas";
  $("#chatDescription").textContent = state.role === "admin" ? "Mensajes de funcionarios" : "Mensajes sobre denuncias asignadas";
  $("#chatSearch").placeholder = state.role === "admin" ? "Buscar funcionario o consulta..." : state.role === "fiscal" ? "Buscar funcionario o denuncia..." : "Buscar ciudadano, fiscal o denuncia...";
  const filtered = activeChats().filter(chat => (chat.citizen + " " + chat.caseId).toLowerCase().includes(query));
  $("#conversationList").innerHTML = filtered.map(chat => { const last = chat.messages.at(-1) || { text: "Sin mensajes", time: "" }; return `<button class="conversation ${chat.id === state.selectedChat ? "active" : ""}" data-chat="${chat.id}"><span class="citizen-avatar">${iniciales(chat.citizen)}${chat.online ? '<i class="online-dot"></i>' : ""}</span><span class="conversation-copy"><b>${escapeHtml(chat.citizen)}</b><small>${escapeHtml(last.text)}</small></span><span class="conversation-meta"><time>${last.time}</time>${chat.unread ? `<i class="unread-dot">${chat.unread}</i>` : ""}</span></button>`; }).join("") || '<div class="notification-footer">No se encontraron conversaciones.</div>';
  renderActiveChat(); updateChatBadge();
}

// Contenido de un mensaje: texto o archivo adjunto (imagen, video, audio o documento)
function chatAttachmentMarkup(message) {
  if (!message.attachment) return escapeHtml(message.text);
  const a = message.attachment, name = escapeHtml(a.name || "Archivo adjunto"), url = escapeHtml(a.url);
  if (a.kind === "image") return `<a class="chat-attachment" href="${url}" target="_blank" rel="noopener"><img src="${url}" alt="${name}"><span>${name}</span></a>`;
  if (a.kind === "video") return `<div class="chat-attachment"><video controls preload="metadata" src="${url}"></video><span>${name}</span></div>`;
  if (a.kind === "audio") return `<div class="chat-audio"><audio controls src="${url}"></audio><span>${name}</span></div>`;
  return `<a class="chat-file-attachment" href="${url}" download="${name}"><span class="file-attachment-icon">▤</span><span><b>${name}</b><small>${Math.max(1, Math.round((a.size || 0) / 1024))} KB · Descargar archivo</small></span></a>`;
}

function renderActiveChat() {
  const chat = chatActual();
  if (!chat) { $("#chatMain").innerHTML = `<div class="chat-empty"><div><b>Selecciona una conversación</b>${state.role === "admin" ? "Selecciona una conversación para atender consultas de funcionarios policiales o fiscales." : state.role === "fiscal" ? "Coordina actuaciones y requerimientos con funcionarios policiales." : "Revisa las consultas relacionadas con tus denuncias."}</div></div>`; return; }
  const phone = chat.phone.replace(/[^\d+]/g, "");
  // El botón de llamada solo aparece cuando la otra persona es un ciudadano con teléfono registrado
  const llamada = phone ? `<a class="call-btn" href="tel:${phone}" title="Llamar al ciudadano" aria-label="Llamar al ciudadano"><svg viewBox="0 0 24 24"><path d="M22 16.9v3a2 2 0 0 1-2.2 2A19.8 19.8 0 0 1 11.2 18a19.4 19.4 0 0 1-6-6A19.8 19.8 0 0 1 2.1 3.2 2 2 0 0 1 4.1 1h3a2 2 0 0 1 2 1.7c.1 1 .4 2 .7 2.9a2 2 0 0 1-.5 2.1L8 8.9a16 16 0 0 0 6 6l1.2-1.3a2 2 0 0 1 2.1-.5c.9.3 1.9.6 2.9.7a2 2 0 0 1 1.8 2.1Z"/></svg></a>` : "";
  $("#chatMain").innerHTML = `<header class="chat-header"><div class="chat-person"><span class="citizen-avatar">${iniciales(chat.citizen)}${chat.online ? '<i class="online-dot"></i>' : ""}</span><span><b>${escapeHtml(chat.citizen)}</b><small>${chat.online ? "En línea · atención activa" : "Última conexión reciente"}</small></span></div><div class="chat-header-actions">${llamada}<button class="case-link" data-preview="${escapeHtml(chat.caseId)}">${escapeHtml(chat.caseId)}</button></div></header><div class="messages" id="messages">${chat.messages.map(message => `<div class="message-row ${message.mine ? "mine" : ""}"><div class="message">${chatAttachmentMarkup(message)}</div><span class="message-time">${message.time}${message.mine ? " · Enviado" : ""}</span></div>`).join("")}</div><form class="chat-compose" id="chatForm">
    <div class="chat-attach-wrap"><button class="compose-tool" id="attachToggle" type="button" aria-label="Adjuntar archivos" title="Adjuntar archivos"><svg viewBox="0 0 24 24"><path d="m21.4 11.1-8.5 8.5a6 6 0 0 1-8.5-8.5l9.2-9.2a4 4 0 0 1 5.7 5.7l-9.2 9.2a2 2 0 0 1-2.8-2.8l8.5-8.5"/></svg></button><div class="attachment-menu hidden" id="attachmentMenu"><button type="button" data-pick="media">Fotos y videos</button><button type="button" data-pick="files">Documentos y archivos</button><button type="button" data-pick="audio">Enviar audio</button></div></div>
    <input id="mediaFileInput" type="file" accept="image/*,video/*" multiple hidden><input id="documentFileInput" type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,application/*" multiple hidden><input id="audioFileInput" type="file" accept="audio/*" hidden>
    <button class="compose-tool audio-record-btn" id="recordAudioBtn" type="button" aria-label="Grabar audio" title="Grabar audio"><svg viewBox="0 0 24 24"><rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10a7 7 0 0 0 14 0M12 17v5m-4 0h8"/></svg></button>
    <input id="chatMessage" autocomplete="off" maxlength="2000" placeholder="${state.role === "admin" ? "Escribe una consulta o respuesta de soporte..." : state.role === "fiscal" ? "Escribe un mensaje al funcionario policial..." : "Escribe una respuesta..."}" aria-label="Mensaje"><button class="send-btn" type="submit" aria-label="Enviar mensaje"><svg viewBox="0 0 24 24"><path d="m3 11 17-8-7 18-2-7-8-3Z"/><path d="m11 14 9-11"/></svg></button></form>`;
  requestAnimationFrame(() => { const messages = $("#messages"); if (messages) messages.scrollTop = messages.scrollHeight; });
  $("#chatForm").addEventListener("submit", sendChatMessage);
  $("#attachToggle").addEventListener("click", () => $("#attachmentMenu").classList.toggle("hidden"));
  $("#attachmentMenu").addEventListener("click", e => { const pick = e.target.closest("[data-pick]"); if (!pick) return; $("#attachmentMenu").classList.add("hidden"); $({ media: "#mediaFileInput", files: "#documentFileInput", audio: "#audioFileInput" }[pick.dataset.pick]).click(); });
  ["#mediaFileInput", "#documentFileInput", "#audioFileInput"].forEach(selector => $(selector).addEventListener("change", async e => { for (const file of [...e.target.files]) await addChatAttachment(file); e.target.value = ""; }));
  $("#recordAudioBtn").addEventListener("click", toggleAudioRecording);
}

// Sube un archivo al backend; este lo guarda en disco y registra el mensaje
async function addChatAttachment(file) {
  const chat = chatActual(); if (!chat || !file) return;
  if (file.size > 20 * 1024 * 1024) return toast("El archivo supera el tamaño máximo permitido (20 MB).");
  const formulario = new FormData(); formulario.append("archivo", file);
  try {
    conversaciones = (await API.post(`/chat/conversaciones/${chat.id}/adjuntos`, formulario)).map(aChat);
    renderChat(); toast("Archivo enviado.");
  } catch (error) { toast(error.message); }
}

// Grabación de audio con el micrófono (cuando el navegador lo permite)
let chatRecorder = null, chatRecordingStream = null, chatAudioChunks = [];
async function toggleAudioRecording() {
  const button = $("#recordAudioBtn");
  if (chatRecorder && chatRecorder.state === "recording") { chatRecorder.stop(); button.classList.remove("recording"); button.title = "Grabar audio"; return; }
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) { toast("La grabación de audio no está disponible en este navegador o contexto. Puedes adjuntar un audio existente."); return; }
  try {
    chatRecordingStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    chatAudioChunks = []; chatRecorder = new MediaRecorder(chatRecordingStream);
    chatRecorder.ondataavailable = e => { if (e.data.size) chatAudioChunks.push(e.data); };
    chatRecorder.onstop = () => { const blob = new Blob(chatAudioChunks, { type: chatRecorder.mimeType || "audio/webm" }); const ext = blob.type.includes("ogg") ? "ogg" : blob.type.includes("mp4") ? "m4a" : "webm"; const file = new File([blob], `audio_${Date.now()}.${ext}`, { type: blob.type }); chatRecordingStream?.getTracks().forEach(track => track.stop()); chatRecordingStream = null; chatRecorder = null; addChatAttachment(file); };
    chatRecorder.start(); button.classList.add("recording"); button.title = "Detener y enviar audio"; toast("Grabando audio. Presiona nuevamente para detener y enviar.");
  } catch (error) { toast("No se pudo acceder al micrófono. Revisa los permisos del navegador."); }
}

// Envía un mensaje de texto
async function sendChatMessage(e) {
  e.preventDefault(); const input = $("#chatMessage"), texto = input.value.trim(), chat = chatActual(); if (!texto || !chat) return;
  try {
    conversaciones = (await API.post(`/chat/conversaciones/${chat.id}/mensajes`, { texto })).map(aChat);
    renderChat();
  } catch (error) { toast(error.message); }
}

// Al abrir una conversación, sus mensajes recibidos se marcan como leídos
$("#conversationList").addEventListener("click", async e => {
  const button = e.target.closest("[data-chat]"); if (!button) return;
  state.selectedChat = Number(button.dataset.chat); renderChat();
  const chat = chatActual(); if (!chat || !chat.unread) return;
  try { conversaciones = (await API.post(`/chat/conversaciones/${chat.id}/leida`)).map(aChat); renderChat(); } catch (error) { toast(error.message); }
});
$("#chatSearch").addEventListener("input", renderChat);

// Cada 15 segundos se consultan mensajes nuevos. La conversación abierta solo se
// vuelve a dibujar si llegó algo y el funcionario no está escribiendo ni grabando.
setInterval(async () => {
  if (!state.user || document.hidden || !tienePermiso("chat")) return;
  let nuevas; try { nuevas = (await API.get("/chat/conversaciones")).map(aChat); } catch (error) { return; }
  if (!state.user) return;
  const contar = lista => lista.reduce((suma, chat) => suma + chat.messages.length, 0), huboCambios = contar(nuevas) !== contar(conversaciones);
  conversaciones = nuevas; updateChatBadge();
  if (huboCambios && state.page === "chat" && !$("#chatMessage")?.value && !chatRecorder) renderChat();
}, 15000);

/* ---------------------------------------------------------------------
   Fecha y hora del panel
   --------------------------------------------------------------------- */

function updateDateTime() {
  const now = new Date();
  $("#todayDate").innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg><span>${now.toLocaleDateString("es-PE", { day: "numeric", month: "long", year: "numeric" })} · ${now.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" })}</span>`;
}
setInterval(updateDateTime, 30000);

/* ---------------------------------------------------------------------
   Mi perfil
   --------------------------------------------------------------------- */

function renderProfile() {
  const u = state.user, p = profile();
  $("#profileDisplayName").textContent = u.nombreCompleto;
  $("#profileRankSummary").textContent = (u.rango || p.role) + " · " + p.institution;
  $("#profileInitials").textContent = iniciales(u.nombreCompleto);
  $("#profileName").value = u.nombreCompleto; $("#profileDni").value = u.dni;
  $("#profileCode").value = u.codigo; $("#profileRank").value = u.rango || p.role; $("#profileUnit").value = u.dependencia || "";
  $("#profileInstitution").value = u.institucion; $("#profilePhone").value = u.telefono || "";
  $("#profileEmail").value = u.correo || ""; $("#profileAddress").value = u.direccion || ""; $("#profileEmergency").value = u.contactoEmergencia || "";
  // Foto: si existe se pide al backend; el parámetro "t" evita que el navegador muestre una copia antigua
  const photo = $("#profilePhoto"); photo.querySelector("img")?.remove();
  $("#profileInitials").classList.toggle("hidden", u.tieneFoto);
  if (u.tieneFoto) { const img = document.createElement("img"); img.src = "/api/perfil/foto?t=" + Date.now(); img.alt = "Foto de perfil de " + u.nombreCompleto; photo.appendChild(img); }
}
$("#profileForm").addEventListener("submit", async e => {
  e.preventDefault();
  try {
    state.user = await API.put("/perfil", { telefono: $("#profilePhone").value.trim(), correo: $("#profileEmail").value.trim(), direccion: $("#profileAddress").value.trim(), contactoEmergencia: $("#profileEmergency").value.trim() });
    renderProfile(); toast("Tu información de contacto se actualizó correctamente.");
  } catch (error) { toast(error.message); }
});
$("#photoInput").addEventListener("change", async e => {
  const file = e.target.files[0]; e.target.value = ""; if (!file) return;
  if (file.size > 5 * 1024 * 1024) return toast("La fotografía no puede superar los 5 MB.");
  const formulario = new FormData(); formulario.append("foto", file);
  try { state.user = await API.post("/perfil/foto", formulario); renderProfile(); toast("Foto de perfil actualizada."); } catch (error) { toast(error.message); }
});

/* ---------------------------------------------------------------------
   Vista rápida de una denuncia (ventana de solo lectura)
   --------------------------------------------------------------------- */

function openCasePreview(id) {
  const c = getCases().find(item => item.id === id); if (!c) return;
  $("#previewContent").innerHTML = `<div class="modal-kicker">EXPEDIENTE ${escapeHtml(c.id)}</div><div class="preview-grid"><div class="preview-item"><span>Ciudadano</span><b>${escapeHtml(c.citizen)}</b></div><div class="preview-item"><span>Delito</span><b>${escapeHtml(c.crime)}</b></div><div class="preview-item"><span>Fecha de registro</span><b>${escapeHtml(c.date)}</b></div><div class="preview-item"><span>Estado actual</span>${statusTag(c.status)}</div></div><div class="preview-description"><span>Descripción de la denuncia</span><div class="description-box">${escapeHtml(c.description)}</div></div><div class="modal-footer-note"><svg viewBox="0 0 24 24"><path d="M12 3 4 6v5c0 5 3.4 8.5 8 10 4.6-1.5 8-5 8-10V6l-8-3Z"/><path d="m9 12 2 2 4-4"/></svg>Vista de solo lectura.${state.role === "admin" ? "" : " Para realizar cambios, utiliza el icono de lápiz."}</div>`;
  $("#casePreview").classList.remove("hidden"); document.body.style.overflow = "hidden"; $("#closePreview").focus();
}
function closeCasePreview() { $("#casePreview").classList.add("hidden"); document.body.style.overflow = ""; }
$("#closePreview").addEventListener("click", closeCasePreview);
$("#casePreview").addEventListener("click", e => { if (e.target === $("#casePreview")) closeCasePreview(); });

/* ---------------------------------------------------------------------
   Al cargar la página: si el servidor ya tiene una sesión abierta
   (por ejemplo, tras recargar con F5) se entra directo al panel.
   --------------------------------------------------------------------- */
(async () => {
  try { const usuario = await API.get("/auth/sesion"); if (usuario) await iniciarAplicacion(usuario); }
  catch (error) { /* sin sesión: se queda en la pantalla de acceso */ }
})();

/* ---------------------------------------------------------------------
   Barra de accesibilidad (igual para los tres perfiles)
   --------------------------------------------------------------------- */

// --- Color: cada clic pasa al siguiente modo. Los colores de cada uno están en styles.css ---
// "corto" es el texto que se muestra en el botón para saber qué modo está activo.
const MODOS_COLOR = [
  { id: "normal", nombre: "vista normal", corto: "Color" },
  { id: "colorblind", nombre: "paleta general para daltonismo", corto: "Daltonismo" },
  { id: "protanopia", nombre: "protanopia (dificultad con el rojo)", corto: "Protanopia" },
  { id: "deuteranopia", nombre: "deuteranopia (dificultad con el verde)", corto: "Deuteranopia" },
  { id: "tritanopia", nombre: "tritanopia (dificultad con azul y amarillo)", corto: "Tritanopia" },
  { id: "mono", nombre: "acromatopsia (escala de grises)", corto: "Grises" },
  { id: "contrast", nombre: "alto contraste", corto: "Contraste" },
  { id: "dark", nombre: "modo oscuro", corto: "Oscuro" }
];
function aplicarModoColor(id) {
  const modo = MODOS_COLOR.find(m => m.id === id) || MODOS_COLOR[0];
  // Los colores de cada modo están en styles.css, en las reglas body[data-color-mode="..."]
  if (modo.id === "normal") document.body.removeAttribute("data-color-mode"); else document.body.dataset.colorMode = modo.id;
  $("#colorMode").title = `Modo actual: ${modo.nombre}. Pulsa para cambiar.`;
  $("#colorMode").setAttribute("aria-label", `Cambiar colores: ${modo.nombre}`);
  const etiqueta = $("#colorMode").querySelector("b");
  if (etiqueta) etiqueta.textContent = modo.corto;
  // La preferencia se recuerda en el navegador (si este lo permite)
  try { localStorage.setItem("sgd-modo-color", modo.id); } catch (error) { /* almacenamiento no disponible */ }
}
$("#colorMode").addEventListener("click", () => {
  const actual = document.body.dataset.colorMode || "normal";
  const siguiente = MODOS_COLOR[(MODOS_COLOR.findIndex(m => m.id === actual) + 1) % MODOS_COLOR.length];
  aplicarModoColor(siguiente.id);
});
let modoColorGuardado = "normal";
try { modoColorGuardado = localStorage.getItem("sgd-modo-color") || "normal"; } catch (error) { /* almacenamiento no disponible */ }
aplicarModoColor(modoColorGuardado);

// --- Escuchar: lee en voz alta el título y el contenido de la página visible ---
function detenerLectura() { if ("speechSynthesis" in window) speechSynthesis.cancel(); $("#voiceReader").setAttribute("aria-pressed", "false"); $("#voiceReaderLabel").textContent = "Escuchar"; }
$("#voiceReader").addEventListener("click", () => {
  if (!("speechSynthesis" in window)) return toast("La lectura por voz no está disponible en este navegador.");
  if (speechSynthesis.speaking) return detenerLectura();
  const pagina = $(".page:not(.hidden)");
  const mensaje = new SpeechSynthesisUtterance($("#pageTitle").textContent + ". " + (pagina ? pagina.innerText : ""));
  mensaje.lang = "es-PE"; mensaje.rate = .95;
  mensaje.addEventListener("end", detenerLectura);
  speechSynthesis.speak(mensaje);
  $("#voiceReader").setAttribute("aria-pressed", "true"); $("#voiceReaderLabel").textContent = "Detener";
});

// --- A− y A+: reducen o amplían el contenido (del 80 % al 150 %, de 10 en 10) ---
let escalaTexto = 100;
function cambiarEscala(pasos) {
  escalaTexto = Math.min(150, Math.max(80, escalaTexto + pasos * 10));
  $("#appView main").style.zoom = escalaTexto / 100;   // amplía todo el contenido del área principal
  $("#textSmaller").title = $("#textLarger").title = "Tamaño actual: " + escalaTexto + " %";
}
$("#textSmaller").addEventListener("click", () => cambiarEscala(-1));
$("#textLarger").addEventListener("click", () => cambiarEscala(1));

// --- Ayuda: el botón abre la página "Ayuda y glosario" (lo hace su atributo data-page="help") ---

// Muestra solo los términos del perfil actual; los títulos de sección sin términos visibles se ocultan
function filtrarGlosario() {
  const texto = $("#glossarySearch").value.toLocaleLowerCase("es").trim();
  let visibles = 0;
  $$("#glossary article").forEach(termino => {
    // data-roles indica a qué perfiles corresponde el término (vacío = todos)
    const roles = termino.dataset.roles, delPerfil = !roles || roles.split(" ").includes(state.role);
    const coincide = !texto || (termino.textContent + " " + termino.dataset.term).toLocaleLowerCase("es").includes(texto);
    termino.classList.toggle("hidden", !(delPerfil && coincide));
    if (delPerfil && coincide) visibles++;
  });
  $$("#glossary .glossary-heading").forEach(titulo => {
    let siguiente = titulo.nextElementSibling, hay = false;
    while (siguiente && siguiente.tagName === "ARTICLE") { if (!siguiente.classList.contains("hidden")) hay = true; siguiente = siguiente.nextElementSibling; }
    titulo.classList.toggle("hidden", !hay);
  });
  $("#noTerms").classList.toggle("hidden", visibles > 0);
}
$("#glossarySearch").addEventListener("input", filtrarGlosario);
$("#searchGlossary").addEventListener("click", filtrarGlosario);


/* ---------------------------------------------------------------------
   Tablero de estadísticas del administrador (página "Reportes")
   Todos los números vienen calculados del backend: GET /api/admin/estadisticas
   --------------------------------------------------------------------- */

// Color de cada estado en los gráficos (los que no estén aquí usan la paleta de reserva)
const COLOR_ESTADO = { "En proceso": "#e6a23c", "Por resolver": "#e8c34a", "Resuelto": "#279b70", "En investigación": "#397db7", "Diligencias": "#8a78c5", "Con disposición": "#25a27a", "Archivado": "#8996a5" };
const PALETA = ["#397db7", "#25a27a", "#e6a23c", "#8a78c5", "#d16c78", "#7994a8", "#e8c34a", "#5bb3c9"];
const colorDe = (nombre, i) => COLOR_ESTADO[nombre] || PALETA[i % PALETA.length];

async function cargarEstadisticasAdmin() {
  const mount = $("#adminDashboard");
  let e;
  try { e = await API.get("/admin/estadisticas"); }
  catch (error) { mount.innerHTML = `<div class="empty-report">${escapeHtml(error.message)}</div>`; return; }
  // Si mientras llegaba la respuesta se cambió de página o de usuario, no se dibuja
  if (state.role !== "admin" || state.page !== "reports") return;
  const r = e.resumen, l = e.enLinea, h = escapeHtml;

  // --- Tarjeta de indicador ---
  const tarjeta = (titulo, valor, detalle, clase = "") => `<article class="dash-kpi ${clase}"><span class="dash-kpi-title">${titulo}</span><strong>${valor}</strong><small>${detalle}</small></article>`;

  // --- Gráfico circular con leyenda ---
  const circular = (titulo, subtitulo, datos) => {
    const total = datos.reduce((s, d) => s + d.cantidad, 0) || 1; let desde = 0;
    const tramos = datos.map((d, i) => { const hasta = desde + d.cantidad / total * 100, t = `${colorDe(d.nombre, i)} ${desde}% ${hasta}%`; desde = hasta; return t; }).join(",");
    const leyenda = datos.map((d, i) => `<div class="dash-legend-row"><i style="background:${colorDe(d.nombre, i)}"></i><span>${h(d.nombre)}</span><b>${d.cantidad}</b><em>${Math.round(d.cantidad / total * 100)}%</em></div>`).join("");
    return `<section class="panel dash-card"><h3>${titulo}</h3><p class="muted">${subtitulo}</p><div class="dash-donut-layout"><div class="dash-donut" style="background:conic-gradient(${tramos || "#e3e9f0 0 100%"})"><div><strong>${datos.reduce((s, d) => s + d.cantidad, 0)}</strong><span>Total</span></div></div><div class="dash-legend">${leyenda || '<span class="muted">Sin datos</span>'}</div></div></section>`;
  };

  // --- Barras horizontales ---
  const barras = datos => { const max = Math.max(1, ...datos.map(d => d.cantidad)); return datos.map((d, i) => `<div class="dash-hbar"><span>${h(d.nombre)}</span><div><i style="width:${d.cantidad / max * 100}%;background:${PALETA[i % PALETA.length]}"></i></div><b>${d.cantidad}</b></div>`).join("") || '<p class="muted">Sin datos</p>'; };

  // --- Lista corta de conteos (actuaciones, documentos, remisiones) ---
  const lista = (titulo, datos) => `<div class="dash-mini"><h4>${titulo}</h4>${datos.map(d => `<div><span>${h(d.nombre)}</span><b>${d.cantidad}</b></div>`).join("") || '<div><span class="muted">Sin registros</span></div>'}</div>`;

  // --- Punto verde o gris según esté conectado ---
  const punto = enLinea => `<i class="dash-dot ${enLinea ? "on" : ""}" title="${enLinea ? "Usando el sistema ahora" : "Sin actividad reciente"}"></i>`;

  // --- Barras verticales de los últimos 12 meses ---
  const maxMes = Math.max(1, ...e.porMes.map(m => m.cantidad));
  const meses = e.porMes.map(m => `<div class="dash-col"><b>${m.cantidad || ""}</b><div class="dash-col-bar" style="height:${Math.max(2, m.cantidad / maxMes * 100)}%" title="${m.cantidad} denuncias"></div><small>${h(m.nombre)}</small></div>`).join("");

  const filasPolicia = e.policias.map(p => `<tr><td>${punto(p.enLinea)}<b>${h(p.codigo)}</b></td><td>${h(p.nombre)}</td><td>${p.asignadas}</td><td>${p.enProceso}</td><td>${p.porResolver}</td><td>${p.resueltas}</td><td><div class="dash-progress"><i style="width:${p.porcentajeResolucion}%"></i></div><span>${p.porcentajeResolucion}%</span></td></tr>`).join("");
  const filasFiscal = e.fiscales.map(f => `<tr><td>${h(f.nombre)}</td><td>${f.carpetas}</td><td>${f.enInvestigacion}</td><td>${f.diligencias}</td><td>${f.conDisposicion}</td><td>${f.archivadas}</td></tr>`).join("");
  const filasActividad = e.actividad.map(u => `<tr><td>${punto(u.enLinea)}<b>${h(u.codigo)}</b></td><td>${h(u.nombre)}</td><td>${h(u.perfil)}</td><td>${u.actualizaciones}</td><td>${u.operaciones}</td><td>${u.mensajes}</td><td>${u.ultimoAcceso ? fechaHora(u.ultimoAcceso) : "Nunca"}</td></tr>`).join("");
  const tabla = (encabezados, filas) => `<div class="table-scroll dash-table"><table><thead><tr>${encabezados.map(t => `<th>${t}</th>`).join("")}</tr></thead><tbody>${filas || `<tr><td colspan="${encabezados.length}">Sin registros.</td></tr>`}</tbody></table></div>`;

  mount.innerHTML = `
    <div class="dash-updated"><span><i class="dash-dot on"></i> Tablero en vivo · se actualiza cada 30 segundos</span><span>Actualizado a las ${new Date().toLocaleTimeString("es-PE")}</span></div>
    <div class="dash-kpis">
      ${tarjeta("Personas usando el sistema", l.total, `${l.policias} policía(s) · ${l.fiscales} fiscal(es) · ${l.administradores} admin.`, "online")}
      ${tarjeta("Denuncias registradas", r.totalDenuncias, `${r.registradasHoy} hoy · ${r.registradasEsteMes} este mes`)}
      ${tarjeta("Resolución policial", r.porcentajeResolucion + "%", `${r.resueltas} denuncias resueltas`)}
      ${tarjeta("Decisión fiscal", r.porcentajeDecisionFiscal + "%", `${r.conDecisionFiscal} con disposición o archivo`)}
      ${tarjeta("Tiempo promedio de atención", r.diasPromedioAtencion + " días", "Del registro a la resolución")}
      ${tarjeta("Cuentas de funcionarios", r.cuentasHabilitadas, `habilitadas · ${r.cuentasDeshabilitadas} deshabilitada(s)`)}
    </div>
    <div class="dash-grid three">
      <section class="panel dash-card"><h3>Denuncias registradas por mes</h3><p class="muted">Últimos 12 meses</p><div class="dash-cols">${meses}</div></section>
      ${circular("Estado en la PNP", "Atención policial", e.estadoPolicial)}
      ${circular("Estado en la Fiscalía", "Investigación fiscal", e.estadoFiscal)}
    </div>
    <div class="dash-grid two">
      <section class="panel dash-card"><h3>Denuncias por tipo de delito</h3><p class="muted">${e.porDelito.length} tipos registrados</p><div class="dash-hbars">${barras(e.porDelito)}</div></section>
      <section class="panel dash-card"><h3>Trabajo registrado</h3><p class="muted">Actuaciones, documentos y remisiones por estado</p><div class="dash-minis">${lista("Actuaciones policiales", e.actuacionesPoliciales)}${lista("Actuaciones fiscales", e.actuacionesFiscales)}${lista("Documentos y evidencias", e.documentos)}${lista("Remisiones a Fiscalía", e.remisiones)}</div></section>
    </div>
    <section class="panel dash-card"><h3>Desempeño de funcionarios policiales</h3><p class="muted">${e.policias.length} funcionarios · denuncias a su cargo según el estado policial</p>${tabla(["Código", "Funcionario", "Asignadas", "En proceso", "Por resolver", "Resueltas", "Resolución"], filasPolicia)}</section>
    <div class="dash-grid two">
      <section class="panel dash-card"><h3>Carga de fiscales responsables</h3><p class="muted">${e.fiscales.length} fiscales · carpetas según el estado fiscal</p>${tabla(["Fiscal", "Carpetas", "En investigación", "Diligencias", "Con disposición", "Archivadas"], filasFiscal)}</section>
      <section class="panel dash-card"><h3>Uso del sistema por funcionario</h3><p class="muted">Policías y fiscales con cuenta en el sistema</p>${tabla(["Código", "Funcionario", "Perfil", "Actualizaciones", "Operaciones", "Mensajes", "Último acceso"], filasActividad)}</section>
    </div>`;
}

// El tablero se vuelve a consultar cada 30 segundos mientras el administrador lo tiene abierto
setInterval(() => {
  if (state.user && state.role === "admin" && state.page === "reports" && !document.hidden) cargarEstadisticasAdmin();
}, 30000);
