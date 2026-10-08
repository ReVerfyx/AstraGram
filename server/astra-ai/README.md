# Astra AI VPS

This is the small CPU-only backend expected by AstraGram #14.

Target host: Ubuntu 24.04, 2 vCPU, 4 GB RAM, 60 GB disk.

It installs:

- Ollama bound only to `127.0.0.1:11434`
- `qwen2.5:3b`
- 4 GB swap if the VPS has no swap
- nginx exposing only the Astra AI chat endpoint
- a publicly trusted short-lived TLS certificate for the server IP
- automatic daily certificate renewal
- basic per-IP rate limiting

Run on the VPS:

```bash
sudo SERVER_IP=2.26.85.86 bash server/astra-ai/install.sh
```

Health:

```bash
curl https://2.26.85.86/ai/health
```

AstraGram uses:

```text
https://2.26.85.86/ai/v1/chat/completions
```

The Ollama management API is intentionally not exposed directly to the internet.
