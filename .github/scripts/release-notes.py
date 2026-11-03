#!/usr/bin/env python3
"""Puts the notes in .github/whatsnew onto a build that is already uploaded.

    release-notes.py play <versionCode> [--dry-run]
    release-notes.py testflight <buildNumber> [--dry-run]
    release-notes.py appstore <buildNumber> [--version-name <x.y.z>] [--dry-run]

--dry-run reads everything as usual and prints what it would write, without writing to any store.

Play: the notes of the internal-track release holding that versionCode. They travel with the
release when it is promoted. TestFlight: the build's "What to Test", the only notes a TestFlight
build has. App Store: finds or creates the App Store version for the build's version name, attaches
the build and writes "What's New" on its localizations, so only "Submit for Review" is left by hand.
It never submits, releases or touches a version that is in review or later. --version-name is the
name the release gave both apps; the step refuses a build that reports another one.
"""

from __future__ import annotations

import json
import os
import sys
import time
from dataclasses import dataclass
from pathlib import Path

# The store clients are imported where they are used, so test_release_notes.py can load this file
# with nothing installed.

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


def play(version_code: str, dry_run: bool) -> None:
    from google.auth.transport.requests import AuthorizedSession
    from google.oauth2 import service_account

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

    if dry_run:
        # An edit is only a draft until committed; deleting it leaves Play as it was.
        session.delete(f"{base}/{edit}")
        print(f"Play: would set notes on versionCode {version_code} ({', '.join(notes())})")
        return

    release["releaseNotes"] = [{"language": lang, "text": text} for lang, text in notes().items()]
    session.put(f"{base}/{edit}/tracks/{PLAY_TRACK}", json=track).raise_for_status()
    session.post(f"{base}/{edit}:commit").raise_for_status()
    print(f"Play: notes set on versionCode {version_code} ({', '.join(notes())})")


def asc_session():
    import jwt
    import requests

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


def find_app(session) -> dict:
    app = session.get(f"{ASC_API}/apps", params={"filter[bundleId]": BUNDLE_ID})
    app.raise_for_status()
    return app.json()["data"][0]


def find_build(session, app_id: str, build_number: str) -> dict:
    # A build only appears once App Store Connect has received it; processing can take a while.
    for _ in range(30):
        builds = session.get(
            f"{ASC_API}/builds", params={"filter[app]": app_id, "filter[version]": build_number}
        )
        builds.raise_for_status()
        if builds.json()["data"]:
            return builds.json()["data"][0]
        time.sleep(60)
    sys.exit(f"::error::Build {build_number} never showed up in App Store Connect")


# appVersionState values, plus the deprecated appStoreState's for a version that only reports that one.
EDITABLE_STATES = {
    "PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED", "METADATA_REJECTED", "INVALID_BINARY",
}
DONE_STATES = {
    "READY_FOR_DISTRIBUTION", "REPLACED_WITH_NEW_VERSION",
    "READY_FOR_SALE", "PREORDER_READY_FOR_SALE", "REMOVED_FROM_SALE", "DEVELOPER_REMOVED_FROM_SALE",
}
# Everything else is in flight — submitted, in review, approved or waiting for release — including a
# state Apple adds later: a state the script does not know must never lead to a write.


@dataclass(frozen=True)
class AppStoreVersion:
    id: str
    name: str
    state: str


@dataclass(frozen=True)
class VersionPlan:
    action: str  # "use" | "rename" | "create" | "skip"
    version: AppStoreVersion | None = None
    reason: str = ""


def version_key(name: str) -> tuple[int, ...]:
    return tuple(int(part) for part in name.split("."))


def plan_version(versions: list[AppStoreVersion], name: str) -> VersionPlan:
    """Which App Store version a build of `name` goes on. Never plans a write to a version in flight."""
    def editable(v: AppStoreVersion) -> bool:
        return v.state in EDITABLE_STATES

    def in_flight(v: AppStoreVersion) -> bool:
        return v.state not in EDITABLE_STATES and v.state not in DONE_STATES

    same = [v for v in versions if v.name == name]
    if same:
        target = next((v for v in same if editable(v)), None)
        if target:
            return VersionPlan("use", target)
        return VersionPlan("skip", same[0], f"version {name} is already {same[0].state}")

    flying = next((v for v in versions if in_flight(v)), None)
    if flying:
        return VersionPlan("skip", flying, f"version {flying.name} is {flying.state}")

    newer = [v for v in versions if version_key(v.name) > version_key(name)]
    if newer:
        top = max(newer, key=lambda v: version_key(v.name))
        return VersionPlan("skip", top, f"build {name} is older than version {top.name}")

    # App Store Connect keeps a single editable version per platform; an unsubmitted older one is
    # renamed rather than left to block the new one.
    stale = next((v for v in versions if editable(v)), None)
    if stale:
        return VersionPlan("rename", stale)
    return VersionPlan("create")


@dataclass(frozen=True)
class NotesPlan:
    texts: dict[str, str]  # localization → its What's New
    fallbacks: list[str]   # localizations given the English notes
    unused: list[str]      # notes files with no localization to go on


def plan_notes(localizations: list[str], texts: dict[str, str]) -> NotesPlan:
    """What's New per existing localization, by App Store Connect locale. Never adds a localization."""
    english = texts.get("en-GB") or next(iter(texts.values()))
    planned = {loc: texts.get(loc, english) for loc in localizations}
    return NotesPlan(
        texts=planned,
        fallbacks=[loc for loc in localizations if loc not in texts],
        unused=[loc for loc in texts if loc not in localizations],
    )


def testflight(build_number: str, dry_run: bool) -> None:
    session = asc_session()

    app = find_app(session)
    primary_locale = app["attributes"]["primaryLocale"]
    build = find_build(session, app["id"], build_number)["id"]

    existing = session.get(f"{ASC_API}/builds/{build}/betaBuildLocalizations")
    existing.raise_for_status()
    by_locale = {loc["attributes"]["locale"]: loc["id"] for loc in existing.json()["data"]}

    texts = {asc_locale(lang): text for lang, text in notes().items()}
    # Testers whose language has no notes see the app's primary language, so it must have some.
    if primary_locale not in texts:
        texts[primary_locale] = texts.get("en-GB") or next(iter(texts.values()))

    if dry_run:
        print(f"TestFlight: would set notes on build {build_number} ({', '.join(texts)})")
        return

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


def check_version_name(build_version: str, release_version: str | None) -> str | None:
    """Why a build cannot go on the App Store version of this release, if it cannot."""
    if release_version and build_version != release_version:
        return (f"build reports version {build_version} but the release is {release_version}; "
                "Android and iOS must ship the same version name")
    return None


def asc_check(response, what: str) -> dict:
    if not response.ok:
        sys.exit(f"::error::App Store Connect refused to {what}: {response.status_code} {response.text}")
    return response.json() if response.content else {}


def build_version_name(session, build_id: str) -> str:
    # A build's CFBundleShortVersionString lives on its pre-release version, not on the build.
    version = asc_check(session.get(f"{ASC_API}/builds/{build_id}/preReleaseVersion"),
                        f"read the version of build {build_id}")
    return version["data"]["attributes"]["version"]


def wait_until_processed(session, build_id: str, build_number: str) -> dict:
    for _ in range(30):
        build = asc_check(session.get(f"{ASC_API}/builds/{build_id}"), f"read build {build_number}")
        attributes = build["data"]["attributes"]
        state = attributes.get("processingState")
        if state == "VALID":
            return attributes
        if state in ("FAILED", "INVALID"):
            sys.exit(f"::error::App Store Connect could not process build {build_number}: {state}")
        time.sleep(60)
    sys.exit(f"::error::Build {build_number} was still processing after 30 minutes; re-run this job")


def list_versions(session, app_id: str) -> list[AppStoreVersion]:
    response = session.get(
        f"{ASC_API}/apps/{app_id}/appStoreVersions",
        params={"filter[platform]": "IOS", "limit": 200,
                "fields[appStoreVersions]": "versionString,appVersionState,appStoreState"},
    )
    return [
        AppStoreVersion(
            id=v["id"],
            name=v["attributes"]["versionString"],
            state=v["attributes"].get("appVersionState") or v["attributes"].get("appStoreState") or "UNKNOWN",
        )
        for v in asc_check(response, "list the App Store versions")["data"]
    ]


def list_localizations(session, version_id: str) -> dict[str, str]:
    """Locale → localization id of an App Store version."""
    response = session.get(f"{ASC_API}/appStoreVersions/{version_id}/appStoreVersionLocalizations",
                           params={"limit": 200})
    data = asc_check(response, "list the version's localizations")["data"]
    return {loc["attributes"]["locale"]: loc["id"] for loc in data}


def appstore(build_number: str, dry_run: bool, release_version: str | None = None) -> None:
    session = asc_session()
    app = find_app(session)
    build = find_build(session, app["id"], build_number)

    name = build_version_name(session, build["id"])
    mismatch = check_version_name(name, release_version)
    if mismatch:
        sys.exit(f"::error::{mismatch}")

    attributes = wait_until_processed(session, build["id"], build_number)
    if attributes.get("usesNonExemptEncryption") is None:
        sys.exit(f"::error::Build {build_number} is missing its export compliance declaration "
                 "(ITSAppUsesNonExemptEncryption in Info.plist); it cannot be attached or submitted")

    versions = list_versions(session, app["id"])
    plan = plan_version(versions, name)
    if plan.action == "skip":
        print(f"::warning::App Store: nothing changed, {plan.reason}")
        return

    texts = {asc_locale(lang): text for lang, text in notes().items()}
    if dry_run:
        # A new version starts with the localizations of the latest one, so those are the ones shown.
        source = plan.version or max(versions, key=lambda v: version_key(v.name), default=None)
        locales = list(list_localizations(session, source.id)) if source else []
        planned = plan_notes(locales, texts)
        what = {"use": f"use version {name}", "create": f"create version {name}",
                "rename": f"rename version {plan.version.name if plan.version else ''} to {name}"}[plan.action]
        print(f"App Store: would {what}, attach build {build_number} and set What's New on "
              f"{', '.join(planned.texts) or 'its localizations'}")
        report_notes(planned)
        return

    if plan.action == "create":
        created = asc_check(session.post(f"{ASC_API}/appStoreVersions", json={"data": {
            "type": "appStoreVersions",
            "attributes": {"platform": "IOS", "versionString": name},
            "relationships": {"app": {"data": {"type": "apps", "id": app["id"]}}},
        }}), f"create version {name}")
        version_id = created["data"]["id"]
        print(f"App Store: created version {name}")
    else:
        version_id = plan.version.id
        if plan.action == "rename":
            asc_check(session.patch(f"{ASC_API}/appStoreVersions/{version_id}", json={"data": {
                "type": "appStoreVersions", "id": version_id, "attributes": {"versionString": name},
            }}), f"rename version {plan.version.name} to {name}")
            print(f"App Store: renamed version {plan.version.name} to {name}")

    asc_check(session.patch(f"{ASC_API}/appStoreVersions/{version_id}/relationships/build",
                            json={"data": {"type": "builds", "id": build["id"]}}),
              f"attach build {build_number} to version {name}")
    print(f"App Store: build {build_number} attached to version {name}")

    localizations = list_localizations(session, version_id)
    planned = plan_notes(list(localizations), texts)
    for locale, text in planned.texts.items():
        asc_check(session.patch(f"{ASC_API}/appStoreVersionLocalizations/{localizations[locale]}", json={"data": {
            "type": "appStoreVersionLocalizations", "id": localizations[locale], "attributes": {"whatsNew": text},
        }}), f"set the {locale} What's New")
    print(f"App Store: What's New set on version {name} ({', '.join(planned.texts)})")
    report_notes(planned)


def report_notes(planned: NotesPlan) -> None:
    for locale in planned.fallbacks:
        print(f"::warning::App Store: {locale} has no notes file and gets the en-GB text")
    for locale in planned.unused:
        print(f"::warning::App Store: the {locale} notes were not used; the listing has no {locale} localization")


def main(argv: list[str]) -> None:
    dry_run = "--dry-run" in argv
    args = [a for a in argv if a != "--dry-run"]
    release_version = None
    if "--version-name" in args:
        at = args.index("--version-name")
        if at + 1 >= len(args):
            sys.exit(__doc__)
        release_version = args[at + 1] or None
        del args[at:at + 2]
    commands = {"play": play, "testflight": testflight, "appstore": appstore}
    if len(args) != 2 or args[0] not in commands or (release_version and args[0] != "appstore"):
        sys.exit(__doc__)
    if args[0] == "appstore":
        appstore(args[1], dry_run, release_version)
    else:
        commands[args[0]](args[1], dry_run)


if __name__ == "__main__":
    main(sys.argv[1:])
