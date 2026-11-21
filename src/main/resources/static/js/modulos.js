/* =====================================================================
   modulos.js — Pantallas de detalle de los módulos:
     - Documentos y evidencias (ver, descargar y subir archivos)
     - Actuaciones y disposiciones fiscales (con ventana de detalle)
     - Seguimiento de Fiscalía (etapa de cada carpeta fiscal)
     - Informes a Fiscalía (respuesta de la Fiscalía)
     - Reportes con filtros (imprimir, exportar a Excel y a PDF)
     - Acciones de habilitar/deshabilitar cuentas del administrador
   Se carga después de app.js y usa sus funciones ($, escapeHtml, toast,
   statusTag, etiqueta, colorEstado, fechaCorta, fechaHora, shell, state...).
   ===================================================================== */

/* ---------------------------------------------------------------------
   Iconos (Bootstrap Icons)
   --------------------------------------------------------------------- */
const ICONOS = {
  imprimir: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M2.5 8a.5.5 0 1 0 0-1 .5.5 0 0 0 0 1"/><path d="M5 1a2 2 0 0 0-2 2v2H2a2 2 0 0 0-2 2v3a2 2 0 0 0 2 2h1v1a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-1h1a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-1V3a2 2 0 0 0-2-2zM4 3a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2H4zm1 5a2 2 0 0 0-2 2v1H2a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v-1a2 2 0 0 0-2-2zm7 2v3a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1"/></svg>',
  excel: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M14 4.5V14a2 2 0 0 1-2 2h-1v-1h1a1 1 0 0 0 1-1V4.5h-2A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v9H2V2a2 2 0 0 1 2-2h5.5zM2.575 15.202H.785v-1.073H2.47v-.606H.785v-1.025h1.79v-.648H0v3.999h2.575zM6.31 11.85h-.893l-.823 1.439h-.036l-.832-1.439h-.931l1.227 1.983-1.239 2.016h.861l.853-1.415h.035l.85 1.415h.908l-1.254-1.992zm1.025 3.352h1.79v.647H6.548V11.85h2.576v.648h-1.79v1.025h1.684v.606H7.334v1.073Z"/></svg>',
  pdf: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path fill-rule="evenodd" d="M14 4.5V14a2 2 0 0 1-2 2h-1v-1h1a1 1 0 0 0 1-1V4.5h-2A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v9H2V2a2 2 0 0 1 2-2h5.5zM1.6 11.85H0v3.999h.791v-1.342h.803q.43 0 .732-.173.305-.175.463-.474a1.4 1.4 0 0 0 .161-.677q0-.375-.158-.677a1.2 1.2 0 0 0-.46-.477q-.3-.18-.732-.179m.545 1.333a.8.8 0 0 1-.085.38.57.57 0 0 1-.238.241.8.8 0 0 1-.375.082H.788V12.48h.66q.327 0 .512.181.185.183.185.522m1.217-1.333v3.999h1.46q.602 0 .998-.237a1.45 1.45 0 0 0 .595-.689q.196-.45.196-1.084 0-.63-.196-1.075a1.43 1.43 0 0 0-.589-.68q-.396-.234-1.005-.234zm.791.645h.563q.371 0 .609.152a.9.9 0 0 1 .354.454q.118.302.118.753a2.3 2.3 0 0 1-.068.592 1.1 1.1 0 0 1-.196.422.8.8 0 0 1-.334.252 1.3 1.3 0 0 1-.483.082h-.563zm3.743 1.763v1.591h-.79V11.85h2.548v.653H7.896v1.117h1.606v.638z"/></svg>',
  descargar: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5"/><path d="M7.646 11.854a.5.5 0 0 0 .708 0l3-3a.5.5 0 0 0-.708-.708L8.5 10.293V1.5a.5.5 0 0 0-1 0v8.793L5.354 8.146a.5.5 0 1 0-.708.708z"/></svg>',
  subir: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5"/><path d="M7.646 1.146a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1-.708.708L8.5 2.707V11.5a.5.5 0 0 1-1 0V2.707L5.354 4.854a.5.5 0 1 1-.708-.708z"/></svg>',
  carpeta: '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.825a2 2 0 0 1-1.991-1.819l-.637-7a2 2 0 0 1 .342-1.31L.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3m-8.322.12q.322-.119.684-.12h5.396l-.707-.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981z"/></svg>'
};

/* ---------------------------------------------------------------------
   Utilidades
   --------------------------------------------------------------------- */
const h = escapeHtml; // atajo: todo dato de la base de datos se escapa antes de insertarlo

// Tamaño legible: 2048 -> "2 KB"
function tamanoLegible(bytes) {
  if (!bytes) return "—";
  if (bytes < 1024 * 1024) return Math.max(1, Math.round(bytes / 1024)) + " KB";
  return (bytes / 1024 / 1024).toFixed(1) + " MB";
}

// Botón con icono para las acciones de las tablas
const botonIcono = (icono, titulo, atributos) => `<button class="action-icon view" type="button" title="${titulo}" aria-label="${titulo}" ${atributos}>${icono}</button>`;

// Divide una lista en páginas
function paginar(lista, pagina, porPagina = 15) {
  const total = Math.max(1, Math.ceil(lista.length / porPagina));
  pagina = Math.min(Math.max(1, pagina), total);
  return { items: lista.slice((pagina - 1) * porPagina, pagina * porPagina), pagina, total, desde: lista.length ? (pagina - 1) * porPagina + 1 : 0, hasta: Math.min(pagina * porPagina, lista.length), cantidad: lista.length };
}
// Controles "‹ Página 1 de 5 ›"
function controlesPagina(p) {
  if (p.cantidad === 0) return "";
  return `<div class="pager"><span>Mostrando ${p.desde}–${p.hasta} de ${p.cantidad}</span><div><button class="outline-btn" type="button" data-pagina="${p.pagina - 1}" ${p.pagina <= 1 ? "disabled" : ""} aria-label="Página anterior">‹</button><b>Página ${p.pagina} de ${p.total}</b><button class="outline-btn" type="button" data-pagina="${p.pagina + 1}" ${p.pagina >= p.total ? "disabled" : ""} aria-label="Página siguiente">›</button></div></div>`;
}

// Tarjetas pequeñas de resumen (cantidad por categoría)
const resumenChips = items => `<div class="mod-summary">${items.map(([texto, valor, clase = ""]) => `<div class="mod-chip ${clase}"><b>${valor}</b><span>${texto}</span></div>`).join("")}</div>`;

// Tabla genérica para los módulos (las celdas ya vienen escapadas)
const tablaModulo = (encabezados, filas, vacio = "No hay registros con los filtros seleccionados.") =>
  `<div class="table-scroll"><table class="mod-table"><thead><tr>${encabezados.map(t => `<th>${t}</th>`).join("")}</tr></thead><tbody>${filas.map(f => `<tr>${f.map(c => `<td>${c}</td>`).join("")}</tr>`).join("") || `<tr><td colspan="${encabezados.length}" class="mod-empty">${vacio}</td></tr>`}</tbody></table></div>`;

// Descarga un archivo generado por el backend (PDF o Excel) mostrando los errores en un aviso
async function descargarDesde(url, nombre) {
  let respuesta;
  try { respuesta = await fetch(url, { credentials: "same-origin" }); } catch (e) { return toast("No se pudo conectar con el servidor."); }
  if (!respuesta.ok) {
    let mensaje = "No se pudo generar el archivo.";
    try { mensaje = (await respuesta.json()).mensaje || mensaje; } catch (e) { /* sin JSON */ }
    if (respuesta.status === 401) alExpirarSesion();
    return toast(mensaje);
  }
  const enlace = document.createElement("a");
  enlace.href = URL.createObjectURL(await respuesta.blob()); enlace.download = nombre;
  document.body.appendChild(enlace); enlace.click(); enlace.remove();
  setTimeout(() => URL.revokeObjectURL(enlace.href), 2000);
}

/* ---------------------------------------------------------------------
   Ventanas (modales): detalle del expediente y visor de archivos
   --------------------------------------------------------------------- */
function abrirModal(id) { $("#" + id).classList.remove("hidden"); document.body.style.overflow = "hidden"; }
function cerrarModal(id) {
  $("#" + id).classList.add("hidden");
  if (id === "fileModal") $("#fileModalBody").innerHTML = ""; // detiene videos o audios que se estén reproduciendo
  if ($$(".modal-backdrop:not(.hidden)").length === 0) document.body.style.overflow = "";
}
["expModal", "fileModal"].forEach(id => {
  $("#" + id).addEventListener("click", e => { if (e.target.id === id || e.target.closest("[data-cerrar]")) cerrarModal(id); });
});
document.addEventListener("keydown", e => {
  if (e.key !== "Escape") return;
  if (!$("#fileModal").classList.contains("hidden")) cerrarModal("fileModal"); else if (!$("#expModal").classList.contains("hidden")) cerrarModal("expModal");
});

// Documentos ya consultados, para abrirlos desde cualquier pantalla por su código
const documentosConocidos = {};
const urlArchivo = (codigo, descargar) => `/api/documentos/${encodeURIComponent(codigo)}/archivo${descargar ? "?descargar=true" : ""}`;
const insigniaTipo = d => { const t = { pdf: ["PDF", "pdf"], imagen: ["IMG", "img"], video: ["VID", "vid"], audio: ["AUD", "aud"], otro: ["DOC", "otro"], ninguno: ["—", "none"] }[d.categoria] || ["DOC", "otro"]; return `<span class="file-badge ${t[1]}">${t[0]}</span>`; };

// Visor de archivos: PDF, imágenes, video y audio se ven en la página; el resto se descarga
function verArchivo(codigo) {
  const d = documentosConocidos[codigo]; if (!d) return;
  $("#fileModalTitle").textContent = d.nombre;
  $("#fileModalSubtitle").textContent = `${d.codigo} · Denuncia ${d.codigoDenuncia}` + (d.archivoNombre ? ` · ${d.archivoNombre} (${tamanoLegible(d.archivoTamano)})` : "");
  const ver = urlArchivo(d.codigo, false), bajar = urlArchivo(d.codigo, true);
  let contenido;
  if (!d.tieneArchivo) contenido = `<div class="file-empty">Este documento está registrado en el expediente, pero no tiene un archivo adjunto.</div>`;
  else if (d.categoria === "pdf") contenido = `<iframe class="file-frame" src="${ver}" title="${h(d.nombre)}"></iframe>`;
  else if (d.categoria === "imagen") contenido = `<img class="file-image" src="${ver}" alt="${h(d.nombre)}">`;
  else if (d.categoria === "video") contenido = `<video class="file-video" controls src="${ver}"></video>`;
  else if (d.categoria === "audio") contenido = `<div class="file-empty"><audio controls src="${ver}"></audio></div>`;
  else contenido = `<div class="file-empty">Este tipo de archivo no tiene vista previa en el navegador. Descárgalo para abrirlo con su programa.</div>`;
  $("#fileModalBody").innerHTML = contenido + (d.descripcion ? `<p class="file-desc">${h(d.descripcion)}</p>` : "");
  $("#fileModalDownload").classList.toggle("hidden", !d.tieneArchivo);
  $("#fileModalDownload").href = bajar; $("#fileModalDownload").setAttribute("download", d.archivoNombre || d.nombre);
  abrirModal("fileModal");
}

// Acciones de los documentos (ver, descargar, abrir expediente) desde cualquier lugar de la página
document.addEventListener("click", e => {
  const ver = e.target.closest("[data-doc-ver]"); if (ver) { verArchivo(ver.dataset.docVer); return; }
  const exp = e.target.closest("[data-expediente]");
  if (exp) verExpediente(exp.dataset.expediente, { tipo: exp.dataset.focoTipo || "", codigo: exp.dataset.focoCodigo || "" });
});
const accionesDocumento = d => `<div class="action-group">${botonIcono(SIDEBAR_ICONS.eye, "Ver archivo", `data-doc-ver="${h(d.codigo)}"`)}${d.tieneArchivo ? `<a class="action-icon view" href="${urlArchivo(d.codigo, true)}" download="${h(d.archivoNombre || d.nombre)}" title="Descargar" aria-label="Descargar ${h(d.nombre)}">${ICONOS.descargar}</a>` : ""}</div>`;

/* ---------------------------------------------------------------------
   Expediente: ventana de detalle con lo que registró la PNP y lo que
   muestra la Fiscalía. foco = { tipo: actuacion|disposicion|remision|documento|etapas, codigo }
   --------------------------------------------------------------------- */
async function verExpediente(codigo, foco = {}) {
  $("#expModalTitle").textContent = "Expediente " + codigo;
  $("#expModalSubtitle").textContent = "Cargando información...";
  $("#expModalBody").innerHTML = '<div class="file-empty">Cargando...</div>';
  abrirModal("expModal");
  let x;
  try { x = await API.get("/expedientes/" + encodeURIComponent(codigo)); }
  catch (error) { $("#expModalBody").innerHTML = `<div class="file-empty">${h(error.message)}</div>`; return; }
  x.documentos.forEach(d => documentosConocidos[d.codigo] = d);
  $("#expModalSubtitle").textContent = `${x.tipoDelito} · Registrada el ${fechaHora(x.fechaRegistro)}`;
  $("#expModalBody").innerHTML = htmlExpediente(x, foco);
}

function htmlExpediente(x, foco) {
  const dato = (etiquetaDato, valor) => `<div><span>${etiquetaDato}</span><b>${valor || "—"}</b></div>`;
  const etiquetasEtapa = { COMPLETADO: "Completado", EN_CURSO: "En curso", PENDIENTE: "Pendiente" };

  // Recuadro destacado con el elemento desde el que se abrió la ventana
  let destacado = "";
  if (foco.tipo === "actuacion") {
    const a = x.actuaciones.find(i => i.codigo === foco.codigo);
    if (a) destacado = `<div class="exp-focus"><span class="eyebrow">ACTUACIÓN ${h(a.codigo)}</span><h3>${h(a.descripcion)}</h3><p>${h(a.perfil)} · ${fechaCorta(a.fecha)} · ${etiqueta(a.estado, colorEstado(a.estado))}</p><p class="muted">Actuación registrada en la denuncia ${h(x.codigo)}. Abajo se muestra el resto del expediente: etapas, otras actuaciones, documentos e historial.</p></div>`;
  } else if (foco.tipo === "disposicion") {
    const d = x.disposiciones.find(i => i.numero === foco.codigo);
    if (d) destacado = `<div class="exp-focus"><span class="eyebrow">DISPOSICIÓN ${h(d.numero)}</span><h3>${h(d.tipo)}</h3><p>${fechaCorta(d.fecha)} · ${h(d.fiscalResponsable)} · ${etiqueta(d.estado, colorEstado(d.estado))}</p><p class="muted">Emitida en la carpeta fiscal ${h(x.carpetaFiscal)} (denuncia ${h(x.codigo)}).</p></div>`;
  } else if (foco.tipo === "remision" && x.remision) {
    const r = x.remision;
    destacado = `<div class="exp-focus"><span class="eyebrow">INFORME REMITIDO A LA FISCALÍA</span><h3>Remitido el ${fechaCorta(r.fechaRemision)} · ${etiqueta(r.estado, colorEstado(r.estado))}</h3><p>Última respuesta de la Fiscalía: <b>${r.ultimaRespuesta ? fechaCorta(r.ultimaRespuesta) : "sin respuesta todavía"}</b></p><p class="muted">Respuesta registrada por la Fiscalía: carpeta, etapa de la investigación, disposiciones, actuaciones fiscales y documentos.</p></div>`;
  }

  const requerimiento = x.requerimiento ? `<div class="exp-alert"><b>Requerimiento fiscal:</b> la Fiscalía solicitó información o diligencias adicionales sobre esta denuncia. Revisa las disposiciones y coordina por el chat institucional.</div>` : "";

  // Línea de etapas de la investigación fiscal
  const etapas = `<section class="exp-section"><div class="exp-section-head"><h3>¿En qué etapa está la Fiscalía?</h3><span class="exp-progress-label">${x.avance}% de avance</span></div>
    <div class="exp-progress"><i style="width:${x.avance}%"></i></div>
    <ol class="exp-steps">${x.etapas.map((e, i) => `<li class="${e.estado.toLowerCase()} ${i === x.etapaActual ? "actual" : ""}"><span class="exp-step-dot">${e.estado === "COMPLETADO" ? "✓" : i + 1}</span><div><b>${h(e.titulo)}</b><small>${etiquetasEtapa[e.estado]}${e.fecha ? " · " + fechaCorta(e.fecha) : ""}</small><p>${h(e.detalle)}</p></div></li>`).join("")}</ol></section>`;

  const datos = `<div class="exp-columns">
    <section class="exp-section"><h3>Denuncia (PNP)</h3><div class="exp-grid">
      ${dato("Código", h(x.codigo))}${dato("Tipo de delito", h(x.tipoDelito))}
      ${dato("Estado PNP", statusTag(x.estadoPolicial))}${dato("Última actualización", fechaHora(x.ultimaActualizacion))}
      ${dato("Ciudadano", h(x.ciudadano.nombre))}${dato("DNI", h(x.ciudadano.dni))}
      ${dato("Lugar de los hechos", h(x.lugarHechos))}${dato("Distrito / provincia", h(x.distrito) + " · " + h(x.provincia))}
      ${dato("Funcionario responsable", x.responsablePolicial ? h(x.responsablePolicial.nombre) + " · " + h(x.responsablePolicial.codigo) : "Sin asignar")}
    </div></section>
    <section class="exp-section"><h3>Fiscalía</h3><div class="exp-grid">
      ${dato("Carpeta fiscal", h(x.carpetaFiscal || "Por asignar"))}${dato("Estado fiscal", statusTag(x.estadoFiscal))}
      ${dato("Despacho fiscal", h(x.despachoFiscal))}${dato("Fiscal responsable", h(x.fiscalResponsable))}
      ${dato("Remisión de la PNP", x.remision ? fechaCorta(x.remision.fechaRemision) + " · " + h(x.remision.estado) : "Sin remisión registrada")}
      ${dato("Última respuesta", x.remision && x.remision.ultimaRespuesta ? fechaCorta(x.remision.ultimaRespuesta) : "—")}
    </div></section></div>`;

  const disposiciones = `<section class="exp-section"><h3>Disposiciones fiscales <small>(${x.disposiciones.length})</small></h3>${tablaModulo(["N.º", "Tipo", "Fecha", "Fiscal", "Estado"],
    x.disposiciones.map(d => [`<b class="${foco.tipo === "disposicion" && foco.codigo === d.numero ? "exp-mark" : ""}">${h(d.numero)}</b>`, h(d.tipo), fechaCorta(d.fecha), h(d.fiscalResponsable), etiqueta(d.estado, colorEstado(d.estado))]),
    "La Fiscalía aún no emite disposiciones en esta carpeta.")}</section>`;

  const actuaciones = `<section class="exp-section"><h3>Actuaciones policiales y fiscales <small>(${x.actuaciones.length})</small></h3>${tablaModulo(["Código", "Institución", "Actuación", "Fecha", "Estado"],
    x.actuaciones.map(a => [`<b class="${foco.tipo === "actuacion" && foco.codigo === a.codigo ? "exp-mark" : ""}">${h(a.codigo)}</b>`, h(a.perfil), h(a.descripcion), fechaCorta(a.fecha), etiqueta(a.estado, colorEstado(a.estado))]),
    "No hay actuaciones registradas.")}</section>`;

  const documentos = `<section class="exp-section"><h3>Documentos y evidencias <small>(${x.documentos.length})</small></h3>${tablaModulo(["", "Documento", "Fecha", "Responsable", "Estado", ""],
    x.documentos.map(d => [insigniaTipo(d), `<b>${h(d.nombre)}</b><small class="cell-sub">${h(d.codigo)}${d.archivoNombre ? " · " + h(d.archivoNombre) : ""}</small>`, fechaCorta(d.fecha), h(d.responsable), etiqueta(d.estado, colorEstado(d.estado)), accionesDocumento(d)]),
    "No hay documentos registrados.")}</section>`;

  const historial = `<section class="exp-section"><h3>Historial de seguimiento</h3>${x.historial.length ? `<ol class="exp-history">${x.historial.map(s => `<li><time>${fechaHora(s.fechaHora)}</time><div>${statusTag(s.estado)} <b>${h(s.usuario)}</b> · ${h(s.perfil)}${s.observacion ? `<p>${h(s.observacion)}</p>` : ""}</div></li>`).join("")}</ol>` : '<p class="muted">Aún no se registran cambios de estado.</p>'}</section>`;

  const hechos = `<section class="exp-section"><h3>Descripción de los hechos</h3><div class="description-box">${h(x.descripcion)}</div></section>`;

  return destacado + requerimiento + etapas + datos + disposiciones + actuaciones + documentos + historial + hechos;
}

/* ---------------------------------------------------------------------
   Documentos y evidencias (policía y fiscal)
   --------------------------------------------------------------------- */
const estadoDocumentos = { lista: [], pagina: 1, texto: "", tipo: "" };

async function renderDocumentos(mount) {
  estadoDocumentos.lista = await API.get("/documentos");
  estadoDocumentos.lista.forEach(d => documentosConocidos[d.codigo] = d);
  const cuenta = c => estadoDocumentos.lista.filter(d => d.categoria === c).length;
  mount.innerHTML = shell("GESTIÓN DOCUMENTAL", "Documentos y evidencias", "Archivos asociados a las denuncias y carpetas fiscales. Puedes verlos, descargarlos o subir nuevos.",
    resumenChips([["Documentos", estadoDocumentos.lista.length], ["PDF", cuenta("pdf")], ["Imágenes", cuenta("imagen")], ["Videos y audios", cuenta("video") + cuenta("audio")], ["Otros archivos", cuenta("otro")]]) +
    `<section class="panel">
      <div class="mod-toolbar">
        <input class="mod-search" id="docSearch" type="search" placeholder="Buscar por documento, código, denuncia o ciudadano" value="${h(estadoDocumentos.texto)}" aria-label="Buscar documentos">
        <select class="mod-select" id="docType" aria-label="Tipo de archivo"><option value="">Todos los tipos</option><option value="pdf">PDF</option><option value="imagen">Imágenes</option><option value="video">Videos</option><option value="audio">Audios</option><option value="otro">Otros archivos</option><option value="ninguno">Sin archivo</option></select>
        <button class="primary-btn mod-btn" type="button" id="docUploadToggle">${ICONOS.subir}<span>Subir documento</span></button>
      </div>
      <form id="docUploadForm" class="mod-upload hidden">
        <div class="mod-upload-grid">
          <div class="form-field"><label for="docDenuncia">Código de denuncia</label><input id="docDenuncia" list="docDenunciaList" required maxlength="20" placeholder="Ej. D-000584" autocomplete="off"><datalist id="docDenunciaList">${getCases().map(c => `<option value="${h(c.id)}">${h(c.crime)} · ${h(c.citizen)}</option>`).join("")}</datalist></div>
          <div class="form-field"><label for="docNombre">Nombre del documento</label><input id="docNombre" required maxlength="150" placeholder="Ej. Registro fotográfico"></div>
          <div class="form-field mod-wide"><label for="docDescripcion">Descripción (opcional)</label><input id="docDescripcion" maxlength="500" placeholder="Breve descripción del contenido"></div>
          <div class="form-field mod-wide"><label for="docArchivo">Archivo (PDF, imagen, video, audio, Word, Excel o texto · máx. 20 MB)</label><input id="docArchivo" type="file" required accept=".pdf,.jpg,.jpeg,.png,.webp,.gif,.mp4,.webm,.mp3,.wav,.ogg,.m4a,.doc,.docx,.xls,.xlsx,.txt"></div>
        </div>
        <div class="report-filter-actions"><button class="secondary-btn" type="button" id="docUploadCancel">Cancelar</button><button class="primary-btn" type="submit">Subir documento</button></div>
      </form>
      <div id="docTable"></div>
    </section>`);
  $("#docType").value = estadoDocumentos.tipo;
  $("#docSearch").addEventListener("input", e => { estadoDocumentos.texto = e.target.value; estadoDocumentos.pagina = 1; dibujarDocumentos(); });
  $("#docType").addEventListener("change", e => { estadoDocumentos.tipo = e.target.value; estadoDocumentos.pagina = 1; dibujarDocumentos(); });
  $("#docUploadToggle").addEventListener("click", () => { $("#docUploadForm").classList.toggle("hidden"); $("#docDenuncia").focus(); });
  $("#docUploadCancel").addEventListener("click", () => { $("#docUploadForm").reset(); $("#docUploadForm").classList.add("hidden"); });
  $("#docUploadForm").addEventListener("submit", subirDocumento);
  $("#docTable").addEventListener("click", e => { const b = e.target.closest("[data-pagina]"); if (b) { estadoDocumentos.pagina = Number(b.dataset.pagina); dibujarDocumentos(); } });
  dibujarDocumentos();
}

function dibujarDocumentos() {
  const q = estadoDocumentos.texto.toLowerCase().trim();
  const lista = estadoDocumentos.lista.filter(d => (!estadoDocumentos.tipo || d.categoria === estadoDocumentos.tipo)
    && (!q || [d.codigo, d.nombre, d.descripcion, d.codigoDenuncia, d.tipoDelito, d.ciudadano, d.responsable, d.responsableNombre, d.archivoNombre].join(" ").toLowerCase().includes(q)));
  const p = paginar(lista, estadoDocumentos.pagina); estadoDocumentos.pagina = p.pagina;
  $("#docTable").innerHTML = tablaModulo(["Tipo", "Documento", "Denuncia", "Responsable", "Fecha", "Tamaño", "Estado", "Acciones"],
    p.items.map(d => [insigniaTipo(d),
      `<b>${h(d.nombre)}</b><small class="cell-sub">${h(d.codigo)}${d.archivoNombre ? " · " + h(d.archivoNombre) : " · Sin archivo"}</small>`,
      `<button class="link-btn" type="button" data-expediente="${h(d.codigoDenuncia)}" data-foco-tipo="documento" data-foco-codigo="${h(d.codigo)}">${h(d.codigoDenuncia)}</button><small class="cell-sub">${h(d.tipoDelito || "")}</small>`,
      `${h(d.responsable)}<small class="cell-sub">${h(d.responsableNombre || "")}</small>`,
      fechaCorta(d.fecha), tamanoLegible(d.archivoTamano), etiqueta(d.estado, colorEstado(d.estado)), accionesDocumento(d)])) + controlesPagina(p);
}

async function subirDocumento(e) {
  e.preventDefault();
  const archivo = $("#docArchivo").files[0];
  if (!archivo) return toast("Selecciona el archivo que quieres subir.");
  if (archivo.size > 20 * 1024 * 1024) return toast("El archivo supera el tamaño máximo permitido (20 MB).");
  const datos = new FormData();
  datos.append("denuncia", $("#docDenuncia").value.trim()); datos.append("nombre", $("#docNombre").value.trim());
  datos.append("descripcion", $("#docDescripcion").value.trim()); datos.append("archivo", archivo);
  const boton = e.submitter; if (boton) boton.disabled = true;
  try {
    const nuevo = await API.post("/documentos", datos);
    toast(`Documento ${nuevo.codigo} subido correctamente.`);
    estadoDocumentos.texto = ""; estadoDocumentos.tipo = ""; estadoDocumentos.pagina = 1;
    await renderDocumentos($("#documentsPage"));
  } catch (error) { toast(error.message); }
  finally { if (boton) boton.disabled = false; }
}

/* ---------------------------------------------------------------------
   Actuaciones (policía y fiscal) con detalle
   --------------------------------------------------------------------- */
const estadoActuaciones = { lista: [], pagina: 1, texto: "", estado: "" };

async function renderActuaciones(mount) {
  estadoActuaciones.lista = await API.get("/actuaciones");
  const policia = state.role === "policia", lista = estadoActuaciones.lista;
  const cuenta = s => lista.filter(a => a.estado === s).length;
  const estados = [...new Set(lista.map(a => a.estado))].sort();
  mount.innerHTML = shell(policia ? "ACTIVIDAD POLICIAL" : "ACTIVIDAD FISCAL", policia ? "Actuaciones policiales" : "Actuaciones",
    "Actuaciones registradas en cada expediente. Abre el detalle para ver la denuncia, la etapa fiscal, los documentos y el historial.",
    resumenChips([["Actuaciones", lista.length], ["Completadas", cuenta("Completada"), "green"], ["En proceso", cuenta("En proceso"), "amber"], ["Programadas", cuenta("Programada")], ["Pendientes", cuenta("Pendiente")]]) +
    `<section class="panel"><div class="mod-toolbar">
      <input class="mod-search" id="actSearch" type="search" placeholder="Buscar por código, denuncia o actuación" aria-label="Buscar actuaciones" value="${h(estadoActuaciones.texto)}">
      <select class="mod-select" id="actEstado" aria-label="Estado"><option value="">Todos los estados</option>${estados.map(s => `<option>${h(s)}</option>`).join("")}</select>
    </div><div id="actTable"></div></section>`);
  $("#actEstado").value = estadoActuaciones.estado;
  $("#actSearch").addEventListener("input", e => { estadoActuaciones.texto = e.target.value; estadoActuaciones.pagina = 1; dibujarActuaciones(); });
  $("#actEstado").addEventListener("change", e => { estadoActuaciones.estado = e.target.value; estadoActuaciones.pagina = 1; dibujarActuaciones(); });
  $("#actTable").addEventListener("click", e => { const b = e.target.closest("[data-pagina]"); if (b) { estadoActuaciones.pagina = Number(b.dataset.pagina); dibujarActuaciones(); } });
  dibujarActuaciones();
}

function dibujarActuaciones() {
  const q = estadoActuaciones.texto.toLowerCase().trim();
  const lista = estadoActuaciones.lista.filter(a => (!estadoActuaciones.estado || a.estado === estadoActuaciones.estado)
    && (!q || [a.codigo, a.codigoDenuncia, a.descripcion].join(" ").toLowerCase().includes(q)));
  const p = paginar(lista, estadoActuaciones.pagina); estadoActuaciones.pagina = p.pagina;
  $("#actTable").innerHTML = tablaModulo(["Código", "Denuncia", "Actuación", "Fecha", "Estado", "Detalle"],
    p.items.map(a => [`<b>${h(a.codigo)}</b>`, h(a.codigoDenuncia), h(a.descripcion), fechaCorta(a.fecha), etiqueta(a.estado, colorEstado(a.estado)),
      botonIcono(SIDEBAR_ICONS.eye, "Ver detalle", `data-expediente="${h(a.codigoDenuncia)}" data-foco-tipo="actuacion" data-foco-codigo="${h(a.codigo)}"`)])) + controlesPagina(p);
}

/* ---------------------------------------------------------------------
   Disposiciones fiscales (fiscal) con detalle
   --------------------------------------------------------------------- */
const estadoDisposiciones = { lista: [], pagina: 1, texto: "", estado: "" };

async function renderDisposiciones(mount) {
  estadoDisposiciones.lista = await API.get("/disposiciones");
  const lista = estadoDisposiciones.lista, cuenta = s => lista.filter(d => d.estado === s).length;
  mount.innerHTML = shell("GESTIÓN FISCAL", "Disposiciones fiscales", "Disposiciones emitidas en las carpetas fiscales. Abre el detalle para ver la carpeta completa.",
    resumenChips([["Disposiciones", lista.length], ["Emitidas", cuenta("Emitida"), "green"], ["Notificadas", cuenta("Notificada"), "amber"], ["Carpetas", new Set(lista.map(d => d.carpetaFiscal)).size]]) +
    `<section class="panel"><div class="mod-toolbar">
      <input class="mod-search" id="dispSearch" type="search" placeholder="Buscar por número, carpeta, denuncia, tipo o fiscal" aria-label="Buscar disposiciones" value="${h(estadoDisposiciones.texto)}">
      <select class="mod-select" id="dispEstado" aria-label="Estado"><option value="">Todos los estados</option>${[...new Set(lista.map(d => d.estado))].sort().map(s => `<option>${h(s)}</option>`).join("")}</select>
    </div><div id="dispTable"></div></section>`);
  $("#dispEstado").value = estadoDisposiciones.estado;
  $("#dispSearch").addEventListener("input", e => { estadoDisposiciones.texto = e.target.value; estadoDisposiciones.pagina = 1; dibujarDisposiciones(); });
  $("#dispEstado").addEventListener("change", e => { estadoDisposiciones.estado = e.target.value; estadoDisposiciones.pagina = 1; dibujarDisposiciones(); });
  $("#dispTable").addEventListener("click", e => {
    const b = e.target.closest("[data-pagina]"); if (b) { estadoDisposiciones.pagina = Number(b.dataset.pagina); dibujarDisposiciones(); return; }
    const sola = e.target.closest("[data-disposicion-sola]"); if (sola) verDisposicionSola(sola.dataset.disposicionSola);
  });
  dibujarDisposiciones();
}

function dibujarDisposiciones() {
  const q = estadoDisposiciones.texto.toLowerCase().trim();
  const lista = estadoDisposiciones.lista.filter(d => (!estadoDisposiciones.estado || d.estado === estadoDisposiciones.estado)
    && (!q || [d.numero, d.carpetaFiscal, d.codigoDenuncia, d.tipo, d.fiscalResponsable, d.tipoDelito, d.ciudadano].join(" ").toLowerCase().includes(q)));
  const p = paginar(lista, estadoDisposiciones.pagina); estadoDisposiciones.pagina = p.pagina;
  $("#dispTable").innerHTML = tablaModulo(["N.º disposición", "Carpeta fiscal", "Denuncia", "Tipo", "Fecha", "Fiscal responsable", "Estado", "Detalle"],
    p.items.map(d => [`<b>${h(d.numero)}</b>`, h(d.carpetaFiscal), d.codigoDenuncia ? `${h(d.codigoDenuncia)}<small class="cell-sub">${h(d.tipoDelito)}</small>` : "—",
      h(d.tipo), fechaCorta(d.fecha), h(d.fiscalResponsable), etiqueta(d.estado, colorEstado(d.estado)),
      d.codigoDenuncia ? botonIcono(SIDEBAR_ICONS.eye, "Ver detalle", `data-expediente="${h(d.codigoDenuncia)}" data-foco-tipo="disposicion" data-foco-codigo="${h(d.numero)}"`)
        : botonIcono(SIDEBAR_ICONS.eye, "Ver detalle", `data-disposicion-sola="${h(d.numero)}"`)])) + controlesPagina(p);
}

// Disposición cuya carpeta no corresponde a ninguna denuncia registrada
function verDisposicionSola(numero) {
  const d = estadoDisposiciones.lista.find(x => x.numero === numero); if (!d) return;
  $("#expModalTitle").textContent = "Disposición " + d.numero; $("#expModalSubtitle").textContent = "Carpeta fiscal " + d.carpetaFiscal;
  $("#expModalBody").innerHTML = `<div class="exp-focus"><span class="eyebrow">DISPOSICIÓN ${h(d.numero)}</span><h3>${h(d.tipo)}</h3><p>${fechaCorta(d.fecha)} · ${h(d.fiscalResponsable)} · ${etiqueta(d.estado, colorEstado(d.estado))}</p><p class="muted">La carpeta ${h(d.carpetaFiscal)} no está asociada a ninguna denuncia registrada en el sistema.</p></div>`;
  abrirModal("expModal");
}

/* ---------------------------------------------------------------------
   Seguimiento de Fiscalía (policía): etapa de cada carpeta fiscal
   --------------------------------------------------------------------- */
const estadoSeguimiento = { lista: [], pagina: 1, texto: "", etapa: "", soloRequerimientos: false };
const NOMBRES_ETAPAS = ["Remisión a la Fiscalía", "Carpeta fiscal asignada", "Investigación preliminar", "Diligencias fiscales", "Decisión fiscal"];

async function renderSeguimientoFiscal(mount) {
  estadoSeguimiento.lista = await API.get("/fiscalia/seguimiento");
  const lista = estadoSeguimiento.lista, cuenta = n => lista.filter(s => s.etapa === n).length;
  mount.innerHTML = shell("COORDINACIÓN INTERINSTITUCIONAL", "Seguimiento de Fiscalía", "Etapa en la que se encuentra la Fiscalía con cada denuncia. Abre el detalle para ver las disposiciones, actuaciones y documentos.",
    `<div class="mod-stages">${NOMBRES_ETAPAS.map((n, i) => `<button type="button" class="mod-stage ${estadoSeguimiento.etapa === n ? "active" : ""}" data-etapa="${h(n)}"><span>${i + 1}</span><b>${cuenta(n)}</b><small>${n}</small></button>`).join("")}
      <button type="button" class="mod-stage alert ${estadoSeguimiento.soloRequerimientos ? "active" : ""}" data-requerimientos="1"><span>!</span><b>${lista.filter(s => s.requerimiento).length}</b><small>Con requerimiento fiscal</small></button></div>
    <section class="panel"><div class="mod-toolbar">
      <input class="mod-search" id="segSearch" type="search" placeholder="Buscar por denuncia, carpeta, delito, ciudadano o fiscal" aria-label="Buscar" value="${h(estadoSeguimiento.texto)}">
      <select class="mod-select" id="segEtapa" aria-label="Etapa"><option value="">Todas las etapas</option>${NOMBRES_ETAPAS.map(n => `<option>${n}</option>`).join("")}</select>
    </div><div id="segTable"></div></section>`);
  $("#segEtapa").value = estadoSeguimiento.etapa;
  const redibujar = () => { estadoSeguimiento.pagina = 1; renderSeguimientoFiscalFiltros(mount); };
  $("#segSearch").addEventListener("input", e => { estadoSeguimiento.texto = e.target.value; estadoSeguimiento.pagina = 1; dibujarSeguimiento(); });
  $("#segEtapa").addEventListener("change", e => { estadoSeguimiento.etapa = e.target.value; redibujar(); });
  mount.querySelectorAll("[data-etapa]").forEach(b => b.addEventListener("click", () => { estadoSeguimiento.etapa = estadoSeguimiento.etapa === b.dataset.etapa ? "" : b.dataset.etapa; redibujar(); }));
  mount.querySelector("[data-requerimientos]").addEventListener("click", () => { estadoSeguimiento.soloRequerimientos = !estadoSeguimiento.soloRequerimientos; redibujar(); });
  $("#segTable").addEventListener("click", e => { const b = e.target.closest("[data-pagina]"); if (b) { estadoSeguimiento.pagina = Number(b.dataset.pagina); dibujarSeguimiento(); } });
  dibujarSeguimiento();
}
// Actualiza el resaltado de las tarjetas de etapa y la tabla sin volver a pedir los datos
function renderSeguimientoFiscalFiltros(mount) {
  mount.querySelectorAll("[data-etapa]").forEach(b => b.classList.toggle("active", b.dataset.etapa === estadoSeguimiento.etapa));
  mount.querySelector("[data-requerimientos]").classList.toggle("active", estadoSeguimiento.soloRequerimientos);
  $("#segEtapa").value = estadoSeguimiento.etapa;
  dibujarSeguimiento();
}

function dibujarSeguimiento() {
  const q = estadoSeguimiento.texto.toLowerCase().trim();
  const lista = estadoSeguimiento.lista.filter(s => (!estadoSeguimiento.etapa || s.etapa === estadoSeguimiento.etapa)
    && (!estadoSeguimiento.soloRequerimientos || s.requerimiento)
    && (!q || [s.codigo, s.carpetaFiscal, s.tipoDelito, s.ciudadano, s.fiscalResponsable, s.despachoFiscal].join(" ").toLowerCase().includes(q)));
  const p = paginar(lista, estadoSeguimiento.pagina); estadoSeguimiento.pagina = p.pagina;
  $("#segTable").innerHTML = tablaModulo(["Denuncia", "Carpeta fiscal", "Fiscal responsable", "Etapa actual", "Estado fiscal", "Remisión", "Detalle"],
    p.items.map(s => [`<b>${h(s.codigo)}</b><small class="cell-sub">${h(s.tipoDelito)}</small>`,
      `${h(s.carpetaFiscal || "Por asignar")}<small class="cell-sub">${h(s.despachoFiscal || "")}</small>`, h(s.fiscalResponsable || "—"),
      `<div class="mod-stage-cell"><b>${s.etapaNumero}/${s.totalEtapas} · ${h(s.etapa)}</b><div class="dash-progress"><i style="width:${s.avance}%"></i></div>${s.requerimiento ? '<span class="status amber">Requerimiento</span>' : ""}</div>`,
      statusTag(s.estadoFiscal), s.remisionEstado ? `${etiqueta(s.remisionEstado, colorEstado(s.remisionEstado))}<small class="cell-sub">${fechaCorta(s.fechaRemision)}</small>` : '<small class="cell-sub">Sin remisión</small>',
      botonIcono(SIDEBAR_ICONS.eye, "Ver en qué etapa está", `data-expediente="${h(s.codigo)}" data-foco-tipo="etapas"`)])) + controlesPagina(p);
}

/* ---------------------------------------------------------------------
   Informes a Fiscalía (policía): informes remitidos y respuesta de la Fiscalía.
   Se muestra en la página de reportes del policía, debajo de los análisis operativos.
   --------------------------------------------------------------------- */
async function renderInformesFiscalia() {
  const mount = $("#informesFiscalia");
  mount.classList.toggle("hidden", state.role !== "policia");
  if (state.role !== "policia") return;
  let lista;
  try { lista = await API.get("/remisiones"); } catch (error) { mount.innerHTML = `<div class="empty-report">${h(error.message)}</div>`; return; }
  const cuenta = s => lista.filter(r => r.estado === s).length;
  mount.innerHTML = `<div class="report-section-title">Informes remitidos y respuesta de la Fiscalía</div>` +
    resumenChips([["Informes remitidos", lista.length], ["Recibidos", cuenta("Recibido"), "green"], ["Requerimientos", cuenta("Requerimiento"), "amber"], ["En espera", cuenta("En espera") + cuenta("Pendiente")], ["Con disposición fiscal", lista.filter(r => r.disposiciones > 0).length]]) +
    `<section class="panel">${tablaModulo(["Denuncia", "Remitido", "Estado del informe", "Carpeta fiscal", "Fiscal responsable", "Etapa fiscal", "Respuesta"],
      lista.map(r => [`<b>${h(r.codigoDenuncia)}</b><small class="cell-sub">${h(r.tipoDelito)} · ${h(r.ciudadano)}</small>`, fechaCorta(r.fechaRemision),
        `${etiqueta(r.estado, colorEstado(r.estado))}<small class="cell-sub">${r.ultimaRespuesta ? "Respondió el " + fechaCorta(r.ultimaRespuesta) : "Sin respuesta"}</small>`,
        `${h(r.carpetaFiscal || "Por asignar")}<small class="cell-sub">${h(r.despachoFiscal || "")}</small>`, h(r.fiscalResponsable || "—"),
        `${h(r.etapa)}<small class="cell-sub">${h(r.estadoFiscal)} · ${r.disposiciones} disposición(es)</small>`,
        `<button class="outline-btn" type="button" data-expediente="${h(r.codigoDenuncia)}" data-foco-tipo="remision">Ver respuesta</button>`]),
      "Todavía no se registran informes remitidos a la Fiscalía.")}</section>`;
}

/* ---------------------------------------------------------------------
   Reportes con filtros (tres perfiles)
   --------------------------------------------------------------------- */
const estadoReporte = { opciones: null, filas: [], pagina: 1, consulta: "" };

// Prepara el formulario (una sola vez) y muestra todas las denuncias
async function iniciarReportes() {
  if (!estadoReporte.opciones) {
    try { estadoReporte.opciones = await API.get("/reportes/opciones"); } catch (error) { return toast(error.message); }
    const o = estadoReporte.opciones, opciones = (lista, todos) => `<option value="">${todos}</option>` + lista.map(v => `<option value="${h(v)}">${h(v)}</option>`).join("");
    $("#repDelito").innerHTML = opciones(o.delitos, "Todos los delitos");
    $("#repEstadoPnp").innerHTML = opciones(o.estadosPoliciales, "Todos los estados");
    $("#repEstadoFiscal").innerHTML = opciones(o.estadosFiscales, "Todos los estados");
    $("#repProvincia").innerHTML = opciones(o.provincias.map(p => p.nombre), "Todas las provincias");
    llenarDistritos();
    $("#repProvincia").addEventListener("change", llenarDistritos);
    $("#reportFilterForm").addEventListener("submit", e => { e.preventDefault(); estadoReporte.pagina = 1; consultarReporte(); });
    $("#reportFilterForm").addEventListener("reset", () => setTimeout(() => { llenarDistritos(); estadoReporte.pagina = 1; consultarReporte(); }, 0));
    $("#printReport").addEventListener("click", imprimirReporte);
    $("#exportReportExcel").addEventListener("click", () => descargarDesde("/api/reportes/denuncias/excel?" + estadoReporte.consulta, `reporte_denuncias_${new Date().toISOString().slice(0, 10)}.xlsx`));
    $("#exportReportPdf").addEventListener("click", () => descargarDesde("/api/reportes/denuncias/pdf?" + estadoReporte.consulta, `reporte_denuncias_${new Date().toISOString().slice(0, 10)}.pdf`));
    $("#reportResultRows").addEventListener("click", e => {
      const b = e.target.closest("[data-pagina]"); if (b) { estadoReporte.pagina = Number(b.dataset.pagina); dibujarReporte(); return; }
      const v = e.target.closest("[data-rep-ver]"); if (v) state.role === "admin" ? openCasePreview(v.dataset.repVer) : verExpediente(v.dataset.repVer);
    });
  }
  consultarReporte();
}

// Distritos de la provincia elegida
function llenarDistritos() {
  const p = (estadoReporte.opciones?.provincias || []).find(x => x.nombre === $("#repProvincia").value);
  $("#repDistrito").innerHTML = `<option value="">${p ? "Todos los distritos" : "Elige una provincia"}</option>` + (p ? p.distritos.map(d => `<option value="${h(d)}">${h(d)}</option>`).join("") : "");
  $("#repDistrito").disabled = !p;
}

// Filtros del formulario en formato de URL: delito=Robo&provincia=Lima...
function filtrosReporte() {
  const valores = { delito: $("#repDelito").value, estadoPolicial: $("#repEstadoPnp").value, estadoFiscal: $("#repEstadoFiscal").value, provincia: $("#repProvincia").value, distrito: $("#repDistrito").value, desde: $("#repDesde").value, hasta: $("#repHasta").value, texto: $("#repTexto").value.trim(), situacion: $("#repSituacion").value };
  const params = new URLSearchParams();
  Object.entries(valores).forEach(([k, v]) => { if (v) params.set(k, v); });
  return params.toString();
}

async function consultarReporte() {
  const desde = $("#repDesde").value, hasta = $("#repHasta").value;
  if (desde && hasta && desde > hasta) return toast("La fecha inicial no puede ser posterior a la fecha final.");
  const consulta = filtrosReporte();
  try { estadoReporte.filas = await API.get("/reportes/denuncias" + (consulta ? "?" + consulta : "")); }
  catch (error) { return toast(error.message); }
  estadoReporte.consulta = consulta;
  $("#reportResultsPanel").classList.remove("hidden");
  dibujarReporte();
}

// Texto con los filtros usados ("Delito: Robo · Provincia: Lima")
function descripcionFiltros() {
  const partes = [["Delito", "#repDelito"], ["Estado PNP", "#repEstadoPnp"], ["Estado Fiscalía", "#repEstadoFiscal"], ["Provincia", "#repProvincia"], ["Distrito", "#repDistrito"], ["Desde", "#repDesde"], ["Hasta", "#repHasta"], ["Texto", "#repTexto"], ["Inactividad", "#repSituacion"]]
    .filter(([, id]) => $(id).value.trim()).map(([n, id]) => `${n}: ${id === "#repDesde" || id === "#repHasta" ? fechaCorta($(id).value) : id === "#repSituacion" ? $(id).selectedOptions[0].textContent : $(id).value.trim()}`);
  return partes.length ? partes.join(" · ") : "Todas las denuncias (sin filtros)";
}

function dibujarReporte() {
  const filas = estadoReporte.filas, p = paginar(filas, estadoReporte.pagina, 20); estadoReporte.pagina = p.pagina;
  $("#reportResultSubtitle").textContent = descripcionFiltros();
  $("#reportResultCount").textContent = `${filas.length} denuncia(s) encontrada(s).`;
  ["#printReport", "#exportReportExcel", "#exportReportPdf"].forEach(id => $(id).disabled = filas.length === 0);
  $("#reportResultRows").innerHTML = tablaModulo(["Código", "Fecha", "Última actividad", "Días sin actividad", "Situación", "Tipo de delito", "Ciudadano", "Ubicación", "Estado PNP", "Estado Fiscalía", "Carpeta fiscal", ""],
    p.items.map(r => [`<b>${h(r.codigo)}</b>`, fechaCorta(r.fechaRegistro),
      `${fechaCorta(r.ultimaActividad)}<small class="cell-sub">${h(r.tipoActividad)}</small>`, `<span class="inact-dias">${r.diasSinActividad}</span>`, situacionTag(r.situacion),
      h(r.tipoDelito), h(r.ciudadano), `${h(r.distrito)}<small class="cell-sub">${h(r.provincia)}</small>`, statusTag(r.estadoPolicial), statusTag(r.estadoFiscal), h(r.carpetaFiscal || "—"),
      botonIcono(SIDEBAR_ICONS.eye, "Ver denuncia", `data-rep-ver="${h(r.codigo)}"`)]),
    "No se encontraron denuncias con los filtros seleccionados.") + controlesPagina(p) + leyendaSituaciones();
}

// Etiqueta de la situación de inactividad (el color acompaña al texto, nunca lo reemplaza)
const CLASE_SITUACION = { "Normal": "normal", "Seguimiento": "seguimiento", "Crítica": "critica", "Prioritaria": "prioritaria", "Concluida": "concluida" };
const situacionTag = s => `<span class="sit-tag ${CLASE_SITUACION[s] || "concluida"}">${h(s)}</span>`;

// Leyenda de las situaciones bajo la tabla de resultados
const leyendaSituaciones = () => `<div class="sit-legend" aria-label="Significado de la situación de inactividad">
  <span>${situacionTag("Normal")} 0 a 7 días · actividad reciente</span><span>${situacionTag("Seguimiento")} 8 a 14 días · próxima alerta</span>
  <span>${situacionTag("Crítica")} 15 a 19 días · requiere revisión</span><span>${situacionTag("Prioritaria")} 20 días o más · inactividad prolongada</span>
  <span>${situacionTag("Concluida")} resuelta o archivada · sin alerta</span></div>
  <p class="sit-note">Días sin una actuación válida: cambio de estado justificado, actuación policial o fiscal, documento o evidencia, informe a la Fiscalía o disposición fiscal. Los cambios administrativos y los mensajes del chat no reinician el contador.</p>`;

// Imprimir: abre una hoja con todas las filas del resultado y el diálogo de impresión
function imprimirReporte() {
  const filas = estadoReporte.filas; if (!filas.length) return toast("No hay resultados para imprimir.");
  const ventana = window.open("", "_blank"); if (!ventana) return toast("Permite las ventanas emergentes para imprimir.");
  const cuerpo = filas.map(r => `<tr><td>${h(r.codigo)}</td><td>${fechaCorta(r.fechaRegistro)}</td><td>${fechaCorta(r.ultimaActividad)}</td><td>${r.diasSinActividad}</td><td>${h(r.situacion)}</td><td>${h(r.tipoDelito)}</td><td>${h(r.ciudadano)}</td><td>${h(r.distrito)}</td><td>${h(r.provincia)}</td><td>${h(r.estadoPolicial)}</td><td>${h(r.estadoFiscal)}</td><td>${h(r.carpetaFiscal || "—")}</td></tr>`).join("");
  ventana.document.write(`<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Reporte de denuncias</title><style>body{font-family:Arial,sans-serif;color:#1d2938;padding:24px}h1{font-size:18px;color:#173b63;margin:0 0 4px}.sub{color:#65758a;font-size:11px}.meta{font-size:11px;line-height:1.7;margin:14px 0}table{width:100%;border-collapse:collapse;font-size:9.5px}th{background:#eaf1f8;text-align:left}th,td{border:1px solid #cbd5e1;padding:5px 6px}tr:nth-child(even) td{background:#f7f9fb}.foot{margin-top:18px;font-size:9px;color:#718096}@page{size:landscape;margin:12mm}</style></head><body><h1>REPORTE INSTITUCIONAL DE DENUNCIAS</h1><div class="sub">Sistema de Gestión y Seguimiento de Denuncias · PNP – Fiscalía</div><div class="meta"><b>Funcionario:</b> ${h(state.name)} (${h(state.code)})<br><b>Filtros:</b> ${h(descripcionFiltros())}<br><b>Fecha de emisión:</b> ${new Date().toLocaleString("es-PE")}<br><b>Total:</b> ${filas.length} denuncia(s)</div><table><thead><tr><th>Código</th><th>Fecha</th><th>Última actividad</th><th>Días sin actividad</th><th>Situación</th><th>Tipo de delito</th><th>Ciudadano</th><th>Distrito</th><th>Provincia</th><th>Estado PNP</th><th>Estado Fiscalía</th><th>Carpeta fiscal</th></tr></thead><tbody>${cuerpo}</tbody></table><div class="foot">Prototipo académico con datos ficticios.</div></body></html>`);
  ventana.document.close(); ventana.onload = () => ventana.print();
}

/* ---------------------------------------------------------------------
   Administración: habilitar o deshabilitar cuentas desde
   "Funcionarios" y "Usuarios y perfiles"
   --------------------------------------------------------------------- */
function accionesUsuario(u) {
  if (u.codigo === state.code) return '<span class="muted">Tu cuenta</span>'; // nadie puede deshabilitar su propia cuenta
  return `<button class="outline-btn user-toggle ${u.habilitado ? "" : "enable"}" type="button" data-user-toggle="${h(u.codigo)}">${u.habilitado ? "Deshabilitar" : "Habilitar"}</button>`;
}

// Funcionarios: misma lista que "Usuarios y perfiles" con la columna de acciones
function renderFuncionarios(mount) {
  const filas = usuarios.filter(u => u.rol !== "admin").map(u => [`<b>${h(u.codigo)}</b>`, h(u.nombreCompleto), h(u.institucion), h(u.perfil), h(u.dependencia || "—"), etiqueta(u.habilitado ? "Activo" : "Deshabilitado", u.habilitado ? "green" : "gray"), accionesUsuario(u)]);
  mount.innerHTML = shell("ADMINISTRACIÓN", "Funcionarios", "Funcionarios policiales y fiscales registrados. Puedes habilitar o deshabilitar cada cuenta.",
    `<section class="panel">${tablaModulo(["Código", "Funcionario", "Institución", "Perfil", "Dependencia", "Estado", "Acciones"], filas)}</section>`);
}

// Un solo manejador para los botones de las dos pantallas
document.addEventListener("click", async e => {
  const boton = e.target.closest("#functionsPage [data-user-toggle]"); if (!boton) return;
  try {
    const actualizado = await API.patch(`/usuarios/${encodeURIComponent(boton.dataset.userToggle)}/estado`);
    usuarios = usuarios.map(u => u.codigo === actualizado.codigo ? actualizado : u);
    renderUsers(); renderFuncionarios($("#functionsPage"));
    toast(`Cuenta ${actualizado.habilitado ? "habilitada" : "deshabilitada"}: ${actualizado.codigo}`);
  } catch (error) { toast(error.message); }
});
