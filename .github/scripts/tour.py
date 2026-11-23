# Prueba del tutorial guiado en los dos prototipos (Playwright).
# Escribe capturas en <carpeta> y el resultado de cada comprobación en <carpeta>/tour.txt
import asyncio, os, sys
from playwright.async_api import async_playwright
SD = "http://localhost:8080"; PC = "http://localhost:8081"
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
lineas, fallas = [], [0]

def ok(nombre, cond, detalle=""):
    lineas.append(f"  [{'OK' if cond else 'FALLA'}] {nombre}{(' = ' + str(detalle)) if detalle != '' else ''}")
    if not cond: fallas[0] += 1

async def tarjeta(page):
    if not await page.locator(".tour-tarjeta").count(): return None
    return (await page.locator("#tourPaso").inner_text()) + " · " + (await page.locator("#tourTitulo").inner_text())

async def foto(page, nombre):
    await page.wait_for_timeout(450); await page.screenshot(path=f"{OUT}/{nombre}.png")

async def recorrer(page, prefijo, fotos=()):
    """Avanza con Siguiente hasta el final; guarda captura de los pasos indicados. Devuelve los títulos."""
    titulos, n = [], 0
    while await page.locator(".tour-tarjeta").count() and n < 40:
        n += 1
        t = await tarjeta(page); titulos.append(t)
        if n in fotos: await foto(page, f"{prefijo}-paso{n:02d}")
        await page.click('[data-tour="siguiente"]'); await page.wait_for_timeout(350)
    return titulos

async def login(page, rol, codigo):
    await page.goto(SD); await page.wait_for_selector("#loginForm")
    await page.select_option("#role", rol)
    await page.fill("#institutionCode", codigo); await page.fill("#password", "Demo1234")
    await page.click("#loginForm button[type=submit]"); await page.wait_for_timeout(3500)

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(channel="chrome")
        errores = []

        # ---------------- Sistema de gestión ----------------
        lineas.append("== Sistema de gestión")
        for rol, codigo, fotos in [("admin", "ADM-0001", (1, 2, 3, 7, 15, 17)), ("policia", "PNP-2045", (1, 4, 13)), ("fiscal", "FN-1023", (1, 4))]:
            ctx = await b.new_context(viewport={"width": 1440, "height": 900}, locale="es-PE")
            page = await ctx.new_page(); page.on("pageerror", lambda e: errores.append(str(e)))
            await login(page, rol, codigo)
            await page.wait_for_timeout(800)
            t = await tarjeta(page)
            ok(f"{codigo}: el tutorial se abre solo al primer ingreso", t is not None, t)
            ok(f"{codigo}: la pantalla de fondo sigue visible", await page.locator("#dashboardPage").is_visible())
            ok(f"{codigo}: Anterior deshabilitado en el primer paso", await page.locator('[data-tour="anterior"]').is_disabled())
            if rol == "admin":
                await page.click('[data-tour="siguiente"]'); await page.wait_for_timeout(400)
                t2 = await tarjeta(page)
                await page.click('[data-tour="anterior"]'); await page.wait_for_timeout(400)
                ok("retroceder vuelve al paso anterior", await tarjeta(page) == t, f"{t2} -> {await tarjeta(page)}")
                caja = await page.locator(".tour-foco").bounding_box()
                ok("el primer paso va centrado (sin resaltado)", caja is not None)
            titulos = await recorrer(page, f"sis-{rol}", fotos)
            lineas.append(f"  {codigo}: {len(titulos)} pasos -> " + " | ".join(x.split(' · ')[1] for x in titulos))
            ok(f"{codigo}: Finalizar cierra el tutorial", not await page.locator(".tour-tarjeta").count())
            ok(f"{codigo}: queda marcado como visto", await page.evaluate(f"localStorage.getItem('sgd-tutorial-visto-{codigo}')") == "1")
            # Vuelve a entrar: ya no se abre solo
            await page.click("#logoutBtn"); await page.wait_for_timeout(400)
            await page.click("#confirmLogout"); await page.wait_for_timeout(1500)
            await login(page, rol, codigo); await page.wait_for_timeout(1200)
            ok(f"{codigo}: no se abre solo en el segundo ingreso", not await page.locator(".tour-tarjeta").count())
            if rol == "admin":
                # Botón Tutorial: "No" cancela, "Sí" repite
                await page.click("#tutorialBtn"); await page.wait_for_timeout(400)
                await foto(page, "sis-admin-confirmar")
                ok("botón Tutorial pregunta si se quiere repetir", "repetir el tutorial" in (await page.locator(".tour-confirmar").inner_text()))
                await page.click('.tour-confirmar [data-r="no"]'); await page.wait_for_timeout(400)
                ok("responder No cancela", not await page.locator(".tour-confirmar, .tour-tarjeta").count())
                await page.click('[data-page="reports"]'); await page.wait_for_timeout(1500)
                await page.click("#tutorialBtn"); await page.wait_for_timeout(400)
                await page.click('.tour-confirmar [data-r="si"]'); await page.wait_for_timeout(900)
                ok("responder Sí repite el tutorial (desde el inicio)", (await tarjeta(page) or "").startswith("Paso 1"), await tarjeta(page))
                await page.keyboard.press("ArrowRight"); await page.wait_for_timeout(400)
                ok("flecha derecha avanza", (await tarjeta(page) or "").startswith("Paso 2"), await tarjeta(page))
                await page.keyboard.press("Escape"); await page.wait_for_timeout(400)
                ok("Esc sale del tutorial", not await page.locator(".tour-tarjeta").count())
                ok("tras salir, la página responde a los clics", await page.evaluate("!document.querySelector('.tour-capa')"))
                for m in ["dark", "contrast"]:
                    await page.evaluate(f"localStorage.setItem('sgd-modo-color','{m}')"); await page.reload(); await page.wait_for_timeout(2500)
                    await page.click("#tutorialBtn"); await page.wait_for_timeout(300)
                    await page.click('.tour-confirmar [data-r="si"]'); await page.wait_for_timeout(600)
                    for _ in range(2): await page.click('[data-tour="siguiente"]'); await page.wait_for_timeout(350)
                    await foto(page, f"sis-admin-modo-{m}")
                    await page.click('[data-tour="salir"] >> nth=0'); await page.wait_for_timeout(300)
                await page.evaluate("localStorage.setItem('sgd-modo-color','normal')")
            await ctx.close()

        # Pantalla pequeña (celular)
        ctx = await b.new_context(viewport={"width": 390, "height": 844}, locale="es-PE", is_mobile=True)
        page = await ctx.new_page(); page.on("pageerror", lambda e: errores.append(str(e)))
        await login(page, "policia", "PNP-2045"); await page.wait_for_timeout(800)
        titulos = await recorrer(page, "sis-movil", (1, 2, 8))
        ok("sistema en celular: el recorrido llega al final", len(titulos) > 5, len(titulos))
        await ctx.close()

        # ---------------- Portal ciudadano ----------------
        lineas.append("== Portal ciudadano")
        ctx = await b.new_context(viewport={"width": 1440, "height": 900}, locale="es-PE")
        page = await ctx.new_page(); page.on("pageerror", lambda e: errores.append(str(e)))
        await page.goto(PC); await page.wait_for_timeout(1500)
        t = await tarjeta(page)
        ok("portal: el tutorial se abre solo en la primera visita", t is not None, t)
        titulos = await recorrer(page, "por-inicio", (1, 2, 3, 4, 7, 11, 12))
        lineas.append(f"  inicio: {len(titulos)} pasos -> " + " | ".join(x.split(' · ')[1] for x in titulos))
        await page.reload(); await page.wait_for_timeout(1500)
        ok("portal: no se repite al volver a entrar", not await page.locator(".tour-tarjeta").count())
        await page.fill("#caseCode", "D-000584"); await page.click("#lookupForm button[type=submit]"); await page.wait_for_timeout(1800)
        ok("portal: recorrido de la verificación con PIN", (await tarjeta(page) or "").endswith("Verificación de identidad"), await tarjeta(page))
        titulos = await recorrer(page, "por-pin", (2,))
        lineas.append(f"  verificación: {len(titulos)} pasos")
        await page.fill("#citizenPin", "123456"); await page.click("#pinForm button[type=submit]"); await page.wait_for_timeout(2500)
        ok("portal: recorrido del historial de denuncias", (await tarjeta(page) or "").endswith("Mis denuncias registradas"), await tarjeta(page))
        titulos = await recorrer(page, "por-historial", (2,))
        lineas.append(f"  historial: {len(titulos)} pasos")
        await foto(page, "por-tras-pin")
        lineas.append("  vista tras el PIN: " + await page.evaluate("document.querySelector('main .view:not(.hidden)')?.id"))
        await page.click(".case-option >> nth=0", timeout=8000); await page.wait_for_timeout(2500)
        ok("portal: recorrido del detalle de la denuncia", (await tarjeta(page) or "") != "", await tarjeta(page))
        titulos = await recorrer(page, "por-detalle", (2, 3, 5, 7))
        lineas.append(f"  detalle: {len(titulos)} pasos -> " + " | ".join(x.split(' · ')[1] for x in titulos))
        await page.click("#tutorialBtn"); await page.wait_for_timeout(400)
        await foto(page, "por-confirmar")
        await page.click('.tour-confirmar [data-r="no"]'); await page.wait_for_timeout(300)
        ok("portal: No cancela", not await page.locator(".tour-tarjeta").count())
        await page.click("#tutorialBtn"); await page.wait_for_timeout(300)
        await page.click('.tour-confirmar [data-r="si"]'); await page.wait_for_timeout(800)
        ok("portal: Sí repite el recorrido de la pantalla actual", (await tarjeta(page) or "").endswith("Volver al historial"), await tarjeta(page))
        await page.click('[data-tour="salir"] >> nth=1'); await page.wait_for_timeout(300)
        ok("portal: Salir del tutorial lo cierra", not await page.locator(".tour-tarjeta").count())
        await page.evaluate("localStorage.setItem('pnp-color-mode','dark')"); await page.reload(); await page.wait_for_timeout(1500)
        await page.click("#tutorialBtn"); await page.wait_for_timeout(300); await page.click('.tour-confirmar [data-r="si"]'); await page.wait_for_timeout(700)
        await page.click('[data-tour="siguiente"]'); await page.wait_for_timeout(300); await page.click('[data-tour="siguiente"]')
        await foto(page, "por-modo-dark")
        await ctx.close()

        ctx = await b.new_context(viewport={"width": 390, "height": 844}, locale="es-PE", is_mobile=True)
        page = await ctx.new_page(); page.on("pageerror", lambda e: errores.append(str(e)))
        await page.goto(PC); await page.wait_for_timeout(1500)
        titulos = await recorrer(page, "por-movil", (1, 3, 7))
        ok("portal en celular: el recorrido llega al final", len(titulos) > 5, len(titulos))
        await ctx.close()

        ok("sin errores de JavaScript", not errores, "; ".join(errores[:3]))
        await b.close()

try:
    asyncio.run(main())
except Exception as e:
    lineas.append(f"  [FALLA] la prueba se interrumpió: {str(e).splitlines()[0]}"); fallas[0] += 1
lineas.append(f"== RESULTADO TOUR: {fallas[0]} fallas")
open(f"{OUT}/tour.txt", "w").write("\n".join(lineas) + "\n")
print("\n".join(lineas))
