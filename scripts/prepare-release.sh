#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/prepare-release.sh VERSION
  scripts/prepare-release.sh VERSION --publish [--yes]

Prepare updates appVersionName, creates the release-preparation commit, and
pushes main. After CI passes, use --publish to create and push the vVERSION tag.
EOF
}

fail() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

version="${1:-}"
mode="${2:-prepare}"
confirmation="${3:-}"

[[ -n "$version" ]] || { usage; exit 1; }
[[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] \
  || fail "VERSION must use MAJOR.MINOR.PATCH semantic versioning."
[[ "$mode" == "prepare" || "$mode" == "--publish" ]] \
  || fail "The optional second argument must be --publish."
[[ -z "$confirmation" || "$confirmation" == "--yes" ]] \
  || fail "The optional third argument must be --yes."
[[ "$mode" == "--publish" || -z "$confirmation" ]] \
  || fail "--yes can only be used with --publish."

root="$(git rev-parse --show-toplevel)"
cd "$root"

[[ "$(git branch --show-current)" == "main" ]] || fail "Switch to main before releasing."
git status --porcelain | awk '$2 != "CHANGELOG.md" { dirty = 1 } END { exit dirty }' \
  || fail "Only the intentional CHANGELOG.md release edit may be uncommitted."
[[ -f gradle.properties ]] || fail "gradle.properties is missing."
grep -q '^appVersionName=' gradle.properties || fail "appVersionName is missing from gradle.properties."
[[ -x scripts/extract-changelog-section.sh ]] || fail "scripts/extract-changelog-section.sh is missing or not executable."

validate_changelog_entry() {
  scripts/extract-changelog-section.sh "$1" >/dev/null \
    || fail "CHANGELOG.md must contain a dated entry for [$1]."
}

tag="v$version"

if [[ "$mode" == "prepare" ]]; then
  validate_changelog_entry "$version"
  git fetch origin main
  [[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]] \
    || fail "Local main must exactly match origin/main. Run git pull --ff-only first."
  git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null 2>&1 \
    && fail "Remote tag $tag already exists."
  git rev-parse -q --verify "refs/tags/$tag" >/dev/null \
    && fail "Local tag $tag already exists."

  perl -0pi -e "s/^appVersionName=.*/appVersionName=$version/m" gradle.properties
  git add gradle.properties CHANGELOG.md
  git commit -m "chore(release): prepare $tag"
  git push origin main

  printf 'Prepared and pushed %s. Wait for CI, then run:\n  scripts/prepare-release.sh %s --publish\n' \
    "$tag" "$version"
  exit 0
fi

git fetch origin main
[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]] \
  || fail "Local main must exactly match origin/main. Run git pull --ff-only first."
[[ "$(sed -n 's/^appVersionName=//p' gradle.properties)" == "$version" ]] \
  || fail "appVersionName does not match $version."
validate_changelog_entry "$version"
git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null 2>&1 \
  && fail "Remote tag $tag already exists."
git rev-parse -q --verify "refs/tags/$tag" >/dev/null \
  && fail "Local tag $tag already exists."

if [[ "$confirmation" != "--yes" ]]; then
  read -r -p "CI must have passed on origin/main. Create and push $tag to trigger the public release? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES|Yes) ;;
    *) printf 'Cancelled; no tag was created.\n'; exit 0 ;;
  esac
fi

git tag -a "$tag" -m "Release $tag"
git push origin "$tag"
printf 'Published %s. GitHub Actions will build and publish the release.\n' "$tag"
