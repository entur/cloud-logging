# Results, November 2026

## Ten models, low and high effort

Logs from commit acdcc660 (AI mode with `<file>.compact`) and the same code without AI mode, generated with
`generate-logs.sh`; prompts from `run-agents.sh` (AI mode with the README snippet of that commit). 60 runs: GitHub Copilot
CLI 1.0.107 with seven models and Claude Code (`claude -p`) with three, each at low and high effort, one run per cell.
Graded with `grade.py`, every flagged item checked by hand. Cost is Copilot AI credits / 100 and Claude Code's
API-price estimate. Tokens include cached input; `claude -p` counts its system prompt in every turn, so Claude token
counts are not comparable with the subagent runs further down.

Each cell: score (max 4) / tokens / tool calls / time / cost.

| Tool | Model | Effort | Plain baseline | Careful baseline | AI mode |
|---|---|---|---|---|---|
| Copilot | Claude Sonnet 5.5 | low | 2.50 / 812k / 16 / 88 s / $0.32 | 3.39 / 362k / 15 / 54 s / $0.17 | 4.00 / 185k / 4 / 23 s / $0.10 |
| Copilot | GPT-6.1 Sol | low | 4.00 / 359k / 13 / 108 s / $0.17 | 4.00 / 390k / 21 / 99 s / $0.15 | 4.00 / 141k / 5 / 39 s / $0.08 |
| Copilot | Gemini 4 Argon | low | 3.89 / 2.9M / 58 / 229 s / $0.99 | 4.00 / 2.9M / 76 / 280 s / $0.98 | 4.00 / 784k / 22 / 84 s / $0.33 |
| Copilot | Grok 4.7 | low | 2.97 / 3.9M / 72 / 400 s / $2.32 | 4.00 / 2.0M / 52 / 258 s / $1.26 | 4.00 / 397k / 12 / 80 s / $0.30 |
| Copilot | GPT-6 Luna | low | 2.68 / 450k / 12 / 60 s / $0.01 | 0.00 / 144k / 6 / 24 s / $0.00 | 3.79 / 212k / 13 / 57 s / $0.01 |
| Copilot | Gemini 3.8 Flash | low | 2.92 / 14.9M / 134 / 541 s / $1.83 | 3.12 / 8.0M / 81 / 282 s / $1.01 | 3.95 / 3.8M / 45 / 152 s / $0.54 |
| Copilot | MAI-Code-1.1-Flash | low | 3.00 / 3.0M / 45 / 375 s / $0.14 | 2.63 / 2.7M / 43 / 329 s / $0.11 | 3.43 / 1.2M / 19 / 224 s / $0.07 |
| Claude Code | Claude Opus 5.5 | low | 3.67 / 683k / 28 / 120 s / $0.61 | 4.00 / 421k / 23 / 131 s / $0.59 | 4.00 / 130k / 6 / 45 s / $0.23 |
| Claude Code | Claude Sonnet 5.5 | low | 2.89 / 593k / 24 / 116 s / $0.33 | 3.32 / 245k / 13 / 59 s / $0.17 | 4.00 / 167k / 11 / 37 s / $0.14 |
| Claude Code | Claude Haiku 5.5 | low | 2.92 / 810k / 21 / 105 s / $0.03 | 3.50 / 538k / 26 / 122 s / $0.03 | 4.00 / 393k / 12 / 67 s / $0.02 |
| Copilot | Claude Sonnet 5.5 | high | 4.00 / 1.7M / 30 / 197 s / $0.62 | 4.00 / 718k / 17 / 94 s / $0.26 | 4.00 / 245k / 7 / 38 s / $0.14 |
| Copilot | GPT-6.1 Sol | high | 4.00 / 616k / 27 / 270 s / $0.34 | 4.00 / 308k / 30 / 173 s / $0.22 | 4.00 / 113k / 7 / 71 s / $0.11 |
| Copilot | Gemini 4 Argon | high | 4.00 / 4.4M / 47 / 392 s / $0.94 | 4.00 / 2.9M / 65 / 433 s / $0.91 | 4.00 / 539k / 29 / 133 s / $0.17 |
| Copilot | Grok 4.7 | high | 4.00 / 6.0M / 76 / 664 s / $3.55 | 4.00 / 4.4M / 68 / 610 s / $2.70 | 4.00 / 716k / 24 / 229 s / $0.60 |
| Copilot | GPT-6 Luna | high | 4.00 / 2.3M / 59 / 650 s / $0.07 | 4.00 / 2.4M / 88 / 597 s / $0.08 | 3.95 / 237k / 14 / 135 s / $0.01 |
| Copilot | Gemini 3.8 Flash | high | 4.00 / 20.6M / 191 / 1031 s / $2.44 | 4.00 / 16.7M / 144 / 671 s / $1.93 | 4.00 / 4.9M / 55 / 398 s / $0.65 |
| Copilot | MAI-Code-1.1-Flash | high | 0.00 / 15.8M / 242 / 2287 s / $0.84 (stopped) | 3.68 / 4.9M / 73 / 749 s / $0.31 | 3.46 / 2.9M / 40 / 461 s / $0.18 |
| Claude Code | Claude Opus 5.5 | high | 4.00 / 1.2M / 31 / 167 s / $1.04 | 4.00 / 531k / 27 / 146 s / $0.65 | 4.00 / 152k / 9 / 54 s / $0.25 |
| Claude Code | Claude Sonnet 5.5 | high | 4.00 / 1.7M / 47 / 240 s / $0.71 | 4.00 / 584k / 33 / 109 s / $0.32 | 4.00 / 234k / 15 / 49 s / $0.15 |
| Claude Code | Claude Haiku 5.5 | high | 4.00 / 4.8M / 61 / 455 s / $0.42 | 4.00 / 2.3M / 54 / 314 s / $0.07 | 4.00 / 955k / 18 / 126 s / $0.04 |

Totals: mean score, runs with 4.00, tokens, tool calls, time, cost.

| Runs | Plain baseline | Careful baseline | AI mode |
|---|---|---|---|
| All 20 runs per setup | 84 % (10/20 perfect), 87.7M tokens, 1234 calls, 142 min, $17.72 | 90 % (13/20 perfect), 53.5M tokens, 955 calls, 92 min, $11.92 | 98 % (15/20 perfect), 18.4M tokens, 367 calls, 42 min, $4.12 |
| Low effort | 79 % (1/10 perfect), 28.5M tokens, 423 calls, 36 min, $6.75 | 80 % (4/10 perfect), 17.8M tokens, 356 calls, 27 min, $4.47 | 98 % (7/10 perfect), 7.4M tokens, 149 calls, 13 min, $1.82 |
| High effort | 90 % (9/10 perfect), 59.2M tokens, 811 calls, 106 min, $10.97 | 99 % (9/10 perfect), 35.7M tokens, 599 calls, 65 min, $7.45 | 99 % (8/10 perfect), 11.0M tokens, 218 calls, 28 min, $2.30 |
| Strong models (Opus, Sonnet in both tools, GPT-6.1 Sol, Gemini 4 Argon, Grok 4.7) | 92 % (7/12 perfect), 24.9M tokens, 469 calls, 50 min, $11.94 | 97 % (10/12 perfect), 15.8M tokens, 440 calls, 41 min, $8.38 | 100 % (12/12 perfect), 3.8M tokens, 151 calls, 15 min, $2.60 |
| Cheap models (Haiku 5.5, GPT-6 Luna, Gemini 3.8 Flash, MAI-Code-1.1-Flash) | 74 % (3/8 perfect), 62.8M tokens, 765 calls, 92 min, $5.78 | 78 % (3/8 perfect), 37.7M tokens, 515 calls, 51 min, $3.54 | 96 % (3/8 perfect), 14.6M tokens, 216 calls, 27 min, $1.52 |

- AI mode was the most accurate setup (98 % against 90 % and 84 %) at a quarter to a third of the cost and time: $4.12
  and 42 minutes for its 20 runs, against $11.92 / 92 minutes (careful) and $17.72 / 142 minutes (plain), with 18M tokens
  against 54M and 88M.
- At low effort the gap is widest: 98 % against 80 % and 79 %. High effort lets the baselines catch up on accuracy (99 %
  careful, 90 % plain) but costs 3-5 times as much as AI mode at high effort.
- The strong models were perfect in all 12 AI-mode runs ($2.60 in total), against 10 of 12 with the careful baseline
  ($8.38) and 7 of 12 with the plain one ($11.94). GPT-6.1 Sol was perfect in every setup; for it AI mode meant 2-5 times
  fewer tokens and 2-4 times less time and cost.
- AI mode lifts the cheap models from 74-78 % to 96 %. Claude Haiku 5.5 was perfect in AI mode at $0.02-0.04 a run. The
  remaining AI-mode mistakes all come from the three cheapest Copilot models: miscounted or missing messages (a message
  counted across both loggers that log it, counts doubled, an ERROR variant left out) and, for MAI-Code-1.1-Flash, GET
  instead of POST.
- MAI-Code-1.1-Flash at high effort looped on the plain baseline (242 tool calls, 15.8M tokens) until stopped after 38
  minutes (scored 0); `run-agents.sh` now stops runs after 20 minutes. GPT-6 Luna at low effort gave up on the careful
  baseline after one denied command (0).
- The 403 on `POST /api/secured/endpoint`, rejected by Spring Security before request-response logging and only logged
  at DEBUG, was reported by 19 of 20 AI-mode runs (none in the earlier run, before the snippet pointed to `.full` for
  such details), 15 of 20 plain and 6 of 20 careful runs. It is not in the key, so it is not scored.
- Single runs are noisy: Copilot with Claude Sonnet 5.5 at low effort scored 3.50 on the plain baseline in the earlier
  run below and 2.50 here.

## Earlier run: four agents at low effort

Logs from commit 566bff8e (AI mode) and the same code without AI mode (JUnit XML reports). One run per agent and setup.
Copilot ran with the shell allowlist of `run-agents.sh`. The Claude agents ran as Claude Code subagents (Agent tool,
model and effort set per subagent) with the same prompts, not with `claude -p`; their token counts are the subagent totals.
The AI-mode prompt used the README snippet of 566bff8e; the snippet has since gained the per-logger count example and
the hint about DEBUG-only details, and the compact file has been renamed from `<file>` to `<file>.compact`.

### AI mode against the baselines

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

### Old against new compact format

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
