#!/usr/bin/env python3
"""Prints the answer key for the benchmark questions, computed from the .full files of an AI-mode run.

Usage: answer-key.py <logs>/ai/*.full

Every test JVM writes its own .full file. It has every event (DEBUG and above, no dedupe), one per line:
'HH:mm:ss.SSS LEVEL [thread] logger #t17 [mdc] #id : message | fields: {...} | Exception: message | at frame | Caused by: ...'
(the test token after the logger, '#t17' or '~ #t17 #t21', only for events logged during a test; the stack trace follows the
message, separated by ' | '). Lines starting with '---- ' mark JVMs and tests.
The plain and careful baselines run the same tests, so the key applies to them too (check the totals once with the
careful baseline's grep recipes if the example tests change).
"""
import collections
import re
import sys

EVENT = re.compile(r'^\d\d:\d\d:\d\d\.\d{3} (\w+)(?:\(\w+\))? +\[[^\]]*\] (\S+)(?: #t\d+| ~(?: #t\d+)+)?(?: \[.*?\])? #[0-9a-f]{8,12} : (.*)$')
FRAME = re.compile(r'^at (\S+)\(([^)]*)\)')
EXCEPTION = re.compile(r'^(?:Caused by: |Wrapped by: |Suppressed: )?((?:[a-z_][\w$]*\.)+[A-Z][\w$]*)(?::|$)')
APP = re.compile(r'^(no\.entur\.grpc\.example|org\.entur\.example)\.')
RESPONSE = re.compile(r'^([45]\d\d) [A-Za-z ]+ https?://[^/\s]+(/\S*)')
REQUEST = re.compile(r'^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS) https?://[^/\s]+(/\S*)')


def events(paths):
    """Yields (level, logger, message, trace) per event; trace is the list of stack trace parts (exception lines and frames)."""
    for path in paths:
        with open(path, encoding='utf-8', errors='replace') as f:
            for raw in f:
                m = EVENT.match(raw.rstrip('\n'))
                if m:
                    message, trace = split(m.group(3))
                    yield m.group(1), m.group(2), message, trace


def split(text):
    """Splits the text after ' : ' into the message (with fields) and the parts of the stack trace, which starts at the first
    exception line that is followed by a frame."""
    parts = text.split(' | ')
    for i in range(1, len(parts) - 1):
        if EXCEPTION.match(parts[i]) and FRAME.match(parts[i + 1]):
            return ' | '.join(parts[:i]), parts[i:]
    return text, []


def normalize(message):
    message = re.sub(r'\b[uh]\d+(?:\([0-9a-f-]+\))?(?![\w(])', '<alias>', message)  # an alias, announced in .full as 'u7(<uuid>)'
    return re.sub(r'localhost:\d+', 'localhost:<port>', message)


def main(paths):
    groups = collections.Counter()
    exceptions = {}
    throws = collections.Counter()
    responses = collections.Counter()
    methods = collections.defaultdict(set)
    debug_4xx = collections.Counter()
    tomcat = grpc = 0
    grpc_ports = set()

    for level, logger, message, trace in events(paths):
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
    main(sys.argv[1:])
