#!/usr/bin/env python3
"""Puts the notes in .github/whatsnew onto a build that is already uploaded.

    release-notes.py play <versionCode>
    release-notes.py testflight <buildNumber>

Play: the notes of the internal-track release holding that versionCode. They travel with the
release when it is promoted. TestFlight: the build's "What to Test", the only notes a TestFlight
build has; the App Store's "What's New" belongs to the App Store version, written on promotion.
"""

import json
import os
import sys
import time
from pathlib import Path

import jwt
import requests
from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession

WHATSNEW = Path(__file__).resolve().parent.parent / "whatsnew"
PACKAGE_NAME = "com.geoviksoft.turnia"
BUNDLE_ID = "com.geoviksoft.turnia.Turnia"
PLAY_TRACK = "internal"
ASC_API = "https://api.appstoreconnect.apple.com/v1"


def notes() -> dict[str, str]:
    """Play language code → text, one per whatsnew-<locale> file."""
    found = {
        path.name.removeprefix("whatsnew-"): path.read_text(encoding="utf-8").strip()
        for path in sorted(WHATSNEW.glob("whatsnew-*"))
    }
    if not found:
        sys.exit(f"::error::No release notes in {WHATSNEW}")
    return found


def play(version_code: str) -> None:
    credentials = service_account.Credentials.from_service_account_info(
        json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]),
        scopes=["https://www.googleapis.com/auth/androidpublisher"],
    )
    session = AuthorizedSession(credentials)
    base = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE_NAME}/edits"

    edit = session.post(base).json()["id"]
    track = session.get(f"{base}/{edit}/tracks/{PLAY_TRACK}")
    track.raise_for_status()
    track = track.json()

    release = next(
        (r for r in track.get("releases", []) if version_code in r.get("versionCodes", [])), None
    )
    if release is None:
        session.delete(f"{base}/{edit}")
        sys.exit(f"::error::No release with versionCode {version_code} on Play's {PLAY_TRACK} track")

    release["releaseNotes"] = [{"language": lang, "text": text} for lang, text in notes().items()]
    session.put(f"{base}/{edit}/tracks/{PLAY_TRACK}", json=track).raise_for_status()
    session.post(f"{base}/{edit}:commit").raise_for_status()
    print(f"Play: notes set on versionCode {version_code} ({', '.join(notes())})")


def asc_session() -> requests.Session:
    now = int(time.time())
    token = jwt.encode(
        {"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 15 * 60, "aud": "appstoreconnect-v1"},
        os.environ["ASC_KEY_P8"],
        algorithm="ES256",
        headers={"kid": os.environ["ASC_KEY_ID"], "typ": "JWT"},
    )
    session = requests.Session()
    session.headers["Authorization"] = f"Bearer {token}"
    return session


def asc_locale(play_language: str) -> str:
    # App Store Connect names Italian without a region; the other store languages match Play's.
    return {"it-IT": "it"}.get(play_language, play_language)


def testflight(build_number: str) -> None:
    session = asc_session()

    app = session.get(f"{ASC_API}/apps", params={"filter[bundleId]": BUNDLE_ID})
    app.raise_for_status()
    app = app.json()["data"][0]
    primary_locale = app["attributes"]["primaryLocale"]

    # A build only appears once App Store Connect has received it; processing can take a while.
    for _ in range(30):
        builds = session.get(
            f"{ASC_API}/builds", params={"filter[app]": app["id"], "filter[version]": build_number}
        )
        builds.raise_for_status()
        if builds.json()["data"]:
            break
        time.sleep(60)
    else:
        sys.exit(f"::error::Build {build_number} never showed up in App Store Connect")
    build = builds.json()["data"][0]["id"]

    existing = session.get(f"{ASC_API}/builds/{build}/betaBuildLocalizations")
    existing.raise_for_status()
    by_locale = {loc["attributes"]["locale"]: loc["id"] for loc in existing.json()["data"]}

    texts = {asc_locale(lang): text for lang, text in notes().items()}
    # Testers whose language has no notes see the app's primary language, so it must have some.
    if primary_locale not in texts:
        texts[primary_locale] = texts.get("en-GB") or next(iter(texts.values()))

    written = []
    for locale, text in texts.items():
        if locale in by_locale:
            response = session.patch(
                f"{ASC_API}/betaBuildLocalizations/{by_locale[locale]}",
                json={"data": {"type": "betaBuildLocalizations", "id": by_locale[locale],
                               "attributes": {"whatsNew": text}}},
            )
        else:
            response = session.post(
                f"{ASC_API}/betaBuildLocalizations",
                json={"data": {"type": "betaBuildLocalizations",
                               "attributes": {"locale": locale, "whatsNew": text},
                               "relationships": {"build": {"data": {"type": "builds", "id": build}}}}},
            )
        if response.ok:
            written.append(locale)
        else:
            print(f"::warning::TestFlight refused the {locale} notes: {response.text}")

    if not written:
        sys.exit("::error::TestFlight accepted none of the notes")
    print(f"TestFlight: notes set on build {build_number} ({', '.join(written)})")


if __name__ == "__main__":
    if len(sys.argv) != 3 or sys.argv[1] not in ("play", "testflight"):
        sys.exit(__doc__)
    {"play": play, "testflight": testflight}[sys.argv[1]](sys.argv[2])
