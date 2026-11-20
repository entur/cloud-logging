#!/usr/bin/env python3
"""Prints one line per run written by run-agents.sh (time, tokens, tool calls, cost) and writes each answer to <run>.answer.

Usage: summarize.py <logs dir>

Tokens are input tokens including cached ones plus output tokens, as reported by the tool. Copilot reports them rounded
(for example 556.6k). Claude Code subagents (Agent tool) count differently from claude -p, so compare within one tool.
Cost in USD: Copilot AI credits / 100, Claude Code's total_cost_usd (API prices, also when running on a subscription).
"""
import glob
import json
import os
import re
import sys


def kilo(text):
    number = float(text.rstrip('kKmM'))
    return number * 1000 if text[-1] in 'kK' else number * 1_000_000 if text[-1] in 'mM' else number


def copilot(out):
    text = open(out, encoding='utf-8', errors='replace').read()
    tokens = cost = 0
    m = re.search(r'AI Credits\s+([\d.]+)', text)
    if m:
        cost = float(m.group(1)) / 100
    m = re.search(r'Tokens\s+↑\s*([\d.]+[kKmM]?).*?↓\s*([\d.]+[kKmM]?)', text)
    if m:
        tokens = kilo(m.group(1)) + kilo(m.group(2))
    calls = len(re.findall(r'^ [^\w\s!] \w', text, re.M))  # ' $ Shell', ' / Search', ' ● Read', ' ✗ Shell' (denied)
    denied = len(re.findall(r'^ ✗ ', text, re.M))
    return tokens, calls, denied, cost, copilot_answer(text)


def copilot_answer(text):
    """The text after the last tool call block (' $ Shell ...' up to a blank line) and before the usage summary."""
    answer, in_block = [], False
    for line in text.split('\nChanges ')[0].splitlines():
        if re.match(r'^ [^\w\s] ', line):
            answer, in_block = [], True
        elif in_block:
            in_block = line.strip() != ''
        else:
            answer.append(line)
    return '\n'.join(answer).strip()


def claude(out):
    tokens = calls = denied = cost = 0
    answer = ''
    for line in open(out, encoding='utf-8', errors='replace'):
        try:
            event = json.loads(line)
        except ValueError:
            continue
        if event.get('type') == 'assistant':
            calls += sum(1 for c in event['message'].get('content', []) if c.get('type') == 'tool_use')
        elif event.get('type') == 'result':
            usage = event.get('usage', {})
            tokens = sum(usage.get(k, 0) for k in ('input_tokens', 'cache_creation_input_tokens', 'cache_read_input_tokens', 'output_tokens'))
            denied = len(event.get('permission_denials', []))
            answer = event.get('result', '')
            cost = event.get('total_cost_usd', 0)
    return tokens, calls, denied, cost, answer


def main(logs):
    print(f'{"run":<44} {"exit":>4} {"seconds":>8} {"tokens":>8} {"tools":>5} {"denied":>6} {"cost $":>7}')
    for meta in sorted(glob.glob(os.path.join(logs, 'runs', '*.meta'))):
        base = meta[:-len('.meta')]
        name = os.path.basename(base)
        values = dict(l.strip().split('=', 1) for l in open(meta) if '=' in l)
        tool = name.split('-')[1]
        tokens, calls, denied, cost, answer = (claude if tool == 'claude' else copilot)(base + '.out')
        with open(base + '.answer', 'w', encoding='utf-8') as f:
            f.write(answer + '\n')
        print(f'{name:<44} {values.get("exit", "?"):>4} {float(values.get("seconds", 0)):>8.1f} {tokens / 1000:>7.1f}k {calls:>5} {denied:>6} {cost:>7.2f}')


if __name__ == '__main__':
    main(sys.argv[1])
