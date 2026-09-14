#!/usr/bin/env python3
"""
Builds the store screenshots: a headline for healthcare staff over the brand colour, and the app
screen in a phone frame below it.

The screens in raw/ come from the app's demo mode (made-up data, see `demoModule`):

    Android:  adb shell run-as com.geoviksoft.turnia touch files/demo
    iOS:      xcrun simctl launch booted com.geoviksoft.turnia.Turnia -TurniaDemo

Each page is laid out as HTML and rendered by headless Chrome at the exact size each store asks
for, so the type is real type and not a scaled bitmap. Run from anywhere:

    python3 store/screenshots/generate.py
"""
import html
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent
RAW = ROOT / "raw"
ICON = ROOT.parent.parent / "app/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# (raw file, headline, subheading, dark background), per language.
PAGES = {
    "es": [
        ("1_calendar", "Tu cuadrante, claro de un vistazo",
         "Mañanas, tardes, noches y guardias, cada una con su color.", False),
        ("2_day", "Sabe siempre quién cubre cada turno",
         "Cada cambio queda registrado, de mano en mano.", False),
        ("3_colleagues", "Echa una mano a tu equipo",
         "Cuando un compañero necesita cambio, lo cubres con un toque.", False),
        ("4_requests", "Pide cambio sin perseguir a nadie",
         "En verde, los turnos que ya tienen quien te los cubra.", False),
        ("5_group", "Todo tu servicio en un calendario",
         "Crea el grupo de tu unidad e invita a tu equipo con un enlace.", False),
        ("6_shared", "Comparte tu cuadrante",
         "Que tu familia sepa cuándo trabajas, sin mandar fotos del calendario.", False),
        ("7_dark_calendar", "También para el turno de noche",
         "Modo oscuro para mirar tus turnos sin deslumbrarte.", True),
    ],
    "en": [
        ("1_calendar", "Your roster, clear at a glance",
         "Mornings, afternoons, nights and on-call, each in its own color.", False),
        ("2_day", "Always know who covers each shift",
         "Every swap is recorded, hand to hand.", False),
        ("3_colleagues", "Help out your team",
         "When a colleague needs a swap, cover it with one tap.", False),
        ("4_requests", "Swap shifts without chasing anyone",
         "In green, the shifts someone has already covered for you.", False),
        ("5_group", "Your whole unit in one calendar",
         "Create your team's group and invite everyone with a link.", False),
        ("6_shared", "Share your roster",
         "Let your family know when you work, no more photos of the schedule.", False),
        ("7_dark_calendar", "Made for the night shift too",
         "Dark mode to check your shifts without the glare.", True),
    ],
}

# Canvas, the phone's height on it, and how rounded the device's own screen is (in its pixels).
TARGETS = {
    "google-play": dict(raw="android", language="es", width=1080, height=1920, phone_height=1330,
                        screen_radius=110, headline=74, sub=38, top=110),
    "app-store": dict(raw="ios", language="es", width=1320, height=2868, phone_height=2040,
                      screen_radius=165, headline=96, sub=50, top=170),
    # App Store Connect still asks for the 6.5" size on its own and refuses the 6.9" one there.
    "app-store-6.5": dict(raw="ios", language="es", width=1284, height=2778, phone_height=1976,
                          screen_radius=165, headline=93, sub=48, top=165),
}
# The English listing, from screens captured with the app in English (`-AppleLanguages "(en)"`).
TARGETS["app-store-en"] = TARGETS["app-store"] | dict(raw="ios-en", language="en")
TARGETS["app-store-6.5-en"] = TARGETS["app-store-6.5"] | dict(raw="ios-en", language="en")

TEMPLATE = """<!doctype html>
<html><head><meta charset="utf-8"><style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  html, body {{ width: {width}px; height: {height}px; overflow: hidden; }}
  body {{
    font-family: -apple-system, "SF Pro Display", "Helvetica Neue", Arial, sans-serif;
    background: {background};
    color: #fff;
    position: relative;
  }}
  .glow {{
    position: absolute; inset: 0;
    background:
      radial-gradient(circle at 85% 8%, rgba(255,255,255,.16), transparent 38%),
      radial-gradient(circle at 5% 70%, rgba(255,255,255,.08), transparent 45%);
  }}
  .copy {{ position: absolute; top: {top}px; left: 8%; right: 8%; text-align: center; }}
  h1 {{
    font-size: {headline}px; line-height: 1.08; font-weight: 800; letter-spacing: -0.02em;
    text-wrap: balance;
  }}
  p {{
    margin-top: {sub_gap}px; font-size: {sub}px; line-height: 1.3; font-weight: 500;
    color: {sub_color}; text-wrap: balance;
  }}
  .phone {{
    position: absolute; left: 50%; bottom: {bottom}px; transform: translateX(-50%);
    width: {phone_width}px; height: {phone_height}px;
    padding: {bezel}px; border-radius: {frame_radius}px;
    background: linear-gradient(145deg, #2b2f33, #0d0f10);
    box-shadow: 0 {shadow_y}px {shadow_blur}px rgba(0, 20, 18, .45),
                inset 0 0 0 {rim}px rgba(255,255,255,.08);
  }}
  .phone img {{
    display: block; width: 100%; height: 100%;
    border-radius: {inner_radius}px;
  }}
</style></head>
<body>
  <div class="glow"></div>
  <div class="copy">
    <h1>{title}</h1>
    <p>{subtitle}</p>
  </div>
  <div class="phone"><img src="{image}"></div>
</body></html>
"""

FEATURE = """<!doctype html>
<html><head><meta charset="utf-8"><style>
  * {{ margin: 0; padding: 0; box-sizing: border-box; }}
  html, body {{ width: 1024px; height: 500px; overflow: hidden; }}
  body {{
    font-family: -apple-system, "SF Pro Display", "Helvetica Neue", Arial, sans-serif;
    background: linear-gradient(135deg, #00897A 0%, #006B5F 55%, #00423B 100%);
    color: #fff; position: relative;
  }}
  .glow {{ position: absolute; inset: 0;
    background: radial-gradient(circle at 80% 0%, rgba(255,255,255,.18), transparent 45%); }}
  .brand {{ position: absolute; left: 64px; top: 78px; display: flex; align-items: center; gap: 22px; }}
  .brand img {{ width: 104px; height: 104px; border-radius: 24px; box-shadow: 0 12px 30px rgba(0,0,0,.25); }}
  .brand span {{ font-size: 64px; font-weight: 800; letter-spacing: -0.02em; }}
  h1 {{ position: absolute; left: 64px; top: 236px; width: 520px;
    font-size: 46px; line-height: 1.12; font-weight: 800; letter-spacing: -0.01em; }}
  p {{ position: absolute; left: 64px; top: 356px; width: 500px; font-size: 24px; line-height: 1.3;
    color: #CDEFEA; font-weight: 500; text-wrap: balance; }}
  .phone {{ position: absolute; right: 70px; top: 44px; width: 300px; height: 673px;
    padding: 9px; border-radius: 44px; background: linear-gradient(145deg, #2b2f33, #0d0f10);
    box-shadow: 0 30px 60px rgba(0, 20, 18, .45); transform: rotate(-6deg); }}
  .phone img {{ width: 100%; height: 100%; border-radius: 36px; display: block; }}
</style></head>
<body>
  <div class="glow"></div>
  <div class="brand"><img src="{icon}"><span>Turnia</span></div>
  <h1>Tus cambios de turno, por fin en orden</h1>
  <p>El cuadrante de tu equipo y cada cambio, en un solo sitio.</p>
  <div class="phone"><img src="{image}"></div>
</body></html>
"""


def render(markup: str, width: int, height: int, out: pathlib.Path) -> None:
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8") as page:
        page.write(markup)
    subprocess.run(
        [CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
         "--force-device-scale-factor=1", "--allow-file-access-from-files",
         f"--window-size={width},{height}", f"--screenshot={out}",
         pathlib.Path(page.name).as_uri()],
        check=True, capture_output=True,
    )
    pathlib.Path(page.name).unlink()


def page_markup(target: dict, raw: pathlib.Path, title: str, subtitle: str, dark: bool) -> str:
    from PIL import Image  # only needed for the screenshots' aspect ratio

    with Image.open(raw) as shot:
        aspect = shot.width / shot.height
        scale = target["phone_height"] / shot.height

    bezel = round(target["width"] * 0.016)
    inner_height = target["phone_height"]
    inner_width = round(inner_height * aspect)
    inner_radius = round(target["screen_radius"] * scale)
    return TEMPLATE.format(
        width=target["width"],
        height=target["height"],
        background=(
            "linear-gradient(160deg, #1B211F 0%, #0E1211 60%, #050706 100%)" if dark
            else "linear-gradient(160deg, #00897A 0%, #006B5F 50%, #00423B 100%)"
        ),
        sub_color="#9FD9CF" if dark else "#CDEFEA",
        top=target["top"],
        headline=target["headline"],
        sub=target["sub"],
        sub_gap=round(target["sub"] * 0.55),
        phone_width=inner_width + 2 * bezel,
        phone_height=inner_height + 2 * bezel,
        bottom=round(target["height"] * 0.035),
        bezel=bezel,
        rim=max(1, bezel // 6),
        frame_radius=inner_radius + bezel,
        inner_radius=inner_radius,
        shadow_y=round(target["height"] * 0.02),
        shadow_blur=round(target["height"] * 0.045),
        title=html.escape(title),
        subtitle=html.escape(subtitle),
        image=raw.as_uri(),
    )


def main() -> None:
    for name, target in TARGETS.items():
        out_dir = ROOT / name
        out_dir.mkdir(exist_ok=True)
        for index, (raw_name, title, subtitle, dark) in enumerate(PAGES[target["language"]], start=1):
            raw = RAW / target["raw"] / f"{raw_name}.png"
            out = out_dir / f"{index:02d}_{raw_name.split('_', 1)[1]}.png"
            render(page_markup(target, raw, title, subtitle, dark), target["width"], target["height"], out)
            print(out.relative_to(ROOT))

    feature = ROOT / "google-play" / "feature_graphic.png"
    render(
        FEATURE.format(icon=ICON.as_uri(), image=(RAW / "android" / "1_calendar.png").as_uri()),
        1024, 500, feature,
    )
    print(feature.relative_to(ROOT))


if __name__ == "__main__":
    main()
