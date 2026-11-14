/* =====================================================================
   api.js — Comunicación entre el frontend y el backend Spring Boot.
   Todas las peticiones van a rutas que empiezan con /api y viajan con la
   cookie de sesión (JSESSIONID), por eso el servidor sabe quién las envía.
   ===================================================================== */

const API = {

  /**
   * Envía una petición al backend y devuelve la respuesta ya convertida de JSON.
   * metodo: "GET", "POST", "PUT" o "PATCH"
   * ruta:   ruta sin el prefijo /api (ej.: "/denuncias")
   * cuerpo: objeto que se envía como JSON, o un FormData cuando se suben archivos
   */
  async solicitar(metodo, ruta, cuerpo) {
    const opciones = { method: metodo, credentials: "same-origin", headers: {} };

    if (cuerpo instanceof FormData) {
      // Subida de archivos: el navegador arma solo el encabezado multipart
      opciones.body = cuerpo;
    } else if (cuerpo !== undefined) {
      // Datos normales: se envían en formato JSON
      opciones.headers["Content-Type"] = "application/json";
      opciones.body = JSON.stringify(cuerpo);
    }

    let respuesta;
    try {
      respuesta = await fetch("/api" + ruta, opciones);
    } catch (error) {
      // El servidor está apagado o no hay conexión
      throw new Error("No se pudo conectar con el servidor.");
    }

    // 204 = respuesta correcta pero sin contenido
    if (respuesta.status === 204) return null;

    // Se intenta leer el JSON (los errores llegan como { "mensaje": "..." })
    let datos = null;
    try { datos = await respuesta.json(); } catch (error) { /* respuesta sin JSON */ }

    if (!respuesta.ok) {
      // 401 fuera de las rutas de autenticación = la sesión expiró en el servidor
      if (respuesta.status === 401 && !ruta.startsWith("/auth/") && typeof alExpirarSesion === "function") {
        alExpirarSesion();
      }
      throw new Error((datos && datos.mensaje) || "No se pudo completar la operación.");
    }
    return datos;
  },

  // Atajos para cada tipo de petición
  get(ruta) { return this.solicitar("GET", ruta); },
  post(ruta, cuerpo) { return this.solicitar("POST", ruta, cuerpo === undefined ? {} : cuerpo); },
  put(ruta, cuerpo) { return this.solicitar("PUT", ruta, cuerpo); },
  patch(ruta, cuerpo) { return this.solicitar("PATCH", ruta, cuerpo === undefined ? {} : cuerpo); }
};

/* ---------------------------------------------------------------------
   Adaptadores: convierten los datos del backend (en español) a los nombres
   que ya usaban las pantallas del prototipo, para no reescribir las vistas.
   --------------------------------------------------------------------- */

/** Denuncia del backend -> objeto "caso" que usan las tablas y el detalle. */
function aCaso(d) {
  return {
    id: d.codigo,
    citizen: d.ciudadano,
    crime: d.tipoDelito,
    status: d.estado,
    date: d.fecha,                         // dd/MM/yyyy
    description: d.descripcion,
    tipoDenuncia: d.tipoDelito,
    despachoFiscal: d.despachoFiscal,
    fiscalResponsable: d.fiscalResponsable,
    carpetaFiscal: d.carpetaFiscal,
    ciudadanoCompleto: d.ciudadanoCompleto,
    dniCiudadano: d.dniCiudadano,
    correoCiudadano: d.correoCiudadano,
    telefonoCiudadano: d.telefonoCiudadano,
    fechaRegistro: d.fechaRegistro,        // fecha y hora en formato ISO
    ultimaActualizacion: d.ultimaActualizacion,
    lugarHechos: d.lugarHechos,
    historial: d.historial || []
  };
}

/** Conversación del backend -> objeto "chat" que usa la pantalla de mensajes. */
function aChat(c) {
  return {
    id: c.id,
    citizen: c.nombre,
    phone: c.telefono || "",
    caseId: c.referencia,
    online: c.enLinea,
    unread: c.noLeidos,
    messages: c.mensajes.map(m => ({
      mine: m.mio,
      text: m.texto || "",
      time: m.hora,
      attachment: m.adjunto ? {
        name: m.adjunto.nombre,
        size: m.adjunto.tamano,
        url: m.adjunto.url,
        // Según el tipo de archivo se decide cómo mostrarlo
        kind: (m.adjunto.tipo.startsWith("image/") && !m.adjunto.tipo.includes("svg")) ? "image"
            : m.adjunto.tipo.startsWith("video/") ? "video"
            : m.adjunto.tipo.startsWith("audio/") ? "audio" : "file"
      } : null
    }))
  };
}
