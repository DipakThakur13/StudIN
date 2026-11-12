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
* **Tier 2 (optional)** – MediaPipe LLM Inference with Gemma 3 1B IT int4 `.task`,
  downloaded in-app from `litert-community/Gemma3-1B-IT` (pinned revision + SHA-256; gated
  repo, so the user supplies a Hugging Face read token after accepting the Gemma licence), or
  imported from a file. Only on 64-bit devices with
  ≥ 4 GB RAM, power-aware, used for tutor chat, card generation and UNKNOWN titles in
  the content checker. Never on the accessibility hot path (latency budget).

## Consequences
Network access is used only for the optional model download (added in v2.1 at the product
owner's request). The LLM is not required for the MVP and any failure falls back to Lite AI.
