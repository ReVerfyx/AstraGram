# AstraGram architecture

AstraGram is developed as a small overlay on top of the official Telegram Android source instead of maintaining a large copied codebase.

The overlay is split into four areas:

- `astra`: app settings and feature flags.
- `astra/plugins`: the `.asplug` package format, validation, permissions, and later the runtime sandbox.
- `astra/ai`: provider-independent AI interfaces and OpenAI-compatible transport.
- `astra/automation`: execution code that applies an AI-generated profile plan directly after the user has granted the required permission.

Animations are enabled by default and can be disabled from Astra settings. Account capacity is currently raised to 32 in both Java and native Telegram networking. This is a practical high-capacity implementation, not a claim of truly unbounded accounts; removing the static account arrays requires a larger upstream refactor.

The app icon is intentionally not included in the overlay so the final designer assets can be added without replacing temporary artwork.
