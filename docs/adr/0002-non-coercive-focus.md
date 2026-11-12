# ADR 0002 – Non-coercive focus blocking

**Status:** accepted · **Date:** 2026-11-12

## Decision
* Focus state machine `IDLE → PREPARING → ACTIVE ⇄ PAUSED → COMPLETED/ABORTED`,
  persisted with `elapsedRealtime` anchors + boot count; reboot pauses the session.
* Rule precedence: emergency allowlist > user allow > session pass > app block >
  feed (Smart) rule > advisory.
* Detection: AccessibilityService when enabled (needed for Smart mode), otherwise a
  UsageStats poll from the foreground timer service. Overlay permission only improves
  how the gate appears; without it a notification is used.
* The gate offers active-recall questions (0–5, configurable, no penalty) for a short
  pass, a mindful pause when no cards exist, and an unconditional emergency exit.
* No Device Admin, no Settings interception, no uninstall prevention.
