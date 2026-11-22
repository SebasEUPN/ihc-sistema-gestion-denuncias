/* ---------------------------------------------------------------------
   Gráficos de avance de casos por funcionario (solo administrador).
   Usa los mismos datos del tablero: GET /api/admin/estadisticas
   (campos "policias" y "fiscales"); no agrega datos nuevos.
   --------------------------------------------------------------------- */

// Tramos del gráfico de policías: estado policial de las denuncias a su cargo
const TRAMOS_POLICIA = [
  { campo: "resueltas", nombre: "Resueltas", detalle: "casos cumplidos", color: "var(--av-resuelto)" },
  { campo: "enProceso", nombre: "En proceso", detalle: "casos que está tratando", color: "var(--av-proceso)" },
  { campo: "porResolver", nombre: "Por resolver", detalle: "casos pendientes", color: "var(--av-pendiente)" },
  { campo: "otros", nombre: "Otros estados", detalle: "archivadas u otro estado heredado", color: "var(--av-otros)" }
];

// Tramos del gráfico de fiscales: estado fiscal de las carpetas a su cargo
const TRAMOS_FISCAL = [
  { campo: "conDisposicion", nombre: "Con disposición", detalle: "decisión emitida", color: "var(--av-resuelto)" },
  { campo: "archivadas", nombre: "Archivadas", detalle: "carpeta concluida", color: "var(--av-otros)" },
  { campo: "diligencias", nombre: "Diligencias", detalle: "en trámite", color: "var(--av-proceso)" },
  { campo: "enInvestigacion", nombre: "En investigación", detalle: "pendiente de decisión", color: "var(--av-pendiente)" }
];

// Lo que elige el administrador se conserva aunque el tablero se refresque cada 30 segundos
const vistaAvance = { orden: "asignadas", buscar: "", verTodos: false, ordenFiscal: "carpetas" };
let datosAvance = null;

const pct = (n, total) => total ? Math.round(n / total * 1000) / 10 : 0;

function barraApilada(fila, tramos, total) {
  if (!total) return '<div class="av-bar empty"><span>Sin casos asignados</span></div>';
  return `<div class="av-bar">${tramos.map(t => {
    const n = fila[t.campo] || 0, p = pct(n, total);
    if (!n) return "";
    return `<i style="width:${p}%;background:${t.color}" title="${t.nombre}: ${n} de ${total} (${p}%)">${p >= 9 ? Math.round(p) + "%" : ""}</i>`;
  }).join("")}</div>`;
}

function leyendaAvance(tramos, totales, total) {
  return `<div class="av-legend">${tramos.map(t => `<div class="av-legend-item"><i style="background:${t.color}"></i><div><b>${t.nombre}</b><small>${t.detalle}</small></div><strong>${pct(totales[t.campo], total)}%</strong><em>${totales[t.campo]}</em></div>`).join("")}</div>`;
}

function sumar(filas, tramos) {
  const tot = {};
  tramos.forEach(t => { tot[t.campo] = filas.reduce((s, f) => s + (f[t.campo] || 0), 0); });
  return tot;
}

function htmlAvancePolicias(e) {
  const h = escapeHtml, todos = e.policias;
  const total = todos.reduce((s, p) => s + p.asignadas, 0);
  const totales = sumar(todos, TRAMOS_POLICIA);
  const conCasos = todos.filter(p => p.asignadas > 0).length;

  const q = vistaAvance.buscar.trim().toLowerCase();
  let filas = todos.filter(p => !q || p.codigo.toLowerCase().includes(q) || p.nombre.toLowerCase().includes(q));
  const ordenes = {
    asignadas: (a, b) => b.asignadas - a.asignadas,
    resueltas: (a, b) => pct(b.resueltas, b.asignadas) - pct(a.resueltas, a.asignadas) || b.asignadas - a.asignadas,
    enProceso: (a, b) => pct(b.enProceso, b.asignadas) - pct(a.enProceso, a.asignadas) || b.asignadas - a.asignadas,
    porResolver: (a, b) => pct(b.porResolver, b.asignadas) - pct(a.porResolver, a.asignadas) || b.asignadas - a.asignadas,
    nombre: (a, b) => a.nombre.localeCompare(b.nombre, "es")
  };
  filas = [...filas].sort(ordenes[vistaAvance.orden] || ordenes.asignadas);
  const limite = 12, visibles = vistaAvance.verTodos || q ? filas : filas.slice(0, limite);

  const filasHtml = visibles.map(p => `
    <div class="av-row">
      <div class="av-who"><b>${h(p.codigo)}</b><span>${h(p.nombre)}</span></div>
      ${barraApilada(p, TRAMOS_POLICIA, p.asignadas)}
      <div class="av-num"><b>${p.asignadas}</b><small>casos</small></div>
      <div class="av-num ok"><b>${pct(p.resueltas, p.asignadas)}%</b><small>cumplidos</small></div>
    </div>`).join("") || '<p class="muted">Ningún funcionario coincide con la búsqueda.</p>';

  const opcion = (v, t) => `<option value="${v}" ${vistaAvance.orden === v ? "selected" : ""}>${t}</option>`;
  const boton = !q && filas.length > limite
    ? `<button type="button" class="outline-btn av-more" data-av="ver-todos">${vistaAvance.verTodos ? "Mostrar solo los primeros " + limite : "Ver los " + filas.length + " funcionarios"}</button>` : "";

  return `
    <section class="panel dash-card av-card" id="avancePolicias">
      <div class="av-head">
        <div><h3>Avance de casos por funcionario policial</h3>
          <p class="muted">Porcentaje de las denuncias a cargo de cada policía según su estado policial · ${conCasos} de ${todos.length} policías con casos · ${total} denuncias</p></div>
        <div class="av-tools">
          <label>Buscar<input type="search" data-av="buscar" placeholder="Código o nombre" value="${h(vistaAvance.buscar)}"></label>
          <label>Ordenar por<select data-av="orden">${opcion("asignadas", "Más casos asignados")}${opcion("resueltas", "Mayor % cumplido")}${opcion("enProceso", "Mayor % en proceso")}${opcion("porResolver", "Mayor % pendiente")}${opcion("nombre", "Nombre (A-Z)")}</select></label>
        </div>
      </div>
      ${leyendaAvance(TRAMOS_POLICIA, totales, total)}
      <div class="av-rows">${filasHtml}</div>
      ${boton}
    </section>`;
}

function htmlAvanceFiscales(e) {
  const h = escapeHtml, todos = e.fiscales;
  const total = todos.reduce((s, f) => s + f.carpetas, 0);
  const totales = sumar(todos, TRAMOS_FISCAL);
  const decididas = f => (f.conDisposicion || 0) + (f.archivadas || 0);
  const filas = [...todos].sort(vistaAvance.ordenFiscal === "decision"
    ? (a, b) => pct(decididas(b), b.carpetas) - pct(decididas(a), a.carpetas)
    : (a, b) => b.carpetas - a.carpetas);
  const opcion = (v, t) => `<option value="${v}" ${vistaAvance.ordenFiscal === v ? "selected" : ""}>${t}</option>`;
  const filasHtml = filas.map(f => `
    <div class="av-row">
      <div class="av-who"><b>${h(f.nombre)}</b></div>
      ${barraApilada(f, TRAMOS_FISCAL, f.carpetas)}
      <div class="av-num"><b>${f.carpetas}</b><small>carpetas</small></div>
      <div class="av-num ok"><b>${pct(decididas(f), f.carpetas)}%</b><small>con decisión</small></div>
    </div>`).join("") || '<p class="muted">Sin carpetas fiscales.</p>';
  return `
    <section class="panel dash-card av-card" id="avanceFiscales">
      <div class="av-head">
        <div><h3>Avance de carpetas por fiscal responsable</h3>
          <p class="muted">Porcentaje de carpetas de cada fiscal según su estado fiscal · ${total} carpetas</p></div>
        <div class="av-tools"><label>Ordenar por<select data-av="orden-fiscal">${opcion("carpetas", "Más carpetas")}${opcion("decision", "Mayor % con decisión")}</select></label></div>
      </div>
      ${leyendaAvance(TRAMOS_FISCAL, totales, total)}
      <div class="av-rows">${filasHtml}</div>
    </section>`;
}

/** Devuelve el HTML de los dos gráficos y guarda los datos para redibujar al ordenar o buscar. */
function htmlAvance(e) {
  datosAvance = e;
  return `<div id="avanceMount">${htmlAvancePolicias(e)}${htmlAvanceFiscales(e)}</div>`;
}

function redibujarPolicias() {
  const sec = document.getElementById("avancePolicias");
  if (sec && datosAvance) sec.outerHTML = htmlAvancePolicias(datosAvance);
}

document.addEventListener("input", ev => {
  const el = ev.target.closest && ev.target.closest('[data-av="buscar"]');
  if (!el) return;
  vistaAvance.buscar = el.value;
  const pos = el.selectionStart;
  redibujarPolicias();
  const nuevo = document.querySelector('[data-av="buscar"]');
  if (nuevo) { nuevo.focus(); try { nuevo.setSelectionRange(pos, pos); } catch (_) { } }
});

document.addEventListener("change", ev => {
  const el = ev.target;
  if (el.matches && el.matches('[data-av="orden"]')) { vistaAvance.orden = el.value; redibujarPolicias(); }
  if (el.matches && el.matches('[data-av="orden-fiscal"]') && datosAvance) {
    vistaAvance.ordenFiscal = el.value;
    document.getElementById("avanceFiscales").outerHTML = htmlAvanceFiscales(datosAvance);
  }
});

document.addEventListener("click", ev => {
  const b = ev.target.closest && ev.target.closest('[data-av="ver-todos"]');
  if (!b) return;
  vistaAvance.verTodos = !vistaAvance.verTodos;
  redibujarPolicias();
});
