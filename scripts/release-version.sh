#!/usr/bin/env bash
set -euo pipefail

usage()
{
    echo "usage: scripts/release-version.sh <patch|minor|major>" >&2
}

if [ "$#" -ne 1 ]; then
    usage
    exit 2
fi

bump="$1"
case "$bump" in
    patch|minor|major) ;;
    *)
        usage
        exit 2
        ;;
esac

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

status="$(git status --porcelain)"
if [ -n "$status" ]; then
    echo "Refusing to prepare a release because the working tree is not clean." >&2
    echo >&2
    echo "$status" >&2
    exit 1
fi

current_version="$(sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' pom.xml | head -n 1)"
if [[ ! "$current_version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)-SNAPSHOT$ ]]; then
    echo "Expected pom.xml to contain a x.y.z-SNAPSHOT project version, found: $current_version" >&2
    exit 1
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"

release_version="$major.$minor.$patch"
case "$bump" in
    patch)
        next_version="$major.$minor.$((patch + 1))-SNAPSHOT"
        ;;
    minor)
        next_version="$major.$((minor + 1)).0-SNAPSHOT"
        ;;
    major)
        next_version="$((major + 1)).0.0-SNAPSHOT"
        ;;
esac
tag="v$release_version"

cat <<EOF
Current version: $current_version
Release version: $release_version
Next version:    $next_version
Tag:             $tag

This will update pom.xml to $release_version.
EOF

read -r -p "Continue? [y/N] " answer
case "$answer" in
    y|Y|yes|YES) ;;
    *)
        echo "Aborted."
        exit 1
        ;;
esac

python3 - "$release_version" <<'PY'
import re
import sys
from pathlib import Path

version = sys.argv[1]
pom = Path("pom.xml")
text = pom.read_text()
updated, count = re.subn(r"(<version>)[^<]+(</version>)", rf"\g<1>{version}\2", text, count=1)
if count != 1:
    raise SystemExit("Could not update the project version in pom.xml")
pom.write_text(updated)
PY

echo "pom.xml updated to $release_version"
echo "After committing/tagging the release, bump to: $next_version"
