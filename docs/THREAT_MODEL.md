# Threat model (MASVS-aligned summary)

| Threat | Mitigation in code |
|---|---|
| Malicious deck import (huge files, CSV injection, control chars) | `CsvDeckParser`: 1 MB cap checked before reading, 5,000 rows, 1,000 chars/field, control-char stripping, formula-injection escaping on export, fuzz test in `DomainTests` |
| Exported-file path traversal | Exports are written only to `cacheDir/exports` with sanitised names and shared through `FileProvider` with read-only URI grants |
| Overlay abuse / trapping the user | Gate screen always offers **Back to studying** and **Emergency – open anyway**; emergency & system apps are never gated (`FocusPolicyEngine`) |
| Untrusted accessibility content | Bounded inspection (depth 25, 80 nodes, 2,000 chars), `TextNormalizer` caps tokens, classification is pure & local, raw titles are never persisted or logged |
| Model supply-chain compromise | Models are imported only by the user via the system picker, extension & size validated, copied to private storage, SHA-256 shown for verification; inference runs in-process without network access |
| Insecure deep links / exported components | Only the launcher activity, boot receiver and the system-bound accessibility service are exported; gate activity and services are not |
| Log leakage | No titles or notes are logged; no analytics or crash SDKs |
| Data exfiltration | App removes the `INTERNET` permission at manifest-merge time |
| Denial of service / battery | Event throttling (1.5 s/package), gate cooldown (10 s), power-aware LLM (skips inference on battery saver < 20 %), one generation at a time |
