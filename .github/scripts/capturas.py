# Capturas del módulo de copias de seguridad y de los modos de color nuevos
import asyncio, os, sys
from playwright.async_api import async_playwright
SD = "http://localhost:8080"; PC = "http://localhost:8081"
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)

async def login(page, rol, codigo):
    await page.goto(SD); await page.wait_for_selector("#loginForm")
    await page.select_option("#role", rol)
    await page.fill("#institutionCode", codigo); await page.fill("#password", "Demo1234")
    await page.click("#loginForm button[type=submit]"); await page.wait_for_timeout(3500)

async def ir(page, pagina):
    await page.evaluate("p => { const b=[...document.querySelectorAll('[data-page]')].find(x=>x.dataset.page===p); if(b) b.click(); }", pagina)
    await page.wait_for_timeout(2500)

async def modo(page, clave, valor):
    await page.evaluate(f"localStorage.setItem('{clave}','{valor}')"); await page.reload(); await page.wait_for_timeout(2500)

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(channel="chrome")
        ctx = await b.new_context(viewport={"width": 1440, "height": 900}, locale="es-PE")
        page = await ctx.new_page()
        errores = []
        page.on("pageerror", lambda e: errores.append(str(e)))
        await login(page, "admin", "ADM-0001")
        await ir(page, "reports"); await page.wait_for_timeout(2500)
        await page.locator("#avancePolicias").screenshot(path=f"{OUT}/av-01-policias.png")
        await page.locator("#avanceFiscales").screenshot(path=f"{OUT}/av-02-fiscales.png")
        await page.fill("[data-av='buscar-fiscal']", "Rojas"); await page.wait_for_timeout(500)
        await page.locator("#avanceFiscales").screenshot(path=f"{OUT}/av-05-busqueda-fiscal.png")
        await page.fill("[data-av='buscar-fiscal']", "")
        await page.select_option("[data-av='orden']", "resueltas"); await page.wait_for_timeout(500)
        await page.click("[data-av='ver-todos']"); await page.wait_for_timeout(500)
        await page.locator("#avancePolicias").screenshot(path=f"{OUT}/av-03-orden-cumplidos-todos.png")
        await page.fill("[data-av='buscar']", "Vargas"); await page.wait_for_timeout(500)
        await page.locator("#avancePolicias").screenshot(path=f"{OUT}/av-04-busqueda.png")
        await page.fill("[data-av='buscar']", "")
        await page.locator("#inactividadDashboard").screenshot(path=f"{OUT}/inact-01-tablero.png")
        await page.locator("#reportResultsPanel").screenshot(path=f"{OUT}/inact-02-consulta.png")
        await page.click("#inactividadDashboard [data-inact-filtro='Crítica']"); await page.wait_for_timeout(2500)
        await page.screenshot(path=f"{OUT}/inact-03-filtro-critica.png")
        await page.locator("#reportFilterForm").screenshot(path=f"{OUT}/inact-04-filtros.png")
        for m in ["dark", "deuteranopia"]:
            await modo(page, "sgd-modo-color", m); await ir(page, "reports"); await page.wait_for_timeout(2500)
            await page.locator("#inactividadDashboard").screenshot(path=f"{OUT}/inact-modo-{m}.png")
            await page.locator("#avancePolicias").screenshot(path=f"{OUT}/av-modo-{m}.png")
        await ctx.close()
        ctx = await b.new_context(viewport={"width": 1440, "height": 900}, locale="es-PE")
        page = await ctx.new_page()
        page.on("pageerror", lambda e: errores.append(str(e)))
        await login(page, "policia", "PNP-2045")
        await ir(page, "denuncias")
        try:
            await page.locator("button.action-icon.edit:visible, [title='Editar denuncia']:visible").first.click(timeout=5000)
            await page.wait_for_timeout(2000)
            await page.locator(".police-update-panel").first.screenshot(path=f"{OUT}/inact-05-justificacion.png")
        except Exception as e:
            print("detalle:", e)
            await page.screenshot(path=f"{OUT}/inact-05-justificacion.png")
        await ir(page, "reports"); await page.wait_for_timeout(2500)
        await page.screenshot(path=f"{OUT}/inact-06-policia-reportes.png", full_page=True)
        print("errores JS:", errores)
        await b.close()
asyncio.run(main())
