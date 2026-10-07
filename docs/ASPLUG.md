# .asplug

AstraGram plugins use the `.asplug` extension.

An `.asplug` file is a ZIP container with a required `manifest.json`. The first runtime version reserves `main.js` as the default entry point.

Example:

```json
{
  "id": "dev.example.cleaner",
  "name": "Cleaner",
  "version": "1.0.0",
  "entrypoint": "main.js",
  "permissions": [
    "ui",
    "messages.read"
  ]
}
```

Supported permission names in the initial API are:

- `ui`
- `network`
- `files.read`
- `files.write`
- `messages.read`
- `messages.send`
- `profile.read`
- `profile.write`
- `automation`

Installing a plugin never grants its requested permissions automatically. The package loader rejects path traversal, oversized manifests, and malformed permission names before a plugin reaches the runtime.
