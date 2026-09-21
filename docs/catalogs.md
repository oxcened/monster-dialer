# Publishing a MonsterDialer pack catalog

A catalog is a UTF-8 JSON file that lists `.monsterpack` archives hosted by their publishers.
It is not an archive and does not contain character artwork, sounds, or pack bytes.

Users add catalog HTTPS URLs in **Settings → Catalogs**. MonsterDialer does not maintain a
directory of community catalogs or add them automatically.

## Catalog format

The catalog endpoint must return one JSON object with `formatVersion` set to `1`. Unknown fields
are rejected so publishers can rely on a stable, reviewable contract.

```json
{
  "formatVersion": 1,
  "id": "org.example.forest-packs",
  "name": "Forest Packs",
  "publisher": "Example Studio",
  "website": "https://example.org",
  "updatedAt": "2026-09-20T00:00:00Z",
  "packs": [
    {
      "id": "org.example.mossling-friends",
      "version": "1.0.0",
      "name": "Mossling Friends",
      "creator": "Example Studio",
      "license": "CC BY 4.0",
      "description": "Original pixel-art characters.",
      "downloadUrl": "https://downloads.example.org/mossling-friends-1.0.0.monsterpack",
      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "sizeBytes": 1837421,
      "minAppVersion": "1.0.0"
    }
  ]
}
```

## Field requirements

| Field | Requirement |
| --- | --- |
| `id` | 2–64 characters matching `[a-z0-9][a-z0-9._-]{1,63}`. |
| `name` | Non-blank, at most 120 characters. |
| `publisher`, `website`, `updatedAt` | Optional catalog metadata. |
| `packs` | Up to 200 entries with distinct IDs. |
| `packs[].id` | The stable pack manifest ID. |
| `version` | Non-blank, at most 64 characters. |
| `license` | Non-blank, at most 120 characters; state the assets’ licence or permission. |
| `creator`, `description`, `minAppVersion` | Optional pack metadata. The description is at most 1,000 characters. |
| `downloadUrl` | Direct HTTPS URL to the `.monsterpack`, with no fragment or credentials. Redirects are not followed. |
| `sha256` | Required 64-character hexadecimal SHA-256 of the exact archive bytes. |
| `sizeBytes` | Required archive length from 1 byte through 24 MiB. |

MonsterDialer checks the URL, downloaded byte count, and SHA-256 before passing the archive to the
existing `.monsterpack` validator and installer. A catalog entry does not bypass any pack schema,
asset, or size checks.

## Publisher responsibilities

Only publish packs whose artwork, sounds, names, and other assets you are authorised to distribute.
Keep an archive URL immutable for a particular version: publish a new URL, version, size, and hash
when the archive changes. Catalogs cannot reference other catalogs, execute code, or supply remote
pack assets.
