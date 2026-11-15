# Capturas en modo oscuro de los dos prototipos
import asyncio, os
from playwright.async_api import async_playwright
SD = "http://localhost:8080"; PC = "http://localhost:8081"
OUT = "capturas"; os.makedirs(OUT, exist_ok=True)

async def ctx_dark(b, key, w=1440, h=900):
    ctx = await b.new_context(viewport={"width": w, "height": h}, locale="es-PE")
    await ctx.add_init_script(f"try{{localStorage.setItem('{key}','dark')}}catch(e){{}}")
    return ctx

async def sd_login(page, rol, codigo):
    await page.goto(SD); await page.wait_for_selector("#loginForm")
    await page.screenshot(path=f"{OUT}/sd-00-login.png")
    await page.select_option("#role", rol)
    await page.fill("#institutionCode", codigo); await page.fill("#password", "Demo1234")
    await page.click("#loginForm button[type=submit]"); await page.wait_for_timeout(3500)

async def ir(page, pagina, nombre, full=False):
    await page.evaluate("p => { const b=[...document.querySelectorAll('[data-page]')].find(x=>x.dataset.page===p); if(b) b.click(); }", pagina)
    await page.wait_for_timeout(2500)
    await page.screenshot(path=f"{OUT}/{nombre}.png", full_page=full)

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(channel="chrome")
        # ---- Sistema de gestión ----
        for rol, cod, pags in [("policia", "PNP-2045", ["denuncias", "tracking", "documents", "reports", "chat", "schedule"]),
                               ("fiscal", "FN-1023", ["denuncias", "provisions"]),
                               ("admin", "ADM-0001", ["functions", "users", "activity", "backups"])]:
            ctx = await ctx_dark(b, "sgd-modo-color"); page = await ctx.new_page()
            await sd_login(page, rol, cod)
            await page.screenshot(path=f"{OUT}/sd-{rol}-inicio.png")
            for pg in pags:
                await ir(page, pg, f"sd-{rol}-{pg}")
            # abrir el primer detalle de denuncia (si existe)
            if rol == "policia":
                await ir(page, "denuncias", "sd-policia-denuncias")
                btn = await page.query_selector("button.action-icon.view, .action-icon.view, [data-action='ver']")
                if btn:
                    await btn.click(); await page.wait_for_timeout(2000)
                    await page.screenshot(path=f"{OUT}/sd-policia-detalle.png")
            await ctx.close()
        # ---- Portal ciudadano ----
        ctx = await ctx_dark(b, "pnp-color-mode"); page = await ctx.new_page()
        await page.goto(PC); await page.wait_for_timeout(1500)
        await page.screenshot(path=f"{OUT}/pc-00-inicio.png")
        await page.fill("#caseCode", "D-000584"); await page.keyboard.press("Enter"); await page.wait_for_timeout(1500)
        await page.screenshot(path=f"{OUT}/pc-01-pin.png")
        await page.fill("#citizenPin", "123456"); await page.keyboard.press("Enter"); await page.wait_for_timeout(2500)
        await page.screenshot(path=f"{OUT}/pc-02-casos.png", full_page=True)
        opt = await page.query_selector(".case-option")
        if opt:
            await opt.click(); await page.wait_for_timeout(2500)
            await page.screenshot(path=f"{OUT}/pc-03-detalle.png", full_page=True)
        for v in ["prosecutor", "notifications", "help"]:
            await page.evaluate("v => { const b=[...document.querySelectorAll('[data-view]')].find(x=>x.dataset.view===v); if(b) b.click(); }", v)
            await page.wait_for_timeout(1500)
            await page.screenshot(path=f"{OUT}/pc-{v}.png", full_page=True)
        m = await b.new_context(viewport={"width": 390, "height": 844}, locale="es-PE")
        await m.add_init_script("try{localStorage.setItem('pnp-color-mode','dark')}catch(e){}")
        mp = await m.new_page(); await mp.goto(PC); await mp.wait_for_timeout(1500)
        await mp.screenshot(path=f"{OUT}/pc-movil.png", full_page=True)
        await b.close()
asyncio.run(main())
