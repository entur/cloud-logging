# AI mode benchmark

Measures how well AI agents answer questions about a test run from its logs, with and without [AI mode](../README.md#ai--tooling-compact-log-file).
The logs come from two example test suites (`examples/gcp-web-example`, `examples/gcp-grpc-spring-example`). Each agent
answers the same four questions in three setups:

| Setup | The agent gets |
|---|---|
| `plain` | the JUnit XML reports (log output in `<system-out>`), only told where they are |
| `careful` | the same reports plus a tested grep cookbook for them ([prompts/careful.txt](prompts/careful.txt)): the three console formats, a `sed`+`awk` normaliser to one event per line and recipes for each question |
| `ai` | the compact file and `.full` from an AI-mode run, plus the AGENTS.md snippet from [../README.md](../README.md), read at run time |

Questions (full wording in [prompts/](prompts)):

1. every distinct WARN and ERROR message with logger, total count and exception class,
2. HTTP requests (method and path) with a 4xx or 5xx response, with status and count,
3. how many times Tomcat and the gRPC server were started, and the gRPC port,
4. where the exceptions of ERROR events were thrown in application code.

Agents only get read-only shell commands (`grep wc head tail sort uniq cut tr ls sed awk echo printf cat nl paste test`),
and are told not to read source code.

## Running

Needs Java 25, Python 3 and the agent CLIs: [GitHub Copilot CLI](https://github.com/github/copilot-cli) and/or [Claude Code](https://claude.com/claude-code).

```
cd test/test-logback/ai-benchmark
./generate-logs.sh /tmp/ai-bench           # runs the two suites without and with AI mode, writes answer-key.txt
./run-agents.sh /tmp/ai-bench              # 3 setups x 4 agents in parallel, a few minutes
./summarize.py /tmp/ai-bench               # time, tokens and tool calls per run, writes <run>.answer
```

`run-agents.sh <logs dir> [setups] [agents]` takes the setups (default `"plain careful ai"`) and the agents as
`tool:model:effort` (default `"copilot:default:low claude:opus:low claude:sonnet:low claude:haiku:low"`), for example
`./run-agents.sh /tmp/ai-bench ai "claude:sonnet:high"`. `DRY_RUN=1` only writes the prompts to `<logs dir>/runs/*.prompt`.
A Copilot run uses about 15-35 AI credits.

Tokens are not comparable between tools: Copilot reports input including cached tokens, `claude -p` includes the full
Claude Code system prompt in every turn (about 4× the tokens of a Claude Code subagent on the same task). Compare
setups within one tool and model.

## Grading

Compare each `<logs dir>/runs/<run>.answer` with `<logs dir>/answer-key.txt` ([answer-key.py](answer-key.py) computes
it from `.full`, which has every event; the baselines run the same tests). Each question scores 0 to 1, 4 in total:

- Q1: share of messages with the right count. A missing or wrong exception class, or a message that is not in the key,
  costs half a message. Messages that differ only in a long tail (the two `MergedAnnotation` warnings) may be merged.
- Q2: share of the key's responses; half for a response with the wrong count or without the method. Responses only
  logged at DEBUG (listed under the key's Q2) are not required, but not wrong either.
- Q3: Tomcat starts, gRPC server starts and gRPC port, a third each.
- Q4: half for each exception with the right class and file:line.

## Results

[results-2026-11.md](results-2026-11.md): AI mode against both baselines, and the old against the new compact format.
