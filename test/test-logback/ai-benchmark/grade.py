#!/usr/bin/env python3
"""Grades the answers of the runs against the answer key (rules in README.md) and prints one line per run.

Usage: grade.py <logs dir> [-v]       grades <logs dir>/runs/*.answer (written by summarize.py) against <logs dir>/answer-key.txt
       grade.py <answer-key.txt> <answer file>... [-v]

Matching is lenient about format (pipes, tables, bullets, full or simple logger names) but strict about numbers.
-v prints what was not found or wrong for each run; check those by hand, a paraphrased message is reported as missing.
"""
import glob
import os
import re
import sys


def norm(text):
    return re.sub(r'[^a-z0-9]', '', text.lower())


def read_key(path):
    key = {'q1': [], 'q2': [], 'q4': []}
    section = None
    for line in open(path, encoding='utf-8'):
        if re.match(r'^Q[1-4]:', line):
            section = line[:2].lower()
            if section == 'q3':
                m = re.search(r'Tomcat started (\d+) times, gRPC server started (\d+) times, gRPC port (\S+)', line)
                key['q3'] = (m.group(1), m.group(2), m.group(3))
            continue
        if line.startswith('    ') or not line.startswith('  '):
            continue  # DEBUG-only extras and notes are not graded
        cells = [c.strip() for c in line.strip().split(' | ')]
        if section == 'q1':
            level, logger, message, count, exception = cells
            message = re.split(r'<\w+>', message)[0]  # compare up to the first placeholder (<alias>, <port>)
            key['q1'].append({'level': level, 'logger': logger, 'message': message, 'count': int(count),
                              'exception': None if exception == '-' else exception.rsplit('.', 1)[-1]})
        elif section == 'q2':
            m = re.match(r'(\S+) (\S+) -> (\d+) x(\d+)', line.strip())
            if m:
                key['q2'].append({'method': m.group(1), 'path': m.group(2), 'status': m.group(3), 'count': m.group(4)})
        elif section == 'q4':
            exception, where, _ = cells
            key['q4'].append({'exception': exception.split(':')[0].rsplit('.', 1)[-1], 'where': where})
    return key


HEADER = re.compile(r'^\s{0,3}(?:#+\s*)?(?:\*\*)?\s*(?:Q(?:uestion)?\s*)?([1-4])\s*[.):]')
LEVEL = re.compile(r'\b(WARN|WARNING|ERROR)\b')
Q1_LINE = re.compile(r'\b(WARN|WARNING|ERROR)\s*\|')  # '1. WARN | logger | ...' is an answer, not a header
ITEM = re.compile(r'^\s{0,3}(\d+)[.)]\s')


def sections(answer):
    """Splits the answer at the question headers 1. to 4. (in order); a question without a header gets the whole answer."""
    lines = answer.splitlines()
    starts, expected, indent, item = {}, 1, None, None
    for i, line in enumerate(lines):
        m = HEADER.match(line)
        depth = len(line) - len(line.lstrip())
        n = ITEM.match(line)
        number = int(n.group(1)) if n else None
        if starts and number is not None and (indent is None or depth <= indent):
            # a numbered list under a header at the headers' indentation: starts again at 1, its items are not headers
            # (the next header can continue its numbering only after a blank line)
            header_after_blank = m and int(m.group(1)) == expected and not lines[i - 1].strip()
            if number == 1 or (item is not None and number == item + 1 and not header_after_blank):
                item = number
                continue
        if (m and int(m.group(1)) == expected and not Q1_LINE.search(line)
                and (indent is None or depth <= indent)):  # nested numbered lists are not headers
            starts[expected] = i
            indent = depth if indent is None else indent
            expected += 1
            item = None
            if expected > 4:
                break
    result = {}
    for q in range(1, 5):
        if q in starts:
            end = min([starts[n] for n in starts if n > q] or [len(lines)])
            result[q] = lines[starts[q]:end]
        else:
            result[q] = lines
    return result


def clean(line):
    return line.replace('**', '').replace('`', '')


def count_in(line, message):
    """The count of a Q1 line: the first number after the message (cell, 'x 12', '12 times', ...)."""
    cells = [c.strip() for c in line.split('|')]
    if len(cells) >= 4:
        for i, cell in enumerate(cells):
            if norm(message)[:30] and norm(message)[:30] in norm(cell):
                for later in cells[i + 1:]:
                    m = re.match(r'^[~≈]?\s*(?:about\s+)?(\d[\d,]*)\b', later)
                    if m:
                        return int(m.group(1).replace(',', ''))
                break
    # not a table row: first number after the message text
    text = norm(line)
    position = text.find(norm(message)[:30])
    tail = line
    if position >= 0:
        # cut the line after the message: find where the normalized prefix ends in the original text
        consumed, target = 0, len(norm(message))
        for index, char in enumerate(line):
            if re.match(r'[A-Za-z0-9]', char):
                consumed += 1
            if consumed >= target + position:
                tail = line[index + 1:]
                break
    m = re.search(r'(?<![\w.:/])(\d[\d,]*)(?![\w:/]|\.\d)', tail)
    return int(m.group(1).replace(',', '')) if m else None


def grade_q1(lines, entries, notes):
    lines = [clean(l) for l in lines]
    candidates = [l for l in lines if LEVEL.search(l)]
    used = set()
    groups = {}
    for entry in entries:
        prefix = norm(entry['message'])[:40]
        groups.setdefault((entry['level'], entry['logger'], prefix), []).append(entry)
    points = 0.0
    for (level, logger, prefix), group in groups.items():
        levels = ('WARN', 'WARNING') if level == 'WARN' else ('ERROR',)
        simple = logger.split('$')[0]
        # numbers in the message may be written as N (the agent took them for ids)
        wildcard = norm(re.sub(r'\d+', 'N', group[0]['message']))[:40]
        matching = [i for i, l in enumerate(candidates)
                    if LEVEL.search(l).group(1) in levels and simple in l
                    and (prefix in norm(l) or wildcard in norm(re.sub(r'(?<![\w])[Nn](?![\w])', 'N', l)))]
        counts = [count_in(candidates[i], group[0]['message']) for i in matching]
        used.update(matching)
        expected = sorted(e['count'] for e in group)
        if len(group) > 1 and sum(expected) in counts:  # the same message from several places, merged into one line
            correct = group
        else:
            remaining = list(counts)
            correct = []
            for e in group:
                if e['count'] in remaining:
                    remaining.remove(e['count'])
                    correct.append(e)
        for e in group:
            if e in correct:
                points += 1
                if e['exception']:
                    line = ' '.join(candidates[i] for i in matching)
                    if e['exception'] not in line:
                        points -= 0.5
                        notes.append(f"Q1 exception missing: {e['level']} {e['logger']} {e['message'][:40]} ({e['exception']})")
            else:
                notes.append(f"Q1 wrong: {e['level']} {e['logger']} {e['message'][:50]} expected {e['count']}, found {counts or 'nothing'}")
    for i, line in enumerate(candidates):
        cells = [c.strip() for c in line.split('|')]
        no_logger = len(cells) > 1 and re.match(r'^(-|$)', cells[1])  # JVM warnings on the console, not logged
        if i not in used and not no_logger and re.match(r'^[\s\-*|]*(\d+[.)]\s*)?(WARN|WARNING|ERROR)\s*\|', line):
            points -= 0.5
            notes.append(f'Q1 not in key: {line.strip()[:120]}')
    return max(points, 0) / len(entries)


def q2_count(line, path, status):
    """The first number after the status, if the status follows the path ('POST /a -> 400 x3, 404 x3'), else after the path."""
    after_path = line[line.find(path) + len(path):]
    m = re.search(rf'(?<!\d){status}(?!\d)', after_path)
    tail = after_path[m.end():] if m else after_path
    tail = re.sub(r'localhost:\d+|\d+\s*ms\b|\b[1-5]xx\b', ' ', tail)
    tail = re.sub(r'\b[xX](\d+)\b|\b(\d+)[xX]\b', lambda m: f' {m.group(1) or m.group(2)} ', tail)  # 'x3', '3x'
    m = re.search(r'(?<![\w.:/])(\d+)(?![\w:/]|\.\d)', tail)
    return m.group(1) if m else None


def q2_method(line, path):
    """The last HTTP method before the path, else the first after it."""
    line = re.sub(r'\b(GET|POST|PUT|PATCH|DELETE)/(GET|POST|PUT|PATCH|DELETE)\b', ' ', line)  # 'GET/POST not shown'
    position = line.find(path)
    before = re.findall(r'\b(GET|POST|PUT|PATCH|DELETE)\b', line[:position])
    after = re.findall(r'\b(GET|POST|PUT|PATCH|DELETE)\b', line[position:])
    return before[-1] if before else (after[0] if after else None)


def q2_items(line):
    """A line listing several requests ('POST /a: 400 x3. POST /b: 404 x3.') split into one item per request."""
    starts = [m.start() for m in re.finditer(r'\b(GET|POST|PUT|PATCH|DELETE)\s+/', line)]
    if len(starts) < 2:
        return [line]
    return [line[a:b] for a, b in zip(starts, starts[1:] + [len(line)])]


def grade_q2(lines, entries, notes):
    # a method given once in the section header ('2. 4xx/5xx responses (all POST)') applies to every request
    header = clean(lines[0]) if lines else ''
    methods = set() if re.search(r'(?<![\w])/\w', header) else set(re.findall(r'\b(GET|POST|PUT|PATCH|DELETE)\b', header))
    default_method = methods.pop() if len(methods) == 1 else None
    lines = [item for l in lines for item in q2_items(clean(l))]
    points = 0.0
    for e in entries:
        found = None
        for line in lines:
            if e['path'] in line and re.search(rf'(?<!\d){e["status"]}(?!\d)', line):
                found = (q2_method(line, e['path']), q2_count(line, e['path'], e['status']))
                break
        if not found:
            notes.append(f"Q2 missing: {e['method']} {e['path']} -> {e['status']} x{e['count']}")
            continue
        method, count = found
        method = method or default_method
        if method == e['method'] and count == e['count']:
            points += 1
        else:
            points += 0.5
            notes.append(f"Q2 half: {e['method']} {e['path']} -> {e['status']} x{e['count']}, found method {method}, count {count}")
    return points / len(entries)


def grade_q3(lines, key, notes):
    tomcat, grpc, port = key
    text = ' '.join(clean(l) for l in lines)
    sentences = re.split(r'(?<=[.;])\s+|\n', text)
    points = 0.0
    if any('tomcat' in s.lower() and re.search(rf'(?<![\w.:]){tomcat}(?![\w.:])', s) for s in sentences):
        points += 1
    else:
        notes.append(f'Q3 Tomcat starts: expected {tomcat}')
    if any('grpc' in s.lower() and re.search(rf'(?<![\w.:]){grpc}(?![\w.:])', s) for s in sentences):
        points += 1
    else:
        notes.append(f'Q3 gRPC server starts: expected {grpc}')
    if re.search(rf'(?<!\d){port}(?!\d)', text):
        points += 1
    else:
        notes.append(f'Q3 gRPC port: expected {port}')
    return points / 3


def grade_q4(lines, entries, notes):
    text = ' '.join(clean(l) for l in lines)
    text = re.sub(r'(\w+\.java)\s*[,:]?\s*\(?line\s+(\d+)', r'\1:\2', text)  # 'Foo.java, line 12' as 'Foo.java:12'
    points = 0.0
    for e in entries:
        for part in (e['exception'], e['where']):
            if part in text:
                points += 0.25
            else:
                notes.append(f'Q4 missing: {part}')
    return points / (len(entries) / 2) if entries else 1.0


def grade(answer, key):
    notes = []
    parts = sections(answer)
    scores = (grade_q1(parts[1], key['q1'], notes), grade_q2(parts[2], key['q2'], notes),
              grade_q3(parts[3], key['q3'], notes), grade_q4(parts[4], key['q4'], notes))
    return scores, notes


def main(args):
    verbose = '-v' in args
    args = [a for a in args if a != '-v']
    if len(args) == 1:
        key_file = os.path.join(args[0], 'answer-key.txt')
        answers = sorted(glob.glob(os.path.join(args[0], 'runs', '*.answer')))
    else:
        key_file, answers = args[0], args[1:]
    key = read_key(key_file)
    print(f'{"run":<44} {"Q1":>5} {"Q2":>5} {"Q3":>5} {"Q4":>5} {"score":>6}')
    for path in answers:
        scores, notes = grade(open(path, encoding='utf-8').read(), key)
        name = os.path.basename(path)[:-len('.answer')]
        print(f'{name:<44} ' + ' '.join(f'{s:>5.2f}' for s in scores) + f' {sum(scores):>6.2f}')
        if verbose:
            for note in notes:
                print(f'    {note}')


if __name__ == '__main__':
    main(sys.argv[1:])
