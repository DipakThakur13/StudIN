# Threat model (MASVS-aligned summary)

| Threat | Mitigation in code |
|---|---|
| Malicious deck import (huge files, CSV injection, control chars) | `CsvDeckParser`: 1 MB cap checked before reading, 5,000 rows, 1,000 chars/field, control-char stripping, formula-injection escaping on export, fuzz test in `DomainTests` |
| Exported-file path traversal | Exports are written only to `cacheDir/exports` with sanitised names and shared through `FileProvider` with read-only URI grants |
| Overlay abuse / trapping the user | Gate screen always offers **Back to studying** and **Emergency – open anyway**; emergency & system apps are never gated (`FocusPolicyEngine`) |
| Untrusted accessibility content | Bounded inspection (depth 25, 80 nodes, 2,000 chars), `TextNormalizer` caps tokens, classification is pure & local, raw titles are never persisted or logged |
| Model supply-chain compromise | In-app downloads are pinned to repository commits (Qwen2.5 1.5B `60257f9…`, Qwen3 4B `0c8dd54…`) and must match the SHA-256 and size published by Hugging Face, otherwise they are deleted; the old model is replaced only after the new one verifies. Imported files: extension & size validated, SHA-256 shown. Inference runs in-process |
| Credential leakage (HF token) | Removed in v2.3: the Qwen repositories are public, so builds bundle no token and downloads send no credentials. The downloader still drops any `Authorization` header on cross-host redirects (unit-tested) should a gated model be added later |
| GPU driver crash | LiteRT-LM GPU init errors fall back to the CPU. Native crashes can't be caught, so `GpuPolicy` persists PROBING before GPU init and OK only after a warm-up answer; a leftover PROBING at next start pins that model to the CPU (unit-tested). OpenCL libraries are declared `required="false"` |
| Insecure deep links / exported components | Only the launcher activity, boot receiver and the system-bound accessibility service are exported; gate activity and services are not |
| Log leakage | No titles or notes are logged; no analytics or crash SDKs |
| Data exfiltration | No code path sends user data; the only network client is `ModelDownloader` (GET of one pinned model file). ML Kit's data-transport backend discovery is removed from the manifest so library usage logs can't be uploaded |
| Denial of service / battery | Event throttling (1.5 s/package), gate cooldown (10 s), power-aware LLM (skips inference on battery saver < 20 %), one generation at a time |
