#!/usr/bin/env python3
"""
Validateur du catalogue SCHALOM.

Usage :
    python3 tools/validate_catalog.py schalom_catalog.json

Termine avec :
    SCHALOM CATALOG: VALID
ou :
    SCHALOM CATALOG: INVALID
    (suivi de la liste précise des erreurs)

Vérifie : JSON valide, schema_version présente, identifiants uniques (par
catégorie), "type" cohérent avec la catégorie, nom présent, URLs correctement
formées lorsqu'elles existent, stream_type cohérent, booléens correctement
typés, dates ISO 8601 correctement formées, aucune chaîne TV sans "id".
"""

import json
import re
import sys
from urllib.parse import urlparse

VALID_STREAM_TYPES = {"hls", "mp4", "dash", "local", "unknown"}
VALID_TYPE_BY_CATEGORY = {
    "tv": "tv",
    "radio": "radio",
    "films": "film",
    "series": "serie",
    "dessins_animes": "dessin_anime",
    "anime": "anime",
    "jeux": "jeu",
    "applications": "application",
    "actualites": "actualite",
    "livres": "livre",
}
ISO_DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


def is_valid_iso_date(value: str) -> bool:
    return bool(ISO_DATE_RE.match(value))


def is_valid_url(value: str) -> bool:
    if not value:
        return True  # une URL vide est autorisée (flux non configuré)
    parsed = urlparse(value)
    return parsed.scheme in ("http", "https") and bool(parsed.netloc)


def validate_entry(category: str, index: int, entry: dict, seen_ids: set, errors: list) -> None:
    prefix = f"[{category}][{index}]"

    if not isinstance(entry, dict):
        errors.append(f"{prefix} l'entrée n'est pas un objet JSON")
        return

    entry_id = entry.get("id")
    if not entry_id or not isinstance(entry_id, str):
        errors.append(f"{prefix} 'id' manquant ou invalide")
    elif entry_id in seen_ids:
        errors.append(f"{prefix} identifiant dupliqué : '{entry_id}'")
    else:
        seen_ids.add(entry_id)

    name = entry.get("name")
    if not name or not isinstance(name, str):
        errors.append(f"{prefix} 'name' manquant ou vide (id={entry_id})")

    expected_type = VALID_TYPE_BY_CATEGORY.get(category)
    if expected_type and entry.get("type") not in (expected_type, category):
        errors.append(
            f"{prefix} 'type' incohérent avec la catégorie '{category}' "
            f"(trouvé: {entry.get('type')!r})"
        )

    stream_type = entry.get("stream_type", "unknown")
    if stream_type not in VALID_STREAM_TYPES:
        errors.append(f"{prefix} 'stream_type' invalide : {stream_type!r} (id={entry_id})")

    stream_url = entry.get("stream_url", "")
    if stream_url and not is_valid_url(stream_url):
        errors.append(f"{prefix} 'stream_url' mal formée : {stream_url!r} (id={entry_id})")

    for bool_field in ("is_live", "is_active", "offline_available", "favorite"):
        if bool_field in entry and not isinstance(entry[bool_field], bool):
            errors.append(f"{prefix} '{bool_field}' doit être un booléen (id={entry_id})")

    updated_at = entry.get("updated_at", "")
    if updated_at and not is_valid_iso_date(updated_at):
        errors.append(f"{prefix} 'updated_at' n'est pas une date ISO 8601 valide : {updated_at!r}")


def validate_catalog(data: dict) -> list:
    errors = []

    if "schema_version" not in data:
        errors.append("champ racine 'schema_version' manquant")

    categories = data.get("categories")
    if not isinstance(categories, dict):
        errors.append("champ racine 'categories' manquant ou invalide")
        return errors

    for category, entries in categories.items():
        if not isinstance(entries, list):
            errors.append(f"[{category}] doit être une liste")
            continue
        seen_ids = set()
        for index, entry in enumerate(entries):
            validate_entry(category, index, entry, seen_ids, errors)
            if category == "tv" and not entry.get("id"):
                errors.append(f"[tv][{index}] une chaîne TV sans 'id' n'est pas autorisée")

    return errors


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage : python3 tools/validate_catalog.py <catalogue.json>")
        return 2

    path = sys.argv[1]
    try:
        with open(path, "r", encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        print(f"SCHALOM CATALOG: INVALID\n - fichier introuvable : {path}")
        return 1
    except json.JSONDecodeError as exc:
        print(f"SCHALOM CATALOG: INVALID\n - JSON malformé : {exc}")
        return 1

    errors = validate_catalog(data)
    if errors:
        print("SCHALOM CATALOG: INVALID")
        for error in errors:
            print(f" - {error}")
        return 1

    print("SCHALOM CATALOG: VALID")
    return 0


if __name__ == "__main__":
    sys.exit(main())
