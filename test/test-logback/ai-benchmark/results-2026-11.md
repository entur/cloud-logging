# Results, November 2026

## Ten models, low and high effort

Baselines: JUnit XML reports from commit acdcc660 without AI mode. AI mode: logs and README snippet from commit 39f55f85
(after the code review fixes in #535, which render request-response events from the standard marker), re-run on
2026-11-09; the fixes do not change the output without AI mode, and the answer key computed from the new logs is
identical. Logs generated with `generate-logs.sh`, prompts from `run-agents.sh`.
60 runs: GitHub Copilot CLI 1.0.107 with seven models and Claude Code (`claude -p`) with three, each at low and high
effort, one run per cell. Graded with `grade.py`, every flagged item checked by hand. Cost is Copilot AI credits / 100 and
Claude Code's API-price estimate. The two stopped runs (MAI-Code-1.1-Flash at high effort: the plain baseline, stopped by
hand after 38 minutes, and AI mode, stopped by the 20-minute limit of the time) were re-run on 2026-11-09 with a
60-minute limit and the memory of each run limited to 4 GB (Copilot CLI 1.0.108); AI mode answered after 19 minutes,
the plain baseline after 31. An earlier re-run, spoilt by a runaway command (below), is not used. Tokens include
cached input; `claude -p` counts its system prompt in every turn, so Claude token counts are not comparable with the
subagent runs further down.

Each cell: score (max 4) / tokens / tool calls / time / cost.

| Tool | Model | Effort | Plain baseline | Careful baseline | AI mode |
|---|---|---|---|---|---|
| Copilot | Claude Sonnet 5.5 | low | 2.50 / 812k / 16 / 88 s / $0.32 | 3.39 / 362k / 15 / 54 s / $0.17 | 4.00 / 291k / 7 / 37 s / $0.18 |
| Copilot | GPT-6.1 Sol | low | 4.00 / 359k / 13 / 108 s / $0.17 | 4.00 / 390k / 21 / 99 s / $0.15 | 4.00 / 140k / 5 / 39 s / $0.10 |
| Copilot | Gemini 4 Argon | low | 3.89 / 2.9M / 58 / 229 s / $0.99 | 4.00 / 2.9M / 76 / 280 s / $0.98 | 4.00 / 812k / 30 / 165 s / $0.31 |
| Copilot | Grok 4.7 | low | 2.97 / 3.9M / 72 / 400 s / $2.32 | 4.00 / 2.0M / 52 / 258 s / $1.26 | 4.00 / 237k / 10 / 101 s / $0.22 |
| Copilot | GPT-6 Luna | low | 2.68 / 450k / 12 / 60 s / $0.01 | 0.00 / 144k / 6 / 24 s / $0.00 | 3.79 / 277k / 9 / 51 s / $0.01 |
| Copilot | Gemini 3.8 Flash | low | 2.92 / 14.9M / 134 / 541 s / $1.83 | 3.12 / 8.0M / 81 / 282 s / $1.01 | 4.00 / 3.2M / 40 / 156 s / $0.53 |
| Copilot | MAI-Code-1.1-Flash | low | 3.00 / 3.0M / 45 / 375 s / $0.14 | 3.13 / 2.7M / 43 / 329 s / $0.11 | 3.84 / 1.6M / 23 / 217 s / $0.07 |
| Claude Code | Claude Opus 5.5 | low | 3.67 / 683k / 28 / 120 s / $0.61 | 4.00 / 421k / 23 / 131 s / $0.59 | 4.00 / 184k / 8 / 42 s / $0.28 |
| Claude Code | Claude Sonnet 5.5 | low | 2.89 / 593k / 24 / 116 s / $0.33 | 3.32 / 245k / 13 / 59 s / $0.17 | 4.00 / 172k / 11 / 41 s / $0.13 |
| Claude Code | Claude Haiku 5.5 | low | 2.92 / 810k / 21 / 105 s / $0.03 | 3.50 / 538k / 26 / 122 s / $0.03 | 4.00 / 264k / 8 / 46 s / $0.01 |
| Copilot | Claude Sonnet 5.5 | high | 4.00 / 1.7M / 30 / 197 s / $0.62 | 4.00 / 718k / 17 / 94 s / $0.26 | 4.00 / 342k / 9 / 43 s / $0.21 |
| Copilot | GPT-6.1 Sol | high | 4.00 / 616k / 27 / 270 s / $0.34 | 4.00 / 308k / 30 / 173 s / $0.22 | 4.00 / 136k / 8 / 71 s / $0.13 |
| Copilot | Gemini 4 Argon | high | 4.00 / 4.4M / 47 / 392 s / $0.94 | 4.00 / 2.9M / 65 / 433 s / $0.91 | 4.00 / 300k / 12 / 135 s / $0.23 |
| Copilot | Grok 4.7 | high | 4.00 / 6.0M / 76 / 664 s / $3.55 | 4.00 / 4.4M / 68 / 610 s / $2.70 | 4.00 / 629k / 24 / 286 s / $0.54 |
| Copilot | GPT-6 Luna | high | 4.00 / 2.3M / 59 / 650 s / $0.07 | 4.00 / 2.4M / 88 / 597 s / $0.08 | 4.00 / 260k / 8 / 178 s / $0.02 |
| Copilot | Gemini 3.8 Flash | high | 4.00 / 20.6M / 191 / 1031 s / $2.44 | 4.00 / 16.7M / 144 / 671 s / $1.93 | 4.00 / 6.3M / 67 / 410 s / $0.86 |
| Copilot | MAI-Code-1.1-Flash | high | 2.79 / 15.0M / 225 / 1887 s / $0.76 | 3.68 / 4.9M / 73 / 749 s / $0.31 | 3.68 / 7.4M / 110 / 1124 s / $0.43 |
| Claude Code | Claude Opus 5.5 | high | 4.00 / 1.2M / 31 / 167 s / $1.04 | 4.00 / 531k / 27 / 146 s / $0.65 | 4.00 / 280k / 12 / 60 s / $0.33 |
| Claude Code | Claude Sonnet 5.5 | high | 4.00 / 1.7M / 47 / 240 s / $0.71 | 4.00 / 584k / 33 / 109 s / $0.32 | 4.00 / 318k / 17 / 51 s / $0.18 |
| Claude Code | Claude Haiku 5.5 | high | 4.00 / 4.8M / 61 / 455 s / $0.42 | 4.00 / 2.3M / 54 / 314 s / $0.07 | 4.00 / 498k / 15 / 82 s / $0.02 |

Totals: mean score, runs with 4.00, tokens, tool calls, time, cost.

| Runs | Plain baseline | Careful baseline | AI mode |
|---|---|---|---|
| All 20 runs per setup | 88 % (10/20 perfect), 86.8M tokens, 1217 calls, 135 min, $17.64 | 90 % (13/20 perfect), 53.5M tokens, 955 calls, 92 min, $11.92 | 99 % (17/20 perfect), 23.7M tokens, 433 calls, 56 min, $4.79 |
| Low effort | 79 % (1/10 perfect), 28.5M tokens, 423 calls, 36 min, $6.75 | 81 % (4/10 perfect), 17.8M tokens, 356 calls, 27 min, $4.47 | 99 % (8/10 perfect), 7.2M tokens, 151 calls, 15 min, $1.84 |
| High effort | 97 % (9/10 perfect), 58.3M tokens, 794 calls, 99 min, $10.89 | 99 % (9/10 perfect), 35.7M tokens, 599 calls, 65 min, $7.45 | 99 % (9/10 perfect), 16.5M tokens, 282 calls, 41 min, $2.95 |
| Strong models (Opus, Sonnet in both tools, GPT-6.1 Sol, Gemini 4 Argon, Grok 4.7) | 92 % (7/12 perfect), 24.9M tokens, 469 calls, 50 min, $11.94 | 97 % (10/12 perfect), 15.8M tokens, 440 calls, 41 min, $8.38 | 100 % (12/12 perfect), 3.8M tokens, 153 calls, 18 min, $2.84 |
| Cheap models (Haiku 5.5, GPT-6 Luna, Gemini 3.8 Flash, MAI-Code-1.1-Flash) | 82 % (3/8 perfect), 62.0M tokens, 748 calls, 85 min, $5.70 | 79 % (3/8 perfect), 37.7M tokens, 515 calls, 51 min, $3.54 | 98 % (5/8 perfect), 19.9M tokens, 280 calls, 38 min, $1.95 |

- AI mode was the most accurate setup (99 % against 90 % careful and 88 % plain) at 40 % of the cost of the careful
  baseline and 27 % of the plain one ($4.79 for its 20 runs, against $11.92 and $17.64), in 56 against 92 and 135
  minutes, with 24M tokens against 54M and 87M.
- At low effort the gap is widest: 99 % against 81 % and 79 %. At high effort the careful baseline is as accurate (99 %)
  but uses 2.2 times the tokens and 2.5 times the cost of AI mode; the plain baseline scored 97 % at 3.7 times the cost.
- The strong models were perfect in all 12 AI-mode runs ($2.84 in total), against 10 of 12 with the careful baseline
  ($8.38) and 7 of 12 with the plain one ($11.94). GPT-6.1 Sol was perfect in every setup; for it AI mode meant 2-5 times
  fewer tokens, 2-4 times less time and 1.5-2.6 times less cost.
- AI mode lifts the cheap models from 79-82 % to 98 %. Claude Haiku 5.5 was perfect in AI mode at $0.01-0.02 a run, and
  so were Gemini 3.8 Flash at both efforts and GPT-6 Luna at high effort. The remaining AI-mode mistakes all come from
  the two cheapest Copilot models: GPT-6 Luna and MAI-Code-1.1-Flash at low effort added the INFO events with the same
  text to three WARN counts ("Hello full logging" 8 instead of 4), GPT-6 Luna counted 3 instead of 4 gRPC "Unknown
  exception" errors, and MAI-Code-1.1-Flash at high effort miscounted three messages (7 instead of 5, 5 instead of 4)
  and left out one of the two 404s on `/api/document/some/error`.
- MAI-Code-1.1-Flash at high effort is slow. Its first runs here were stopped: in AI mode by the 20-minute limit (111
  tool calls, 7.4M tokens, no answer), on the plain baseline by hand after 38 minutes (242 tool calls, 15.8M tokens). A
  first re-run with a 60-minute limit was spoilt: in AI mode the agent tested a regex with an awk loop up to `length(a)`
  that read `a[i]`, which adds an element each time, so the loop never ended; it grew to 55 GB until the kernel killed
  it, with the machine swapping under both runs. `run-agents.sh` now limits the memory of each run (`MEMORY_MAX`,
  default 4G) and stops runs after 60 minutes instead of 20. Re-run that way, both answered: 3.68 in AI mode after 19
  minutes (110 tool calls, 7.4M tokens; 3.46 and 3.57 in the two earlier AI-mode runs) and 2.79 on the plain baseline
  after 31 minutes (225 tool calls, 15.0M tokens). GPT-6 Luna at low effort gave up on the careful baseline after one
  denied command (0).
- The 403 on `POST /api/secured/endpoint`, rejected by Spring Security before request-response logging and only logged
  at DEBUG, was reported by 18 of 20 AI-mode runs (18 and 19 of 20 in the two earlier AI-mode runs, none in the earlier
  run below, before the snippet pointed to `.full` for such details), 16 of 20 plain and 6 of 20 careful runs. It is
  not in the key, so it is not scored.
- Single runs are noisy: Copilot with Claude Sonnet 5.5 at low effort scored 3.50 on the plain baseline in the earlier
  run below and 2.50 here; GPT-6 Luna at low effort scored 3.79, 3.21 and 3.79 in the three AI-mode runs, and Gemini 3.8
  Flash used 1.7-3.8M tokens at low effort and 3.3-6.3M at high effort in them.
- Grader changes, each checked against every graded run:
  - `grade.py` reads Q2 answers listing several requests on one line and counts written as `x3`. This raised the careful
    MAI-Code-1.1-Flash run at low effort from 2.63 to 3.13 (its answer was right) and Grok 4.7 at low effort in the
    9b0a3424 AI-mode run from 3.50 to 4.00, and lowered GPT-6 Luna low there from 3.30 to 3.21 (GET instead of POST now
    attributed to the right requests).
  - `grade.py` finds the question headers when an answer numbers the items of each section from 1 again, or when the Q1
    header names the format (`1. WARN/ERROR messages (LEVEL | logger | ...)`), and applies a method given once in the Q2
    header (`all POST`) to every request. Of the 100 graded runs this changed only two, both in the new AI-mode run and
    both right: Claude Haiku 5.5 low from 2.50 to 4.00, Copilot with Claude Sonnet 5.5 low from 3.50 to 4.00.
  - `answer-key.py` reads an alias announced in `.full` (`u7(<uuid>)`, new in 27bbbf73) as an alias; otherwise the
    generated security password, logged once per Spring context, would be 19 different messages.

### AI mode across the review fixes

The first AI-mode run used the logs of acdcc660. The first review fixes in #535 (9b0a3424) add the gRPC MDC
(`requestId`, `correlationId`) to gRPC events in AI mode, as on the console, so the compact file grows from 428 KB to
546 KB with the same 3512 lines. The code review fixes (27bbbf73, measured at 39f55f85) render request-response events
from the standard request-response marker instead of a separate AI-mode sink: the compact file shrinks to 518 KB with the
same lines, as gRPC headers repeating the MDC (`x-correlation-id`) are left out and gRPC error details are shown as JSON.
All three runs graded with the current `grade.py`. Score / tokens / tool calls / time / cost.

| Tool | Model | Effort | acdcc660 | 9b0a3424 | 39f55f85 |
|---|---|---|---|---|---|
| Copilot | Claude Sonnet 5.5 | low | 4.00 / 185k / 4 / 23 s / $0.10 | 4.00 / 276k / 6 / 34 s / $0.17 | 4.00 / 291k / 7 / 37 s / $0.18 |
| Copilot | GPT-6.1 Sol | low | 4.00 / 141k / 5 / 39 s / $0.08 | 4.00 / 135k / 5 / 53 s / $0.10 | 4.00 / 140k / 5 / 39 s / $0.10 |
| Copilot | Gemini 4 Argon | low | 4.00 / 784k / 22 / 84 s / $0.33 | 4.00 / 686k / 14 / 72 s / $0.27 | 4.00 / 812k / 30 / 165 s / $0.31 |
| Copilot | Grok 4.7 | low | 4.00 / 397k / 12 / 80 s / $0.30 | 4.00 / 377k / 18 / 116 s / $0.39 | 4.00 / 237k / 10 / 101 s / $0.22 |
| Copilot | GPT-6 Luna | low | 3.79 / 212k / 13 / 57 s / $0.01 | 3.21 / 245k / 9 / 44 s / $0.01 | 3.79 / 277k / 9 / 51 s / $0.01 |
| Copilot | Gemini 3.8 Flash | low | 3.95 / 3.8M / 45 / 152 s / $0.54 | 4.00 / 1.7M / 23 / 87 s / $0.24 | 4.00 / 3.2M / 40 / 156 s / $0.53 |
| Copilot | MAI-Code-1.1-Flash | low | 3.43 / 1.2M / 19 / 224 s / $0.07 | 3.41 / 1.3M / 23 / 169 s / $0.07 | 3.84 / 1.6M / 23 / 217 s / $0.07 |
| Claude Code | Claude Opus 5.5 | low | 4.00 / 130k / 6 / 45 s / $0.23 | 4.00 / 112k / 5 / 28 s / $0.20 | 4.00 / 184k / 8 / 42 s / $0.28 |
| Claude Code | Claude Sonnet 5.5 | low | 4.00 / 167k / 11 / 37 s / $0.14 | 4.00 / 225k / 11 / 45 s / $0.18 | 4.00 / 172k / 11 / 41 s / $0.13 |
| Claude Code | Claude Haiku 5.5 | low | 4.00 / 393k / 12 / 67 s / $0.02 | 4.00 / 187k / 7 / 48 s / $0.01 | 4.00 / 264k / 8 / 46 s / $0.01 |
| Copilot | Claude Sonnet 5.5 | high | 4.00 / 245k / 7 / 38 s / $0.14 | 4.00 / 353k / 8 / 36 s / $0.17 | 4.00 / 342k / 9 / 43 s / $0.21 |
| Copilot | GPT-6.1 Sol | high | 4.00 / 113k / 7 / 71 s / $0.11 | 4.00 / 127k / 9 / 85 s / $0.12 | 4.00 / 136k / 8 / 71 s / $0.13 |
| Copilot | Gemini 4 Argon | high | 4.00 / 539k / 29 / 133 s / $0.17 | 4.00 / 1.3M / 54 / 251 s / $0.48 | 4.00 / 300k / 12 / 135 s / $0.23 |
| Copilot | Grok 4.7 | high | 4.00 / 716k / 24 / 229 s / $0.60 | 4.00 / 649k / 20 / 307 s / $0.61 | 4.00 / 629k / 24 / 286 s / $0.54 |
| Copilot | GPT-6 Luna | high | 3.95 / 237k / 14 / 135 s / $0.01 | 4.00 / 319k / 8 / 138 s / $0.02 | 4.00 / 260k / 8 / 178 s / $0.02 |
| Copilot | Gemini 3.8 Flash | high | 4.00 / 4.9M / 55 / 398 s / $0.65 | 4.00 / 3.3M / 36 / 322 s / $0.41 | 4.00 / 6.3M / 67 / 410 s / $0.86 |
| Copilot | MAI-Code-1.1-Flash | high | 3.46 / 2.9M / 40 / 461 s / $0.18 | 3.57 / 3.2M / 49 / 602 s / $0.24 | 3.68 / 7.4M / 110 / 1124 s / $0.43 (re-run) |
| Claude Code | Claude Opus 5.5 | high | 4.00 / 152k / 9 / 54 s / $0.25 | 4.00 / 282k / 17 / 71 s / $0.40 | 4.00 / 280k / 12 / 60 s / $0.33 |
| Claude Code | Claude Sonnet 5.5 | high | 4.00 / 234k / 15 / 49 s / $0.15 | 4.00 / 207k / 11 / 48 s / $0.13 | 4.00 / 318k / 17 / 51 s / $0.18 |
| Claude Code | Claude Haiku 5.5 | high | 4.00 / 955k / 18 / 126 s / $0.04 | 4.00 / 598k / 43 / 138 s / $0.03 | 4.00 / 498k / 15 / 82 s / $0.02 |
| **Total** | | | 98 % (15/20 perfect), 18.4M tokens, 367 calls, 42 min, $4.12 | 98 % (17/20 perfect), 15.6M tokens, 376 calls, 45 min, $4.25 | 99 % (17/20 perfect), 23.7M tokens, 433 calls, 56 min, $4.79 |

The three runs scored 98 %, 98 % and 99 % with 18.4M, 15.6M and 23.7M tokens at $4.12, $4.25 and $4.79. Most of the
token difference is two cheap models: Gemini 3.8 Flash used 5.0M tokens for both efforts at 9b0a3424 and 9.5M at
39f55f85, MAI-Code-1.1-Flash at high effort 3.2M and 7.4M. The changes made no measurable difference to the agents; the
differences per run are of the size seen between repeated runs. MAI-Code-1.1-Flash at high effort at 39f55f85 is the
memory-limited re-run described above.

## Formatting cost per event

`formatting-benchmark.sh`: time and allocation per event for the AI files (compact and full line) against the JSON encoder of
the machine-readable output; writing to the files is not included. JDK 25.0.4.1 on an AMD Ryzen 9 9950X, median of three runs
(the run before bb7e3868 is a single run of the same benchmark).

| Event | JSON encoder | AI files before bb7e3868 | AI files at bb7e3868 |
|---|---|---|---|
| Plain message with MDC | 0.7 µs, 0.7 KB | 1.9 µs, 5.8 KB | 1.3 µs, 4.5 KB |
| HTTP response, 1 KB JSON body | 1.7 µs, 5.8 KB | 24.0 µs, 47.8 KB | 9.9 µs, 37.1 KB |
| HTTP response, 100 KB JSON body | 47 µs, 323 KB | 1,603 µs, 2,017 KB | 465 µs, 1,511 KB |
| ERROR with a 3-exception trace | 17.6 µs, 145 KB | 23.7 µs, 170 KB | 22.1 µs, 169 KB |

- Before bb7e3868 a profile of the benchmark (Java Flight Recorder) put half of the AI time of request-response events in a
  UUID regular expression: every hex digit of a JSON body starts a match attempt. bb7e3868 finds UUIDs and hex ids by hand,
  from each `-`, and no longer parses a JSON body a second time when it equals the one in the event's JSON output.
- The AI files still cost more than the JSON encoder, mostly for request-response events: aliasing, the single-line
  conversion of the body and a JSON round trip (the event's structured data is written as JSON and read back for the headers
  and the body). For large bodies most of what is left is that round trip.
- In test runs this does not show. The formatting runs on the async appender's thread, not on the test threads. Recordings of
  the two example suites (test JVMs only) put all logging code at 2.4 % of CPU samples and 2.1 % of allocations without AI
  mode, and at 3.2 % and 1.8 % with AI mode, and the suites took 23.8 and 23.7 s with AI mode against 24.0 and 23.9 s
  without (JUnit test time 21.4/21.3 s against 21.5/21.4 s); the console output drops from 95 KB to 0.3 KB.

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
