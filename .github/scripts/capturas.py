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
        ctx = await b.new_context(viewport={"width": 1280, "height": 900}, locale="es-PE")
        page = await ctx.new_page()
        errores = []
        page.on("pageerror", lambda e: errores.append(str(e)))
        await login(page, "admin", "ADM-0001")
        await ir(page, "backup")
        await page.screenshot(path=f"{OUT}/copias-01.png", full_page=True)
        try:
            await page.click("#bkHistorial"); await page.wait_for_timeout(800)
            await page.screenshot(path=f"{OUT}/copias-02-historial.png")
            await page.click("[data-bk-cerrar]")
            await page.select_option("#bkCopia", index=1)
            await page.click("#bkRestaurar button[type=submit]"); await page.wait_for_timeout(800)
            await page.screenshot(path=f"{OUT}/copias-03-restaurar.png")
            await page.click("#bkModal .secondary-btn")
            await page.fill("#bkNombre", "Backup_Desde_Pantalla"); await page.fill("#bkDescripcion", "Creada desde el formulario")
            await page.click("#bkCrear button[type=submit]"); await page.wait_for_timeout(4000)
            await page.screenshot(path=f"{OUT}/copias-04-creada.png", full_page=True)
        except Exception as e:
            print("copias:", e)
        for m in ["dark", "contrast", "protanopia"]:
            await modo(page, "sgd-modo-color", m); await ir(page, "backup")
            await page.screenshot(path=f"{OUT}/copias-modo-{m}.png", full_page=True)
        for m in ["colorblind", "protanopia", "deuteranopia", "tritanopia", "mono"]:
            await modo(page, "sgd-modo-color", m)
            await page.screenshot(path=f"{OUT}/sd-admin-{m}.png")
        await ctx.close()
        ctx = await b.new_context(viewport={"width": 1280, "height": 900}, locale="es-PE")
        page = await ctx.new_page()
        await login(page, "policia", "PNP-2045")
        for m in ["protanopia", "deuteranopia", "tritanopia", "mono"]:
            await modo(page, "sgd-modo-color", m); await ir(page, "denuncias")
            await page.screenshot(path=f"{OUT}/sd-policia-{m}.png")
        await ctx.close()
        ctx = await b.new_context(viewport={"width": 1280, "height": 900}, locale="es-PE")
        page = await ctx.new_page()
        await page.goto(PC); await page.wait_for_timeout(1500)
        await page.fill("#caseCode", "D-000584"); await page.keyboard.press("Enter"); await page.wait_for_timeout(1500)
        await page.fill("#citizenPin", "123456"); await page.keyboard.press("Enter"); await page.wait_for_timeout(2500)
        try:
            await page.locator(".case-option:visible").first.click(timeout=4000); await page.wait_for_timeout(2000)
        except Exception as e:
            print("portal:", e)
        for m in ["normal", "protanopia", "deuteranopia", "tritanopia", "mono"]:
            await modo(page, "pnp-color-mode", m)
            await page.screenshot(path=f"{OUT}/pc-{m}.png")
        # el botón debe recorrer los 8 modos y volver al inicio
        await modo(page, "pnp-color-mode", "normal")
        vistos = []
        for i in range(9):
            await page.click("#colorMode"); await page.wait_for_timeout(150)
            vistos.append(await page.evaluate("(document.body.dataset.colorMode||'normal')+':'+document.querySelector('#colorMode span').textContent"))
        print("ciclo portal:", vistos)
        print("errores JS:", errores)
        await b.close()
asyncio.run(main())
