#!/usr/bin/env python3

import json
import ssl
import urllib.request
from datetime import datetime, timezone

BASE = "https://iptv-org.github.io/api"

URLS = {
    "channels": f"{BASE}/channels.json",
    "feeds": f"{BASE}/feeds.json",
    "logos": f"{BASE}/logos.json",
    "streams": f"{BASE}/streams.json",
    "countries": f"{BASE}/countries.json",
    "languages": f"{BASE}/languages.json",
}

def get_json(url):
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "SCHAL-TV-catalog-builder/1.0"}
    )

    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)

def main():
    print("Téléchargement des données publiques...")

    channels = get_json(URLS["channels"])
    feeds = get_json(URLS["feeds"])
    logos = get_json(URLS["logos"])
    streams = get_json(URLS["streams"])

    feeds_by_channel = {}
    for feed in feeds:
        feeds_by_channel.setdefault(
            feed.get("channel"),
            []
        ).append(feed)

    logos_by_channel = {}
    for logo in logos:
        if logo.get("in_use", True):
            logos_by_channel.setdefault(
                logo.get("channel"),
                logo.get("url", "")
            )

    streams_by_channel = {}
    for stream in streams:
        channel = stream.get("channel")
        url = stream.get("url", "")

        if not channel or not url:
            continue

        label = (stream.get("label") or "").lower()

        if "geo-blocked" in label:
            continue

        streams_by_channel.setdefault(
            channel,
            []
        ).append(stream)

    result = []

    for channel in channels:
        cid = channel.get("id", "")
        name = channel.get("name", "")

        if not cid or not name:
            continue

        if channel.get("is_nsfw", False):
            continue

        candidates = streams_by_channel.get(cid, [])

        if not candidates:
            continue

        # Priorité à un flux sans label problématique.
        candidates = sorted(
            candidates,
            key=lambda x: (
                bool(x.get("label")),
                not bool(x.get("quality")),
            )
        )

        primary = candidates[0]

        feed_info = feeds_by_channel.get(cid, [])

        languages = set()
        quality = primary.get("quality") or ""

        for feed in feed_info:
            for lang in feed.get("languages", []):
                languages.add(lang)

            if not quality:
                quality = feed.get("format", "")

        language = (
            "fra"
            if "fra" in languages
            else next(iter(languages), "")
        )

        category_list = channel.get("categories", [])

        result.append({
            "id": cid,
            "type": "tv",
            "name": name,
            "country": channel.get("country", ""),
            "language": language,
            "categories": category_list,
            "category": category_list[0] if category_list else "",
            "logo": logos_by_channel.get(cid, ""),
            "stream_url": primary.get("url", ""),
            "streams": [
                x.get("url")
                for x in candidates
                if x.get("url")
            ],
            "stream_type": (
                "hls"
                if ".m3u8" in primary.get("url", "").lower()
                else "unknown"
            ),
            "quality": quality,
            "is_live": True,
            "is_active": True,
            "offline_available": False,
            "stream_status": "unknown",
            "updated_at": datetime.now(
                timezone.utc
            ).strftime("%Y-%m-%dT%H:%M:%SZ")
        })

    # Priorité francophone puis A-Z.
    result.sort(
        key=lambda x: (
            0 if x["language"] == "fra" else 1,
            x["name"].lower()
        )
    )

    output = {
        "schema_version": "2.0",
        "generated_at": datetime.now(
            timezone.utc
        ).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "categories": {
            "tv": result
        }
    }

    with open(
        "catalog-candidate.json",
        "w",
        encoding="utf-8"
    ) as f:
        json.dump(
            output,
            f,
            ensure_ascii=False,
            separators=(",", ":")
        )

    print(
        f"Catalogue candidat : {len(result)} chaînes avec flux."
    )

if __name__ == "__main__":
    main()
