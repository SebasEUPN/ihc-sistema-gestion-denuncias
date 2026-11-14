import asyncio, os
from playwright.async_api import async_playwright
BASE = "http://localhost:8080"
OUT = "capturas"
os.makedirs(OUT, exist_ok=True)

async def login(page, rol, codigo):
    await page.goto(BASE)
    await page.wait_for_selector("#loginForm")
    await page.select_option("#role", rol)
    await page.fill("#institutionCode", codigo)
    await page.fill("#password", "Demo1234")
    await page.click("#loginForm button[type=submit]")
    await page.wait_for_timeout(3500)

async def ir(page, pagina, nombre):
    await page.evaluate("p => { const b=[...document.querySelectorAll('[data-page]')].find(x=>x.dataset.page===p); if(b) b.click(); }", pagina)
    await page.wait_for_timeout(2500)
    await page.screenshot(path=f"{OUT}/{nombre}.png")

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(channel="chrome")
        for w, h, suf in [(1440, 900, ""), (390, 844, "-movil")]:
            ctx = await b.new_context(viewport={"width": w, "height": h}, device_scale_factor=2, locale="es-PE")
            page = await ctx.new_page()
            await page.goto(BASE); await page.wait_for_timeout(2000)
            await page.screenshot(path=f"{OUT}/01-login{suf}.png")
            await login(page, "policia", "PNP-2045")
            await page.screenshot(path=f"{OUT}/02-policia-inicio{suf}.png")
            if not suf:
                await ir(page, "denuncias", "03-policia-denuncias")
                await ir(page, "tracking", "04-policia-actuaciones")
                await ir(page, "documents", "05-policia-documentos")
                await ir(page, "chat", "06-policia-chat")
            await ctx.close()
            ctx = await b.new_context(viewport={"width": w, "height": h}, device_scale_factor=2, locale="es-PE")
            page = await ctx.new_page()
            await login(page, "fiscal", "FN-1023")
            await page.screenshot(path=f"{OUT}/07-fiscal-inicio{suf}.png")
            if not suf:
                await ir(page, "denuncias", "08-fiscal-carpetas")
                await ir(page, "provisions", "09-fiscal-disposiciones")
            await ctx.close()
            ctx = await b.new_context(viewport={"width": w, "height": h}, device_scale_factor=2, locale="es-PE")
            page = await ctx.new_page()
            await login(page, "admin", "ADM-0001")
            await page.screenshot(path=f"{OUT}/10-admin-inicio{suf}.png")
            if not suf:
                await ir(page, "functions", "11-admin-funcionarios")
                await ir(page, "users", "12-admin-permisos")
                await ir(page, "activity", "13-admin-actividad")
            await ctx.close()
        await b.close()
asyncio.run(main())
