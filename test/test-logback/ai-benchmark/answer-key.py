#!/usr/bin/env python3
"""Prints the answer key for the benchmark questions, computed from the .full file of an AI-mode run.

Usage: answer-key.py <logs>/ai/test.full

The .full file has every event (DEBUG and above, no dedupe), each starting with a line
'HH:mm:ss.SSS LEVEL [thread] logger [mdc] #id : message', followed by message continuation lines and the stack trace.
The plain and careful baselines run the same tests, so the key applies to them too (check the totals once with the
careful baseline's grep recipes if the example tests change).
"""
import collections
import re
import sys

EVENT = re.compile(r'^\d\d:\d\d:\d\d\.\d{3} (\w+) +\[[^\]]*\] (\S+)(?: \[.*?\])? #[0-9a-f]{8,12} : (.*)$')
FRAME = re.compile(r'^\s+at (\S+)\(([^)]*)\)')
EXCEPTION = re.compile(r'^(?:Caused by: |Wrapped by: )?((?:[a-z_][\w$]*\.)+[A-Z][\w$]*)(?::|$)')
APP = re.compile(r'^(no\.entur\.grpc\.example|org\.entur\.example)\.')
RESPONSE = re.compile(r'^([45]\d\d) [A-Za-z ]+ https?://[^/\s]+(/\S*)')
REQUEST = re.compile(r'^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS) https?://[^/\s]+(/\S*)')


def events(path):
    """Yields (level, logger, lines) per event; lines[0] is the message part of the first line."""
    current = None
    with open(path, encoding='utf-8', errors='replace') as f:
        for raw in f:
            line = raw.rstrip('\n')
            m = EVENT.match(line)
            if m or line.startswith('---- JVM pid '):
                if current:
                    yield current
                current = (m.group(1), m.group(2), [m.group(3)]) if m else None
            elif current:
                current[2].append(line)
    if current:
        yield current


def split(lines):
    """Splits an event into message lines and stack trace lines (from the exception line before the first frame)."""
    first_frame = next((i for i, l in enumerate(lines) if FRAME.match(l)), None)
    if first_frame is None:
        return lines, []
    start = first_frame - 1
    while start > 0 and not EXCEPTION.match(lines[start]):
        start -= 1
    return lines[:start], lines[start:]


def normalize(message):
    message = re.sub(r'\b[uh]\d+(?:\([0-9a-f-]+\))?(?![\w(])', '<alias>', message)  # an alias, announced in .full as 'u7(<uuid>)'
    return re.sub(r'localhost:\d+', 'localhost:<port>', message)


def main(path):
    groups = collections.Counter()
    exceptions = {}
    throws = collections.Counter()
    responses = collections.Counter()
    methods = collections.defaultdict(set)
    debug_4xx = collections.Counter()
    tomcat = grpc = 0
    grpc_ports = set()

    for level, logger, lines in events(path):
        message_lines, trace = split(lines)
        message = next((l.strip() for l in message_lines if l.strip()), '')
        if level in ('WARN', 'ERROR'):
            key = (level, logger.rsplit('.', 1)[-1], normalize(message))
            groups[key] += 1
            if trace:
                exceptions[key] = EXCEPTION.match(trace[0]).group(1)
                if level == 'ERROR':
                    headers = [i for i, l in enumerate(trace) if EXCEPTION.match(l) and not FRAME.match(l)]
                    root = headers[-1]
                    frame = next((FRAME.match(l) for l in trace[root + 1:] if FRAME.match(l) and APP.match(FRAME.match(l).group(1))), None)
                    where = frame.group(2) if frame else '(no application frame)'
                    throws[(trace[root].replace('Caused by: ', '')[:120], where)] += 1
        m = RESPONSE.match(message)
        if m:
            responses[(m.group(2), m.group(1))] += 1
        m = REQUEST.match(message)
        if m:
            methods[m.group(2)].add(m.group(1))
        if level == 'DEBUG' and re.search(r'Response 4\d\d|Rejecting access', message):
            debug_4xx[(logger.rsplit('.', 1)[-1], normalize(message)[:100])] += 1
        if 'Tomcat started on port' in message:
            tomcat += 1
        if 'gRPC Server started' in message:
            grpc += 1
            grpc_ports.update(re.findall(r'port: (\d+)', message))

    print(f'Q1: {len(groups)} distinct WARN/ERROR messages (level | logger | message | count | exception)')
    for (level, logger, message), count in sorted(groups.items(), key=lambda e: (-e[1], e[0])):
        shown = message if len(message) <= 300 else message[:300] + ' ...'
        print(f'  {level} | {logger} | {shown} | {count} | {exceptions.get((level, logger, message), "-")}')
    print(f'Q2: {len(responses)} HTTP 4xx/5xx responses (method path -> status x count)')
    for (path_, status), count in sorted(responses.items()):
        print(f'  {"/".join(sorted(methods[path_])) or "?"} {path_} -> {status} x{count}')
    print('  not required, only at DEBUG (rejected before request-response logging):')
    for (logger, message), count in sorted(debug_4xx.items()):
        print(f'    {logger}: {message} x{count}')
    print(f'Q3: Tomcat started {tomcat} times, gRPC server started {grpc} times, gRPC port {", ".join(sorted(grpc_ports))}')
    print('Q4: ERROR stack traces: root cause | thrown at (first application frame) | events')
    for (exception, where), count in sorted(throws.items()):
        print(f'  {exception} | {where} | {count}')


if __name__ == '__main__':
    main(sys.argv[1])
