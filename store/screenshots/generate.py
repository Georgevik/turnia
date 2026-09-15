#!/usr/bin/env python3
"""
Builds the store screenshots: a headline for healthcare staff over the brand colour, and the app
screen in a device frame below it. Also builds Google Play's feature graphic.

The screens in raw/ come from the app's demo mode (made-up data, see `demoModule`):

    Android:  adb shell run-as com.geoviksoft.turnia touch files/demo
    iOS:      xcrun simctl launch booted com.geoviksoft.turnia.Turnia -TurniaDemo

and are saved as raw/{android,ios,ipad}/{language}/{screen}.png. A language whose captures are
missing is skipped, so the script runs with whatever has been captured.

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

SCREENS = ["1_calendar", "2_day", "3_colleagues", "4_requests", "5_group", "6_shared", "7_dark_calendar"]

# Per language: one (headline, subheading) per screen, in SCREENS order, then the feature graphic's.
COPY = {
    "es": dict(
        pages=[
            ("Tu cuadrante, claro de un vistazo",
             "Mañanas, tardes, noches y guardias, cada una con su color."),
            ("Sabe siempre quién cubre cada turno",
             "Cada cambio queda registrado, de mano en mano."),
            ("Echa una mano a tu equipo",
             "Cuando un compañero necesita cambio, lo cubres con un toque."),
            ("Pide cambio sin perseguir a nadie",
             "En verde, los turnos que ya tienen quien te los cubra."),
            ("Todo tu servicio en un calendario",
             "Crea el grupo de tu unidad e invita a tu equipo con un enlace."),
            ("Comparte tu cuadrante",
             "Que tu familia sepa cuándo trabajas, sin mandar fotos del calendario."),
            ("También para el turno de noche",
             "Modo oscuro para mirar tus turnos sin deslumbrarte."),
        ],
        feature=("Tus cambios de turno, por fin en orden",
                 "El cuadrante de tu equipo y cada cambio, en un solo sitio."),
    ),
    "en": dict(
        pages=[
            ("Your roster, clear at a glance",
             "Mornings, afternoons, nights and on-call, each in its own color."),
            ("Always know who covers each shift",
             "Every swap is recorded, hand to hand."),
            ("Help out your team",
             "When a colleague needs a swap, cover it with one tap."),
            ("Swap shifts without chasing anyone",
             "In green, the shifts someone has already covered for you."),
            ("Your whole unit in one calendar",
             "Create your team's group and invite everyone with a link."),
            ("Share your roster",
             "Let your family know when you work, no more photos of the schedule."),
            ("Made for the night shift too",
             "Dark mode to check your shifts without the glare."),
        ],
        feature=("Your shift swaps, finally in order",
                 "Your team's roster and every swap, in one place."),
    ),
    "fr": dict(
        pages=[
            ("Ton planning, clair en un coup d'œil",
             "Matins, soirs, nuits et gardes, chacun avec sa couleur."),
            ("Sache toujours qui assure chaque garde",
             "Chaque échange est enregistré, de main en main."),
            ("Donne un coup de main à ton équipe",
             "Quand un collègue cherche un échange, prends sa garde en un geste."),
            ("Échange sans courir après personne",
             "En vert, les gardes que quelqu'un a déjà prises pour toi."),
            ("Tout ton service dans un calendrier",
             "Crée le groupe de ton unité et invite ton équipe avec un lien."),
            ("Partage ton planning",
             "Que ta famille sache quand tu travailles, sans envoyer de photos."),
            ("Pensée aussi pour les nuits",
             "Le mode sombre pour consulter tes gardes sans être ébloui."),
        ],
        feature=("Tes échanges de garde, enfin en ordre",
                 "Le planning de ton équipe et chaque échange, au même endroit."),
    ),
    "de": dict(
        pages=[
            ("Dein Dienstplan auf einen Blick",
             "Früh, Spät, Nacht und Bereitschaft, jeder Dienst in seiner Farbe."),
            ("Wisse immer, wer welchen Dienst hat",
             "Jeder Tausch wird festgehalten, von Hand zu Hand."),
            ("Hilf deinem Team aus",
             "Braucht eine Kollegin einen Tausch, übernimmst du mit einem Tipp."),
            ("Tauschen, ohne hinterherzulaufen",
             "In Grün die Dienste, die schon jemand für dich übernommen hat."),
            ("Deine ganze Station in einem Kalender",
             "Erstelle eine Gruppe und lade dein Team per Link ein."),
            ("Teile deinen Dienstplan",
             "Deine Familie weiß, wann du arbeitest – ohne Fotos vom Plan."),
            ("Auch für den Nachtdienst",
             "Dunkelmodus, damit dich deine Dienste nachts nicht blenden."),
        ],
        feature=("Diensttausch, endlich in Ordnung",
                 "Der Dienstplan deines Teams und jeder Tausch an einem Ort."),
    ),
    "it": dict(
        pages=[
            ("Il tuo piano turni a colpo d'occhio",
             "Mattine, pomeriggi, notti e guardie, ognuno con il suo colore."),
            ("Sai sempre chi copre ogni turno",
             "Ogni cambio resta registrato, di mano in mano."),
            ("Dai una mano al tuo team",
             "Quando un collega cerca un cambio, lo copri con un tocco."),
            ("Chiedi un cambio senza rincorrere nessuno",
             "In verde, i turni che qualcuno ha già coperto per te."),
            ("Tutto il reparto in un calendario",
             "Crea il gruppo della tua unità e invita il team con un link."),
            ("Condividi il tuo piano turni",
             "La tua famiglia sa quando lavori, senza mandare foto del calendario."),
            ("Pensata anche per la notte",
             "Modalità scura per guardare i turni senza abbagliarti."),
        ],
        feature=("I tuoi cambi turno, finalmente in ordine",
                 "Il piano turni del tuo team e ogni cambio, in un solo posto."),
    ),
}

# Canvas, the device's screen height on it, and how rounded the device's own screen is (in its
# pixels). `bezel` is the frame's thickness as a share of the canvas width.
TARGETS = {
    "google-play": dict(raw="android", width=1080, height=1920, screen_height=1330,
                        screen_radius=110, bezel=0.016, headline=74, sub=38, top=110),
    "app-store": dict(raw="ios", width=1320, height=2868, screen_height=2040,
                      screen_radius=165, bezel=0.016, headline=96, sub=50, top=170),
    # App Store Connect still asks for the 6.5" size on its own and refuses the 6.9" one there.
    "app-store-6.5": dict(raw="ios", width=1284, height=2778, screen_height=1976,
                          screen_radius=165, bezel=0.016, headline=93, sub=48, top=165),
    # iPad 13": the tablet is wider than the phone, so it takes a thicker frame and less height.
    "app-store-ipad": dict(raw="ipad", width=2064, height=2752, screen_height=1880,
                           screen_radius=36, bezel=0.022, headline=118, sub=60, top=190),
}

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
  .device {{
    position: absolute; left: 50%; bottom: {bottom}px; transform: translateX(-50%);
    width: {device_width}px; height: {device_height}px;
    padding: {bezel}px; border-radius: {frame_radius}px;
    background: linear-gradient(145deg, #2b2f33, #0d0f10);
    box-shadow: 0 {shadow_y}px {shadow_blur}px rgba(0, 20, 18, .45),
                inset 0 0 0 {rim}px rgba(255,255,255,.08);
  }}
  .device img {{
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
  <div class="device"><img src="{image}"></div>
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
  .brand {{ position: absolute; left: 64px; top: 70px; display: flex; align-items: center; gap: 22px; }}
  .brand img {{ width: 104px; height: 104px; border-radius: 24px; box-shadow: 0 12px 30px rgba(0,0,0,.25); }}
  .brand span {{ font-size: 64px; font-weight: 800; letter-spacing: -0.02em; }}
  .copy {{ position: absolute; left: 64px; top: 220px; width: 540px; }}
  h1 {{ font-size: 44px; line-height: 1.12; font-weight: 800; letter-spacing: -0.01em; text-wrap: balance; }}
  p {{ margin-top: 18px; font-size: 24px; line-height: 1.3; color: #CDEFEA; font-weight: 500; text-wrap: balance; }}
  .phone {{ position: absolute; right: 70px; top: 44px; width: 300px; height: 673px;
    padding: 9px; border-radius: 44px; background: linear-gradient(145deg, #2b2f33, #0d0f10);
    box-shadow: 0 30px 60px rgba(0, 20, 18, .45); transform: rotate(-6deg); }}
  .phone img {{ width: 100%; height: 100%; border-radius: 36px; display: block; }}
</style></head>
<body>
  <div class="glow"></div>
  <div class="brand"><img src="{icon}"><span>Turnia</span></div>
  <div class="copy"><h1>{title}</h1><p>{subtitle}</p></div>
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
        scale = target["screen_height"] / shot.height

    bezel = round(target["width"] * target["bezel"])
    inner_height = target["screen_height"]
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
        device_width=inner_width + 2 * bezel,
        device_height=inner_height + 2 * bezel,
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
        for language, copy in COPY.items():
            raws = [RAW / target["raw"] / language / f"{screen}.png" for screen in SCREENS]
            if not all(raw.exists() for raw in raws):
                continue
            out_dir = ROOT / name / language
            out_dir.mkdir(parents=True, exist_ok=True)
            for index, (screen, raw, (title, subtitle)) in enumerate(zip(SCREENS, raws, copy["pages"]), start=1):
                out = out_dir / f"{index:02d}_{screen.split('_', 1)[1]}.png"
                dark = screen.startswith("7_")
                render(page_markup(target, raw, title, subtitle, dark), target["width"], target["height"], out)
                print(out.relative_to(ROOT))

    for language, copy in COPY.items():
        raw = RAW / "android" / language / "1_calendar.png"
        if not raw.exists():
            continue
        feature = ROOT / "google-play" / language / "feature_graphic.png"
        feature.parent.mkdir(parents=True, exist_ok=True)
        title, subtitle = copy["feature"]
        render(
            FEATURE.format(icon=ICON.as_uri(), image=raw.as_uri(),
                           title=html.escape(title), subtitle=html.escape(subtitle)),
            1024, 500, feature,
        )
        print(feature.relative_to(ROOT))


if __name__ == "__main__":
    main()
