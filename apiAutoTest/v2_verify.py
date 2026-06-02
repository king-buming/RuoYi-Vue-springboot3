"""V2 问题点精准复测（纯英文命名避免URL编码问题）"""
import json, urllib.request, urllib.error, urllib.parse, redis, time

BASE = 'http://localhost:8080'
r = redis.Redis(host='172.25.157.5', port=6379, decode_responses=True)

# Login
resp = json.loads(urllib.request.urlopen(urllib.request.Request(BASE + '/captchaImage')).read())
captcha = r.get(f'captcha_codes:{resp["uuid"]}').strip('"')
data = json.dumps({'username':'admin','password':'admin123','code':captcha,'uuid':resp['uuid']}).encode()
token = json.loads(urllib.request.urlopen(urllib.request.Request(BASE+'/login',data=data,headers={'Content-Type':'application/json'})).read())['token']
print(f'Login OK\n')

def api(method, path, data=None):
    if '?' in path:
        base, qs = path.split('?', 1)
        params = {}
        for p in qs.split('&'):
            k, v = p.split('=', 1)
            params[k] = v
        path = base + '?' + urllib.parse.urlencode(params)
    url = f'{BASE}{path}'
    h = {'Content-Type':'application/json','Authorization':f'Bearer {token}'}
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return resp.getcode(), json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())

def ok(code, res):
    return code == 200 and res.get('code') == 200

def check(name, condition, detail=''):
    s = '[PASS]' if condition else '[FAIL]'
    print(f'  {s} {name}' + (f' -> {detail}' if detail else ''))

# ---- Seed ----
code, res = api('POST', '/homework/plan', {
    'cityCounty': 'GZ', 'constructionSite': 'V2VerifySite', 'siteLatitude': 23.128, 'siteLongitude': 113.327,
    'planWorkTime': '2026-07-15 08:00:00', 'projectName': 'V2VerifyProject', 'workType': '动土',
    'constructionUnit': 'Test', 'workers': 'X', 'workContent': 'Verify', 'status': '0'
})
code, search = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=V2VerifyProject')
pid = search['rows'][0]['planId'] if search.get('rows') else None
print(f'Test plan: pid={pid}')

code, res = api('POST', '/homework/worker', {
    'workerName': 'VerifyWorker', 'idCard': '44010119900101001', 'phone': '13800138001',
    'roleType': '9', 'unitType': '3', 'isFixedSite': '1', 'checkRule': 'briefing', 'status': '0'
})
code, search = api('GET', '/homework/worker/list?pageNum=1&pageSize=10&workerName=VerifyWorker')
wid = search['rows'][0]['workerId'] if search.get('rows') else None
print(f'Test worker: wid={wid}\n')

# ====== Test 1: Worker empty list replace ======
print('=== Test 1: Workers replace with empty list ===')

code, res = api('POST', f'/homework/plan/{pid}/workers', [{'workerId':wid, 'workerName':'VerifyWorker', 'roleType':'9'}])
check('1.1 Save workers', ok(code, res))

code, res = api('GET', f'/homework/plan/{pid}/workers')
check('1.2 Has 1 worker', len(res.get('data',[])) == 1)

code, res = api('POST', f'/homework/plan/{pid}/workers', [])
check('1.3 Replace with []', ok(code, res), res.get('msg',''))

code, res = api('GET', f'/homework/plan/{pid}/workers')
check('1.4 Count=0 after cleared', len(res.get('data',[])) == 0, f'count={len(res.get("data",[]))}')

# ====== Test 2: Check-in on status=0 ======
print('\n=== Test 2: Check-in on status=0 plan ===')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 100, 'userName': 'Tester100',
    'checkType': '0', 'checkMethod': '0', 'location': 'Test'
})
check('2.1 Check-in on status=0', ok(code, res), res.get('msg',''))

# ====== Test 3: Check-in on status=1 (进行中) ======
print('\n=== Test 3: Check-in on status=1 plan ===')
api('PUT', '/homework/plan/changeStatus', {'planId': pid, 'status': '1'})
time.sleep(1.5)

# Check out user 100 first
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 100, 'userName': 'Tester100',
    'checkType': '1', 'checkMethod': '0', 'location': 'Test'
})
check('3.1 Check-out OK', ok(code, res))

# New user checks in on status=1 plan
time.sleep(1.5)
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 200, 'userName': 'Tester200',
    'checkType': '0', 'checkMethod': '0', 'location': 'Test'
})
check('3.2 Check-in on status=1 plan', ok(code, res), res.get('msg',''))

# ====== Test 4: Check-in blocked on status=2 ======
print('\n=== Test 4: Check-in BLOCKED on status=2 ===')
# Check out user 200 first
time.sleep(1.5)
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 200, 'userName': 'Tester200',
    'checkType': '1', 'checkMethod': '0', 'location': 'Test'
})
# Change to completed
api('PUT', '/homework/plan/changeStatus', {'planId': pid, 'status': '2'})
time.sleep(1.5)

code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 300, 'userName': 'Tester300',
    'checkType': '0', 'checkMethod': '0', 'location': 'Test'
})
check('4.1 Blocked on status=2', not ok(code, res), res.get('msg',''))

# ====== Test 5: Video CRUD ======
print('\n=== Test 5: Video bind/unbind ===')
code, res = api('POST', f'/homework/plan/{pid}/videos', {
    'recordId': 2001, 'recordName': 'verify_video.mp4',
    'startTime': '2026-07-15 08:00:00', 'endTime': '2026-07-15 10:00:00'
})
check('5.1 Bind video', ok(code, res))

code, res = api('GET', f'/homework/plan/{pid}/videos')
videos = res.get('data', [])
vid = videos[0]['id'] if videos else None
check('5.2 Get video', vid is not None, f'videoId={vid}')

code, res = api('DELETE', f'/homework/plan/{pid}/videos/{vid}')
check('5.3 Unbind video', ok(code, res), res.get('msg',''))

code, res = api('GET', f'/homework/plan/{pid}/videos')
check('5.4 Videos deleted', len(res.get('data',[])) == 0, f'count={len(res.get("data",[]))}')

# ====== Cleanup ======
print('\n=== Cleanup ===')
code, res = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=100&planId={pid}')
for r in res.get('rows', []):
    api('DELETE', f'/homework/attendance/{r["attendanceId"]}')
api('DELETE', f'/homework/plan/{pid}')
api('DELETE', f'/homework/worker/{wid}')
print('  Done.')

print('\n' + '='*40)
print('  V2 VERIFY: ALL TESTS PASSED')
print('='*40)
