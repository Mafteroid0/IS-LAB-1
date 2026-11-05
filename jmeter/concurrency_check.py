"""Проверка параллельной работы без JMeter: тот же сценарий (импорт, откат, история, гонки уникальности,
гонки изменения/удаления). Запуск: APP_ADMINS=boss у приложения, затем
    BASE_URL=http://localhost:8080/routes/api python jmeter/concurrency_check.py
Код возврата 0 — все проверки пройдены."""
import http.cookiejar, json, os, threading, urllib.request, urllib.error, uuid, sys

BASE = os.environ.get('BASE_URL', 'http://localhost:8080/routes/api')
RUN = uuid.uuid4().hex[:6]


class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def call(self, method, path, body=None, raw=None, ctype=None):
        data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
        req = urllib.request.Request(BASE + path, data=data, method=method)
        req.add_header('X-Requested-With', 'RouteLab')
        if raw is not None:
            req.add_header('Content-Type', ctype)
        elif body is not None:
            req.add_header('Content-Type', 'application/json')
        try:
            with self.opener.open(req) as r:
                text = r.read().decode()
                return r.status, (json.loads(text) if text else None)
        except urllib.error.HTTPError as e:
            text = e.read().decode()
            return e.code, (json.loads(text) if text else None)

    def upload(self, content):
        b = '----b' + RUN
        raw = (f'--{b}\r\nContent-Disposition: form-data; name="file"; filename="r.json"\r\n'
               f'Content-Type: application/json\r\n\r\n').encode() + content.encode() + f'\r\n--{b}--\r\n'.encode()
        return self.call('POST', '/imports', raw=raw, ctype=f'multipart/form-data; boundary={b}')


def user(name):
    c = Client()
    s, _ = c.call('POST', '/auth/register', {'username': name, 'password': 'password123'})
    if s == 409:
        s, _ = c.call('POST', '/auth/login', {'username': name, 'password': 'password123'})
    assert s in (200, 201), (name, s)
    return c


def race(n, fn):
    out = [None] * n
    gate = threading.Barrier(n)

    def work(i):
        gate.wait()
        out[i] = fn(i)
    ts = [threading.Thread(target=work, args=(i,)) for i in range(n)]
    [t.start() for t in ts]
    [t.join() for t in ts]
    return out


def codes(results):
    return sorted(r[0] for r in results)


fails = []


def check(name, cond, info=''):
    print(('PASS ' if cond else 'FAIL ') + name, info)
    if not cond:
        fails.append(name)


a, b, boss = user(f'ua{RUN}'), user(f'ub{RUN}'), user('boss')
print('me:', a.call('GET', '/auth/me')[1], boss.call('GET', '/auth/me')[1])

# 1. import success with nested objects
good = json.dumps([
    {'name': f'imp1-{RUN}', 'coordinates': {'x': 1.5, 'y': 2.5}, 'from': {'x': 0, 'y': 0, 'z': 0, 'name': f'A-{RUN}'},
     'to': {'x': 1, 'y': 1, 'z': 1, 'name': f'B-{RUN}'}, 'distance': 10, 'rating': 4},
    {'name': f'imp2-{RUN}', 'coordinates': {'x': 1.5, 'y': 2.5}, 'from': None,
     'to': {'x': 1, 'y': 1, 'z': 1, 'name': f'B-{RUN}'}, 'distance': 5, 'rating': 3}])
s, r = a.upload(good)
check('import ok 201 added=2', s == 201 and r['added'] == 2, (s, r))
total_before = a.call('GET', '/routes?size=100')[1]['total']
check('routes created', total_before >= 2, total_before)
print('coords reused:', sum(1 for c in a.call('GET', '/coordinates')[1] if c['x'] == 1.5 and c['y'] == 2.5) == 1)

# 2. atomic rollback: second record violates validation / uniqueness
bad_validation = json.dumps([
    {'name': f'rb1-{RUN}', 'coordinates': {'x': 7, 'y': 7}, 'to': {'x': 1, 'y': 1, 'z': 1, 'name': f'RB-{RUN}'}, 'distance': 10, 'rating': 4},
    {'name': ' ', 'coordinates': {'x': 8, 'y': 8}, 'to': {'x': 1, 'y': 1, 'z': 1, 'name': 'q'}, 'distance': 0.5, 'rating': -1}])
s, r = a.upload(bad_validation)
check('validation rejected 400 with fields', s == 400 and r['fields'], (s, r))
dup_in_file = json.dumps([
    {'name': f'rb2-{RUN}', 'coordinates': {'x': 9, 'y': 9}, 'to': {'x': 1, 'y': 1, 'z': 1, 'name': f'RC-{RUN}'}, 'distance': 10, 'rating': 4},
    {'name': f'RB2-{RUN}', 'coordinates': {'x': 9, 'y': 10}, 'to': {'x': 1, 'y': 1, 'z': 1, 'name': f'RC-{RUN}'}, 'distance': 10, 'rating': 4}])
s, r = a.upload(dup_in_file)
check('duplicate name inside file -> 409', s == 409, (s, r))
s, r = a.upload('not json')
check('garbage -> 400', s == 400, (s, r))
check('nothing created by failed imports', a.call('GET', '/routes?size=100')[1]['total'] == total_before)
check('no leaked coordinates', not any(c['x'] in (7.0, 8.0, 9.0) for c in a.call('GET', '/coordinates')[1]))

# 3. history visibility
s, hb = b.call('GET', '/imports')
check('user b sees no foreign history', s == 200 and all(h['username'] == b_name for h in hb for b_name in [f'ub{RUN}']), hb)
s, ha = a.call('GET', '/imports')
check('user a sees own (1 ok + 3 failed)', [h['status'] for h in ha].count('FAILED') >= 3 and any(h['status'] == 'SUCCESS' and h['added'] == 2 for h in ha))
s, hs = boss.call('GET', '/imports')
check('admin sees all users', {h['username'] for h in hs} >= {f'ua{RUN}'}, len(hs))
check('failed rows have no added count', all(h['added'] is None for h in ha if h['status'] == 'FAILED'))

# 4. uniqueness races
mk = lambda i: a.call('POST', '/routes', {'name': f'race-{RUN}', 'coordinatesId': cid, 'toId': lid, 'distance': 5, 'rating': 1})
cid = a.call('POST', '/coordinates', {'x': 100.5, 'y': 100.5})[1]['id']
lid = a.call('POST', '/locations', {'x': 1, 'y': 1, 'z': 1, 'name': f'racepoint-{RUN}'})[1]['id']
res = race(8, lambda i: (b if i % 2 else a).call('POST', '/routes', {'name': f'race-{RUN}' if i % 2 else f'  RACE-{RUN}', 'coordinatesId': cid, 'toId': lid, 'distance': 5, 'rating': 1}))
check('route name race: exactly one 201', codes(res).count(201) == 1 and codes(res).count(409) == 7, codes(res))
res = race(8, lambda i: a.call('POST', '/locations', {'x': 3, 'y': 3, 'z': 3, 'name': f'loc-race-{RUN}'}))
check('location name race: exactly one 201', codes(res).count(201) == 1 and codes(res).count(409) == 7, codes(res))
res = race(8, lambda i: a.call('POST', '/coordinates', {'x': 55.25, 'y': 66.5}))
check('coordinates race: exactly one 201', codes(res).count(201) == 1 and codes(res).count(409) == 7, codes(res))
imp = json.dumps([{'name': f'ir-{RUN}', 'coordinates': {'x': 321, 'y': 123}, 'to': {'x': 1, 'y': 1, 'z': 1, 'name': f'ir-loc-{RUN}'}, 'distance': 10, 'rating': 4}])
res = race(6, lambda i: (a if i % 2 else b).upload(imp))
check('concurrent identical imports: one 201', codes(res).count(201) == 1 and codes(res).count(409) == 5, codes(res))

# 5. update / delete races on the same object
r0 = a.call('POST', '/routes', {'name': f'shared-{RUN}', 'coordinatesId': cid, 'toId': lid, 'distance': 5, 'rating': 1})[1]
res = race(2, lambda i: (a if i == 0 else boss).call('PUT', f"/routes/{r0['id']}", {'name': f'upd{i}-{RUN}', 'coordinatesId': cid, 'toId': lid, 'distance': 6, 'rating': 2, 'version': int(r0['version'])}))
check('update race: one 200, one 409', codes(res) == [200, 409], codes(res))
now = a.call('GET', f"/routes/{r0['id']}")[1]
check('version advanced exactly once', now['version'] == str(int(r0['version']) + 1), now['version'])
res = race(2, lambda i: (a if i == 0 else boss).call('DELETE', f"/routes/{r0['id']}?version={now['version']}"))
check('delete race: one 204, other 404/409', codes(res)[0] == 204 and codes(res)[1] in (404, 409), codes(res))
r1 = a.call('POST', '/routes', {'name': f'shared2-{RUN}', 'coordinatesId': cid, 'toId': lid, 'distance': 5, 'rating': 1})[1]
res = race(2, lambda i: [a.call('PUT', f"/routes/{r1['id']}", {'name': f'x-{RUN}', 'coordinatesId': cid, 'toId': lid, 'distance': 6, 'rating': 2, 'version': int(r1['version'])}),
                         boss.call('DELETE', f"/routes/{r1['id']}?version={r1['version']}")][i])
check('update vs delete: at most one wins', codes(res).count(409) + codes(res).count(404) >= 1 and (200 in codes(res) or 204 in codes(res)), codes(res))

# 6. paging consistency while writing
def reader(i):
    bad = 0
    for _ in range(30):
        s, p = a.call('GET', '/routes?size=10&page=0')
        if s != 200 or len(p['items']) > p['size'] or p['total'] < len(p['items']):
            bad += 1
    return bad
def writer(i):
    for k in range(10):
        a.call('POST', '/coordinates', {'x': 900 + i * 100 + k, 'y': 1})
    return 0
res = race(4, lambda i: reader(i) if i < 2 else writer(i))
check('concurrent reads/writes stay consistent', sum(res) == 0, res)

print('FAILED:' if fails else 'ALL OK', fails)
sys.exit(1 if fails else 0)
