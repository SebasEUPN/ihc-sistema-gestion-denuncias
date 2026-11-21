/* =====================================================================
   Control de denuncias sin actividad (administrador, en "Reportes")
   Días desde la última actuación válida de cada denuncia y su situación:
     Normal 0–7 · Seguimiento 8–14 · Crítica 15–19 · Prioritaria 20 o más.
   Los datos vienen de GET /api/reportes/inactividad (ReporteService).
   ===================================================================== */

const SITUACIONES_ALERTA = [
  { nombre: "Normal", clase: "normal", dias: "0 a 7 días", lectura: "Actividad reciente" },
  { nombre: "Seguimiento", clase: "seguimiento", dias: "8 a 14 días", lectura: "Próxima alerta" },
  { nombre: "Crítica", clase: "critica", dias: "15 a 19 días", lectura: "Requiere revisión" },
  { nombre: "Prioritaria", clase: "prioritaria", dias: "20 días o más", lectura: "Inactividad prolongada" }
];

async function cargarInactividad() {
  const mount = $("#inactividadDashboard");
  let d;
  try { d = await API.get("/reportes/inactividad"); }
  catch (error) { mount.innerHTML = `<div class="empty-report">${escapeHtml(error.message)}</div>`; return; }
  if (state.role !== "admin" || state.page !== "reports") return;
  const e = escapeHtml, cantidad = n => (d.categorias.find(c => c.nombre === n) || { cantidad: 0 }).cantidad;
  const pct = n => d.activas ? Math.round(n / d.activas * 100) : 0;
  const concluidas = cantidad("Concluida");

  // Tarjetas por situación (como la referencia): al pulsarlas se filtra la consulta de abajo
  const tarjetas = SITUACIONES_ALERTA.map(s => { const n = cantidad(s.nombre); return `
    <button class="inact-card ${s.clase}" type="button" data-inact-filtro="${s.nombre}" aria-label="Ver las ${n} denuncias en situación ${s.nombre} (${s.dias})">
      <span class="inact-card-top"><span class="sit-tag ${s.clase}">${s.nombre}</span><b>${s.dias}</b><small>${s.lectura}</small></span>
      <span class="inact-card-num"><strong>${n}</strong><em>${pct(n)}% de las activas</em></span>
      <span class="inact-meter"><i style="width:${pct(n)}%"></i></span>
    </button>`; }).join("");

  // Barra apilada con la distribución de las denuncias activas
  const tramos = SITUACIONES_ALERTA.map(s => { const n = cantidad(s.nombre); return n ? `<i class="${s.clase}" style="width:${n / d.activas * 100}%" title="${s.nombre}: ${n} (${pct(n)}%)"></i>` : ""; }).join("");

  // Por tipo de delito: barras apiladas (solo los 10 con más denuncias por vencer)
  const delitos = d.porDelito.slice(0, 10), maxDelito = Math.max(1, ...delitos.map(x => x.normal + x.seguimiento + x.critica + x.prioritaria));
  const filaDelito = x => { const total = x.normal + x.seguimiento + x.critica + x.prioritaria, w = n => n / maxDelito * 100;
    return `<div class="inact-stack-row"><span title="${e(x.delito)}">${e(x.delito)}</span><div class="inact-stack">${["normal", "seguimiento", "critica", "prioritaria"].map(k => x[k] ? `<i class="${k}" style="width:${w(x[k])}%" title="${k}: ${x[k]}"></i>` : "").join("")}</div><b>${x.critica + x.prioritaria}<small> / ${total}</small></b></div>`; };

  const responsables = d.porResponsable.map(r => `<tr><td><b>${e(r.codigo)}</b><small class="cell-sub">${e(r.nombre)}</small></td><td>${r.critica}</td><td>${r.prioritaria}</td><td><b>${r.critica + r.prioritaria}</b></td></tr>`).join("");
  const inactivas = d.masInactivas.map(f => `<tr><td><b>${e(f.codigo)}</b></td><td>${e(f.tipoDelito)}</td><td>${e(f.ciudadano)}</td><td>${e(f.responsable)}</td><td>${fechaCorta(f.ultimaActividad)}<small class="cell-sub">${e(f.tipoActividad)}</small></td><td class="inact-dias">${f.dias}</td><td>${situacionTag(f.situacion)}</td><td>${botonIcono(SIDEBAR_ICONS.eye, "Ver denuncia", `data-inact-ver="${e(f.codigo)}"`)}</td></tr>`).join("");
  const tabla = (encabezados, filas, vacio) => `<div class="table-scroll dash-table"><table><thead><tr>${encabezados.map(t => `<th>${t}</th>`).join("")}</tr></thead><tbody>${filas || `<tr><td colspan="${encabezados.length}">${vacio}</td></tr>`}</tbody></table></div>`;

  mount.innerHTML = `
    <div class="report-section-title">Control de inactividad</div>
    <section class="panel inact-panel">
      <div class="inact-head">
        <div><h2>Denuncias sin actividad</h2><p class="muted">Días desde la última actuación válida de cada denuncia activa · al ${new Date().toLocaleDateString("es-PE")}</p></div>
        <div class="inact-kpis">
          <div><strong>${d.activas}</strong><span>Denuncias activas</span></div>
          <div class="alerta"><strong>${d.porVencer}</strong><span>Por vencer (15 días o más)</span></div>
          <div><strong>${d.promedioDias}</strong><span>Días sin actividad en promedio</span></div>
        </div>
      </div>
      <div class="inact-cards">${tarjetas}</div>
      <div class="inact-dist" role="img" aria-label="Distribución de las ${d.activas} denuncias activas: ${SITUACIONES_ALERTA.map(s => `${s.nombre} ${cantidad(s.nombre)}`).join(", ")}">${tramos}</div>
      <p class="inact-foot">${concluidas} denuncia(s) resuelta(s) o archivada(s) no generan alerta. Pulsa una tarjeta para ver esas denuncias en la consulta de abajo.</p>
    </section>
    <div class="dash-grid two">
      <section class="panel dash-card"><h3>Situación por tipo de delito</h3><p class="muted">Denuncias activas · el número es cuántas llevan 15 días o más sin actividad</p>
        <div class="inact-stack-legend">${SITUACIONES_ALERTA.map(s => `<span><i class="${s.clase}"></i>${s.nombre}</span>`).join("")}</div>
        <div class="inact-stacks">${delitos.map(filaDelito).join("") || '<p class="muted">Sin denuncias activas.</p>'}</div></section>
      <section class="panel dash-card"><h3>Responsables con más denuncias por vencer</h3><p class="muted">Policías a cargo de denuncias con 15 días o más sin actividad</p>
        ${tabla(["Funcionario", "Crítica", "Prioritaria", "Total"], responsables, "Ninguna denuncia por vencer.")}</section>
    </div>
    <section class="panel dash-card">
      <div class="inact-list-head"><div><h3>Las 10 denuncias con más días sin actividad</h3><p class="muted">Requieren atención primero</p></div>
        <button class="outline-btn" type="button" data-inact-filtro="POR_VENCER">Ver todas las denuncias por vencer (${d.porVencer})</button></div>
      ${tabla(["Código", "Tipo de delito", "Ciudadano", "Responsable", "Última actividad", "Días", "Situación", ""], inactivas, "No hay denuncias activas.")}
    </section>`;
}

// Tarjetas y botones del tablero: filtran la consulta de denuncias o abren el detalle
document.addEventListener("click", e => {
  const filtro = e.target.closest("#inactividadDashboard [data-inact-filtro]");
  if (filtro) {
    $("#repSituacion").value = filtro.dataset.inactFiltro;
    estadoReporte.pagina = 1;
    consultarReporte().then(() => $("#reportResultsPanel").scrollIntoView({ behavior: "smooth", block: "start" }));
    return;
  }
  const ver = e.target.closest("#inactividadDashboard [data-inact-ver]");
  if (ver) openCasePreview(ver.dataset.inactVer);
});
