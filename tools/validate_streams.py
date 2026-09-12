#!/usr/bin/env python3

import json
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

TIMEOUT = 8
MAX_WORKERS = 32

def check(item):
    url = item.get("stream_url", "").strip()

    if not url.startswith(("http://", "https://")):
        return item, "invalid", "URL absente ou non HTTP(S)"

    try:
        req = Request(
            url,
            method="GET",
            headers={
                "User-Agent": "SCHAL-TV-stream-validator/1.0",
                "Range": "bytes=0-4095",
                "Accept": "*/*"
            }
        )

        with urlopen(req, timeout=TIMEOUT) as response:
            code = response.status
            data = response.read(4096)

        if code >= 400:
            return item, "invalid", f"HTTP {code}"

        if not data:
            return item, "invalid", "réponse média vide"

        if ".m3u8" in url.lower() or b"#EXTM3U" in data:
            return item, "online", "HLS accessible"

        return item, "online", f"HTTP {code}"

    except HTTPError as e:
        return item, "invalid", f"HTTP {e.code}"

    except (URLError, TimeoutError, OSError) as e:
        return item, "offline", str(e)

    except Exception as e:
        return item, "invalid", str(e)


def main():
    if len(sys.argv) != 2:
        print("Usage: validate_streams.py catalog.json")
        return 2

    path = sys.argv[1]

    with open(path, encoding="utf-8") as f:
        data = json.load(f)

    channels = data.get("categories", {}).get("tv", [])

    valid = []
    invalid = []

    print(f"Flux à tester : {len(channels)}")

    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as pool:
        futures = [pool.submit(check, c) for c in channels]

        for future in as_completed(futures):
            item, status, reason = future.result()

            item = dict(item)
            item["validation_status"] = status
            item["validation_reason"] = reason
            item["last_checked"] = int(time.time())

            if status == "online":
                valid.append(item)
            else:
                invalid.append(item)

    valid.sort(key=lambda x: x.get("name", "").lower())
    invalid.sort(key=lambda x: x.get("name", "").lower())

    out = {
        "schema_version": data.get("schema_version", "1.0"),
        "generated_at": int(time.time()),
        "categories": {
            "tv": valid
        }
    }

    with open("channels-valid.json", "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))

    with open("channels-invalid.json", "w", encoding="utf-8") as f:
        json.dump(
            {
                "generated_at": int(time.time()),
                "channels": invalid
            },
            f,
            ensure_ascii=False,
            separators=(",", ":")
        )

    report = {
        "total_analyzed": len(channels),
        "valid": len(valid),
        "invalid": len(invalid)
    }

    with open("channels-report.json", "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)

    print(json.dumps(report, ensure_ascii=False, indent=2))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
