import json, urllib.request, urllib.error, time

with open('C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/test_token.txt') as f:
    token = f.read().strip()

def api(method, path, data=None):
    url = f'http://localhost:8080{path}'
    headers = {'Content-Type': 'application/json', 'Authorization': f'Bearer {token}'}
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read())

def ok(res): return res.get('code') == 200

# Create test plan
res = api('POST', '/homework/plan', {
    'projectName': 'DupTest', 'constructionSite': 'Test', 'cityCounty': 'GZ',
    'siteLatitude': 23.1, 'siteLongitude': 113.3, 'planWorkTime': '2026-09-01 08:00:00',
    'workType': '检测', 'constructionUnit': 'T', 'workers': 'X', 'workContent': 'T', 'status': '0'
})
search = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=DupTest')
pid = search['rows'][0]['planId'] if search.get('rows') else None
print(f'Plan created: id={pid}')

if not pid: exit(1)

# Check-in
res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 88, 'userName': 'DupTester',
    'checkType': '0', 'checkMethod': '0', 'location': 'Test'
})
print(f'1. Check-in:   {"[PASS]" if ok(res) else "[FAIL]"} -> {res.get("msg","")}')

# Check-out
time.sleep(0.3)
res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 88, 'userName': 'DupTester',
    'checkType': '1', 'checkMethod': '0', 'location': 'Test'
})
print(f'2. Check-out:  {"[PASS]" if ok(res) else "[FAIL]"} -> {res.get("msg","")}')

# DUPLICATE Check-out - MUST be blocked
res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 88, 'userName': 'DupTester',
    'checkType': '1', 'checkMethod': '0', 'location': 'Test'
})
dup_blocked = not ok(res)
print(f'3. Dup-Out:    {"[PASS]" if dup_blocked else "[FAIL]"} -> {res.get("msg","")}')
print()
print('Fix verified!' if dup_blocked else 'Please restart backend first (RuoyiApplication in IDEA)')

# Cleanup
records = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=100&planId={pid}').get('rows',[])
for r in records: api('DELETE', f'/homework/attendance/{r["attendanceId"]}')
api('DELETE', f'/homework/plan/{pid}')
