/* ---------------------------------------------------------------------
   Tour guiado de la interfaz (onboarding).
   - La pantalla real queda visible; el resto se oscurece ligeramente.
   - El elemento que se explica queda resaltado.
   - Una tarjeta muestra las instrucciones con "Anterior", "Siguiente" y "Salir".
   - Teclado: → o Enter avanza, ← retrocede, Esc sale.
   Uso:  Tour.iniciar(pasos, { alTerminar(completo) {} })
         Tour.confirmar("¿Pregunta?") -> Promise<boolean>
   Cada paso: { el, titulo, texto, antes }
     el     selector, elemento, lista de selectores o función (sin "el" la tarjeta va al centro)
     antes  función opcional que se ejecuta antes de mostrar el paso
   Si el elemento de un paso no existe o no se ve, ese paso se omite.
   Este archivo es igual en el portal ciudadano y en el sistema de gestión.
   --------------------------------------------------------------------- */
const Tour = (() => {
  let pasos = [], actual = 0, opciones = {}, capa, foco, tarjeta, activo = false, elActual = null, focoPrevio = null;

  function crear() {
    if (capa) return;
    capa = document.createElement("div");
    capa.className = "tour-capa";
    capa.addEventListener("click", e => e.stopPropagation()); // la página queda en pausa durante el tour
    foco = document.createElement("div");
    foco.className = "tour-foco";
    tarjeta = document.createElement("div");
    tarjeta.className = "tour-tarjeta";
    tarjeta.setAttribute("role", "dialog");
    tarjeta.setAttribute("aria-modal", "true");
    tarjeta.setAttribute("aria-labelledby", "tourTitulo");
    tarjeta.setAttribute("aria-describedby", "tourTexto");
    tarjeta.innerHTML = `
      <div class="tour-cabecera"><span class="tour-paso" id="tourPaso"></span>
        <button type="button" class="tour-cerrar" data-tour="salir" aria-label="Salir del tutorial" title="Salir del tutorial">✕</button></div>
      <h2 class="tour-titulo" id="tourTitulo"></h2>
      <p class="tour-texto" id="tourTexto"></p>
      <div class="tour-progreso" aria-hidden="true"><i id="tourBarra"></i></div>
      <div class="tour-acciones">
        <button type="button" class="tour-btn tour-saltar" data-tour="salir">Salir del tutorial</button>
        <span class="tour-espacio"></span>
        <button type="button" class="tour-btn tour-sec" data-tour="anterior">← Anterior</button>
        <button type="button" class="tour-btn tour-pri" data-tour="siguiente">Siguiente →</button>
      </div>`;
    tarjeta.addEventListener("click", e => {
      e.stopPropagation();
      const accion = e.target.closest("[data-tour]")?.dataset.tour;
      if (accion === "siguiente") ir(1);
      if (accion === "anterior") ir(-1);
      if (accion === "salir") terminar(false);
    });
    document.body.append(capa, foco, tarjeta);
  }

  function visible(el) {
    if (!el || !el.isConnected) return false;
    const r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0 && getComputedStyle(el).visibility !== "hidden";
  }

  // Devuelve la lista de elementos visibles del paso (puede resaltar varios a la vez)
  function elementos(paso) {
    let el = typeof paso.el === "function" ? paso.el() : paso.el;
    if (!el) return [];
    const lista = Array.isArray(el) ? el : [el];
    return lista.flatMap(x => typeof x === "string" ? [...document.querySelectorAll(x)] : [x]).filter(visible);
  }

  function rectangulo(lista) {
    const rs = lista.map(e => e.getBoundingClientRect());
    const top = Math.min(...rs.map(r => r.top)), left = Math.min(...rs.map(r => r.left));
    const bottom = Math.max(...rs.map(r => r.bottom)), right = Math.max(...rs.map(r => r.right));
    return { top, left, bottom, right, width: right - left, height: bottom - top };
  }

  function iniciar(lista, opc = {}) {
    if (activo) terminar(false, true);
    crear();
    pasos = lista; opciones = opc; activo = true; actual = -1;
    focoPrevio = document.activeElement;
    document.body.classList.add("tour-activo");
    document.addEventListener("keydown", teclas, true);
    window.addEventListener("resize", ubicar);
    window.addEventListener("scroll", ubicar, true);
    ir(1);
  }

  // Avanza o retrocede, saltando los pasos cuyo elemento no está en pantalla
  function ir(dir) {
    let n = actual + dir;
    while (n >= 0 && n < pasos.length) {
      const paso = pasos[n];
      try { if (paso.antes) paso.antes(); } catch (_) { /* el paso sigue aunque falle su preparación */ }
      if (!paso.el || elementos(paso).length) break;
      n += dir;
    }
    if (n >= pasos.length) return terminar(true);
    if (n < 0) return;
    actual = n;
    mostrar();
  }

  function mostrar() {
    const paso = pasos[actual], lista = elementos(paso);
    elActual = lista.length ? lista : null;
    const total = pasos.length;
    tarjeta.querySelector("#tourPaso").textContent = `Paso ${actual + 1} de ${total}`;
    tarjeta.querySelector("#tourTitulo").textContent = paso.titulo || "";
    tarjeta.querySelector("#tourTexto").innerHTML = paso.texto || "";
    tarjeta.querySelector("#tourBarra").style.width = `${Math.round((actual + 1) / total * 100)}%`;
    tarjeta.querySelector('[data-tour="anterior"]').disabled = actual === 0;
    const ultimo = !pasos.slice(actual + 1).some(p => !p.el || elementos(p).length);
    tarjeta.querySelector('[data-tour="siguiente"]').textContent = ultimo ? "Finalizar ✓" : "Siguiente →";
    tarjeta.classList.remove("visible");
    if (elActual) {
      elActual[0].scrollIntoView({ block: "center", inline: "nearest" });
    }
    // Se espera a que termine el desplazamiento antes de ubicar el resaltado y la tarjeta
    setTimeout(() => {
      ubicar();
      tarjeta.classList.add("visible");
      tarjeta.querySelector('[data-tour="siguiente"]').focus({ preventScroll: true });
    }, elActual ? 180 : 0);
  }

  function ubicar() {
    if (!activo) return;
    const vw = window.innerWidth, vh = window.innerHeight, m = 12;
    if (!elActual || !elActual.every(e => e.isConnected)) {
      foco.classList.add("centro");
      foco.style.cssText = "";
      tarjeta.classList.add("centro");
      tarjeta.style.left = tarjeta.style.top = "";
      return;
    }
    foco.classList.remove("centro"); tarjeta.classList.remove("centro");
    const r = rectangulo(elActual), pad = 6;
    const f = {
      top: Math.max(r.top - pad, 4), left: Math.max(r.left - pad, 4),
      width: Math.min(r.width + pad * 2, vw - 8), height: Math.min(r.height + pad * 2, vh - 8)
    };
    Object.assign(foco.style, { top: f.top + "px", left: f.left + "px", width: f.width + "px", height: f.height + "px" });

    // La tarjeta va donde haya espacio: debajo, arriba, a la derecha o a la izquierda
    const tw = tarjeta.offsetWidth, th = tarjeta.offsetHeight;
    let top, left;
    if (f.top + f.height + m + th <= vh) { top = f.top + f.height + m; left = f.left; }
    else if (f.top - m - th >= 0) { top = f.top - m - th; left = f.left; }
    else if (f.left + f.width + m + tw <= vw) { left = f.left + f.width + m; top = f.top; }
    else if (f.left - m - tw >= 0) { left = f.left - m - tw; top = f.top; }
    else { top = vh - th - m; left = (vw - tw) / 2; }
    left = Math.min(Math.max(left, m), vw - tw - m);
    top = Math.min(Math.max(top, m), vh - th - m);
    tarjeta.style.left = left + "px"; tarjeta.style.top = top + "px";
  }

  function teclas(e) {
    if (!activo) return;
    if (e.key === "Escape") { e.preventDefault(); terminar(false); }
    else if (e.key === "ArrowRight") { e.preventDefault(); ir(1); }
    else if (e.key === "ArrowLeft") { e.preventDefault(); ir(-1); }
    else if (e.key === "Tab") { // el foco se queda dentro de la tarjeta
      const b = [...tarjeta.querySelectorAll("button:not([disabled])")];
      const i = b.indexOf(document.activeElement);
      e.preventDefault();
      b[(i + (e.shiftKey ? -1 : 1) + b.length) % b.length].focus();
    }
  }

  function terminar(completo, silencioso) {
    if (!activo) return;
    activo = false;
    document.body.classList.remove("tour-activo");
    document.removeEventListener("keydown", teclas, true);
    window.removeEventListener("resize", ubicar);
    window.removeEventListener("scroll", ubicar, true);
    [capa, foco, tarjeta].forEach(x => x.remove());
    capa = foco = tarjeta = null;
    if (focoPrevio && focoPrevio.focus) focoPrevio.focus({ preventScroll: true });
    if (!silencioso && opciones.alTerminar) opciones.alTerminar(completo);
  }

  // Ventana de confirmación propia (Sí / No)
  function confirmar(pregunta, detalle = "") {
    return new Promise(resolver => {
      const fondo = document.createElement("div");
      fondo.className = "tour-confirmar";
      fondo.innerHTML = `<div class="tour-tarjeta visible centro" role="alertdialog" aria-modal="true" aria-labelledby="tourPregunta">
        <h2 class="tour-titulo" id="tourPregunta">${pregunta}</h2>${detalle ? `<p class="tour-texto">${detalle}</p>` : ""}
        <div class="tour-acciones"><span class="tour-espacio"></span>
          <button type="button" class="tour-btn tour-sec" data-r="no">No</button>
          <button type="button" class="tour-btn tour-pri" data-r="si">Sí, repetir</button></div></div>`;
      const cerrar = valor => { document.removeEventListener("keydown", tecla, true); fondo.remove(); resolver(valor); };
      const tecla = e => { if (e.key === "Escape") { e.preventDefault(); cerrar(false); } };
      fondo.addEventListener("click", e => {
        const r = e.target.closest("[data-r]")?.dataset.r;
        if (r) cerrar(r === "si"); else if (e.target === fondo) cerrar(false);
      });
      document.addEventListener("keydown", tecla, true);
      document.body.append(fondo);
      fondo.querySelector('[data-r="si"]').focus();
    });
  }

  // Recordar si ya se vio (si el navegador no permite guardar, se recuerda solo en esta visita)
  const memoria = {};
  function visto(clave) { try { return localStorage.getItem(clave) === "1"; } catch (_) { return memoria[clave] === true; } }
  function marcarVisto(clave) { memoria[clave] = true; try { localStorage.setItem(clave, "1"); } catch (_) { /* sin almacenamiento */ } }

  return { iniciar, confirmar, visto, marcarVisto, get activo() { return activo; } };
})();
