# Building AstraGram

AstraGram currently tracks Telegram Android 12.10.6 at upstream commit `f2908b14133bbffbf7ab04f641ecb5bfaf533242`.

## Prepare the source

```bash
./scripts/bootstrap-telegram.sh
cd vendor/telegram
```

The bootstrap fetches Telegram's required submodules, applies the AstraGram patches and overlay, renames the Android application to AstraGram, and uses the application id `com.reverfyx.astragram`.

## Required local configuration

Before producing a distributable APK, configure your own Telegram `api_id` / `api_hash`, signing key, and push-service configuration as required by the official Telegram Android build instructions. Do not publish an APK using Telegram's dummy build credentials.

The designer app icon is intentionally not included yet.
