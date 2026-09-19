#!/usr/bin/env bash

set -euo pipefail

version="${1:-}"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-((alpha|beta|rc)\.[0-9]+))?$ ]] || {
  printf 'Usage: %s VERSION\n' "$0" >&2
  exit 1
}

changelog="CHANGELOG.md"
[[ -f "$changelog" ]] || {
  printf 'Error: %s is missing.\n' "$changelog" >&2
  exit 1
}

awk -v version="$version" '
  $0 ~ "^## \\[" version "\\] - [0-9]{4}-[0-9]{2}-[0-9]{2}$" {
    found = 1
    next
  }
  found && /^## / { exit }
  found { print }
  END {
    if (!found) exit 2
  }
' "$changelog"
