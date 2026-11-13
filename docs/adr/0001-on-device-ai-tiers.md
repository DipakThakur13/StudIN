# ADR 0001 – Tiered on-device AI

**Status:** accepted · **Date:** 2026-11-12

## Context
The roadmap requires syllabus-aware filtering and AI study help without uploading
screen content, on a wide range of Indian Android phones (2–12 GB RAM).

## Decision
* **Tier 0** – the student's own allow/block keyword lists.
* **Tier 1** – `ContentClassifier`: Unicode-normalised (Java `TextNormalizer`) n-gram
  lexicon with Hindi/Hinglish/Devanagari terms + weights generated from the student's
  syllabus topics. Logistic calibration; **BLOCK only when p(study) ≤ 0.08 with ≥ 2
  independent distraction signals**; otherwise WARN/UNKNOWN → allowed.
* **Lite AI** – extractive flashcards, keyword extraction, summarisation and BM25
  retrieval over the student's own cards (`LiteAi`). Works on every device.
* **Tier 2 (optional)** – an on-device LLM picked from the phone's RAM (v2.3): **Qwen3 4B**
  (`qwen3_4b_mixed_int4.litertlm`, thinking off) on phones reporting ≥ 7.0 GB, **Qwen2.5 1.5B
  Instruct** (`q8_ekv4096.litertlm`) on 3.3–7.0 GB, nothing below or on 32-bit. Runs on Google
  LiteRT-LM, GPU (OpenCL) first with crash-safe CPU fallback (`GpuPolicy`). Downloaded in-app
  from public `litert-community` repos (pinned revision + SHA-256, no token), or imported from a
  file; legacy Gemma 3 1B `.task` files still run on MediaPipe (CPU). Power-aware, used for tutor
  chat and card generation. Never on the accessibility hot path (latency budget).
  *Why not Gemma 3 1B any more:* in field tests it invented exam facts, links and code; the
  larger Qwen models score well above it on knowledge benchmarks, and the same app-side guard
  still applies to them.

## Consequences
Network access is used only for the optional model download (added in v2.1 at the product
owner's request). The LLM is not required for the MVP and any failure falls back to Lite AI.
