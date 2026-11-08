# Results, November 2026

Logs from commit 566bff8e (AI mode) and the same code without AI mode (JUnit XML reports). One run per agent and setup.
Copilot ran with the shell allowlist of `run-agents.sh`. The Claude agents ran as Claude Code subagents (Agent tool,
model and effort set per subagent) with the same prompts, not with `claude -p`; their token counts are the subagent totals.
The AI-mode prompt used the README snippet of 566bff8e; the snippet has since gained the per-logger count example and
the hint about DEBUG-only details, and the compact file has been renamed from `<file>` to `<file>.compact`.

## AI mode against the baselines

Each cell: score (max 4) / tokens / tool calls / time.

| Tool | Model | Effort | Plain baseline | Careful baseline | AI mode |
|---|---|---|---|---|---|
| GitHub Copilot CLI | Claude Sonnet 5.5 | low | 3.50 / 776k / 17 / 91 s | 3.50 / 557k / 14 / 68 s | 4.00 / 185k / 4 / 30 s |
| Claude Code subagent | Claude Opus 5.5 | low | 2.89 / 50k / 11 / 68 s | 4.00 / 41k / 3 / 24 s | 4.00 / 46k / 5 / 26 s |
| Claude Code subagent | Claude Sonnet 5.5 | low | 2.97 / 67k / 14 / 100 s | 3.47 / 52k / 3 / 29 s | 4.00 / 49k / 10 / 45 s |
| Claude Code subagent | Claude Haiku 5.5 | low | 3.42 / 116k / 26 / 170 s | 4.00 / 72k / 16 / 101 s | 4.00 / 77k / 11 / 52 s |
| **Total** | | | **80 %**, 1008k tokens, 68 calls, 429 s | **94 %**, 721k tokens, 36 calls, 222 s | **100 %**, 357k tokens, 30 calls, 154 s |

- Console output of the test run: 98.7 KB without AI mode (coloured JSON from shutdown logging), 3.7 KB with it.
- Logs to search: 8.2 MB of XML reports mixing pretty-printed JSON, classic text with left-truncated logger names and
  single-line JSON, against a 427 KB compact file (plus 7 MB `.full`).

Where points were lost:

| Tool / model | Setup | Score | Mistakes |
|---|---|---|---|
| Copilot / Sonnet | plain | 3.50 | Q2: every count 1 instead of 3 |
| Copilot / Sonnet | careful | 3.50 | Q2: HTTP methods not extracted (they are on the request line, the recipe lists responses) |
| Opus | plain | 2.89 | Q1 9/19: took the pretty JSON for a repeat of the single-line JSON; Q2 2.5/6: counts 1-2 instead of 3, 403 on `authorizationDenied` not found |
| Sonnet | plain | 2.97 | Q1 9/19 (1006 vs 1012, "about 8" vs 20, 3 vs 5, ...); Q2 3/6: counts 1 instead of 3, assumed each event is printed in several formats |
| Sonnet | careful | 3.47 | Q1: exception class of the protobuf WARN missing; Q2: method not given, guessed GET |
| Haiku | plain | 3.42 | Q1 8/19 (1009 vs 1012, 4 vs 5, 11 vs 20, ...) |

All AI-mode runs scored 4.00. Three of four plain-baseline agents (and the careful Haiku run) also reported a 403 on
`POST /api/secured/endpoint`, rejected by Spring Security before request-response logging and only logged at DEBUG; no
AI-mode run looked for it in `.full`. The README snippet now points to `.full` for such details.

## Old against new compact format

Earlier comparison of the first compact format (e0a16587: full logger names, all headers, MDC keys unshortened) with the
current one (37a42477 plus the port lines kept), same four questions, low and high effort. Score (max 4) / tokens / tool calls / time.

| Tool | Model | Effort | Old format | New format |
|---|---|---|---|---|
| GitHub Copilot CLI | Claude Sonnet 5.5 | low | 3.84 / 231k / 9 / 42 s | 4.00 / 420k / 15 / 60 s |
| GitHub Copilot CLI | Claude Sonnet 5.5 | high | 4.00 / 828k / 33 / 95 s | 4.00 / 646k / 30 / 77 s |
| Claude Code subagent | Claude Opus 5.5 | low | 4.00 / 46k / 5 / 29 s | 4.00 / 50k / 8 / 41 s |
| Claude Code subagent | Claude Opus 5.5 | high | 4.00 / 57k / 10 / 59 s | 4.00 / 59k / 10 / 60 s |
| Claude Code subagent | Claude Sonnet 5.5 | low | 4.00 / 53k / 6 / 42 s | 3.89 / 51k / 5 / 32 s |
| Claude Code subagent | Claude Sonnet 5.5 | high | 4.00 / 52k / 9 / 51 s | 4.00 / 53k / 8 / 48 s |
| Claude Code subagent | Claude Haiku 5.5 | low | 4.00 / 84k / 8 / 77 s | 3.42 / 73k / 9 / 52 s |
| Claude Code subagent | Claude Haiku 5.5 | high | 4.00 / 108k / 14 / 142 s | 4.00 / 84k / 17 / 131 s |
| **Total** | | | **31.84**, 1459k tokens, 94 calls, 537 s | **31.31**, 1435k tokens, 102 calls, 501 s |

No measurable difference. All three wrong answers were miscounted `repeated N more times` lines, which both formats
have (Copilot low/old: 7 and 5 instead of 5 and 4; Sonnet low/new: 8 instead of 5; Haiku low/new: added N+1 per line).
After the summary line was changed to `repeated N more times, N+1 in total` and the snippet explained the counting,
Sonnet and Haiku scored 4.00 at low effort. Copilot dropped to 3.68 (852k tokens, 10 of 29 commands denied) until
`sed`, `awk` and a few other read-only commands were allowed: then 4.00 with 185k tokens.
