"""
作业打卡模块详细测试脚本
1. 先创建测试用的作业计划和人员
2. 全面测试进场/离场打卡的各种场景
"""
import json
import urllib.request
import urllib.error
import urllib.parse
import time

with open('C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/test_token.txt') as f:
    token = f.read().strip()

def api(method, path, data=None):
    # Handle URL encoding for query params
    if '?' in path:
        base, qs = path.split('?', 1)
        params = {}
        for p in qs.split('&'):
            k, v = p.split('=', 1)
            params[k] = v
        encoded_qs = urllib.parse.urlencode(params)
        path = base + '?' + encoded_qs
    url = f'http://localhost:8080{path}'
    headers = {'Content-Type': 'application/json', 'Authorization': f'Bearer {token}'}
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return resp.getcode(), json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())

def hr(title):
    print(f'\n{"="*65}')
    print(f'  {title}')
    print(f'{"="*65}')

def step(msg):
    print(f'\n  >>> {msg}')

def ok(code, res):
    return code == 200 and res.get('code') == 200

def check(name, condition, detail=''):
    s = '[PASS]' if condition else '[FAIL]'
    print(f'    {s} {name}' + (f' -> {detail}' if detail else ''))
    return condition

# ============================================================
hr('Phase 1: Seed Data - Create Test Plans & Workers')
# ============================================================

step('Creating 5 test plans with different work types')
plans = []
plan_data = [
    ('Guangzhou Tianhe Gas Pipeline', '动土',   23.128, 113.327, '2026-06-15 08:00:00', 'PetroChina'),
    ('Shenzhen Nanshan Anti-corrosion', '防腐',  22.543, 113.956, '2026-06-18 09:00:00', 'CNOOC'),
    ('Dongguan Humen Inspection', '检测',        22.815, 113.672, '2026-06-20 10:00:00', 'Sinopec'),
    ('Foshan Shunde Confined Space', '受限空间',  22.840, 113.182, '2026-06-22 07:00:00', 'CRCC'),
    ('Zhuhai Hengqin Mechanical', '机械作业',     22.154, 113.510, '2026-07-01 08:00:00', 'CCCC'),
]
for name, wtype, lat, lng, ptime, unit in plan_data:
    code, res = api('POST', '/homework/plan', {
        'cityCounty': name.split()[0],
        'constructionSite': name,
        'siteLatitude': lat, 'siteLongitude': lng,
        'planWorkTime': ptime,
        'projectName': name + ' Project',
        'workType': wtype, 'constructionUnit': unit,
        'workers': 'WorkerA, WorkerB, WorkerC',
        'workContent': f'{wtype}作业-{name}',
        'status': '0'
    })
    if code == 200 and res.get('code') == 200:
        # Search back to get the ID
        code2, search = api('GET', f'/homework/plan/list?pageNum=1&pageSize=10&projectName={name[:20]}')
        if search.get('rows'):
            pid = search['rows'][0]['planId']
            plans.append({'id': pid, 'name': name, 'lat': lat, 'lng': lng, 'wtype': wtype})
            print(f'    Created: planId={pid} | {name} [{wtype}]')

check(f'Created {len(plans)} plans', len(plans) == 5, f'{len(plans)}/5')

step('Creating 9 workers (one per role)')
workers = []
role_map = {
    '1': ('Applier','1'), '2': ('Approver','1'), '3': ('Guardian','1'),
    '4': ('Supervisor','2'), '5': ('Manager','3'), '6': ('SafetyOfficer','3'),
    '7': ('SiteLead','3'), '8': ('UnitGuard','3'), '9': ('Worker','3')
}
for rid, (rname, utype) in role_map.items():
    code, res = api('POST', '/homework/worker', {
        'workerName': f'Role{rid}_{rname}',
        'idCard': f'44010119900101000{rid}',
        'phone': f'1380013800{rid}',
        'roleType': rid, 'unitType': utype,
        'isFixedSite': '1',
        'checkRule': {'1':'point','2':'point','3':'checkInOut,hourly','4':'checkInOut,hourly','5':'point','6':'point','7':'briefing,checkOut,hourly,safety','8':'checkInOut,hourly,safety','9':'briefing'}[rid],
        'status': '0'
    })
    if code == 200 and res.get('code') == 200:
        # Search back to get ID
        code2, search = api('GET', '/homework/worker/list?pageNum=1&pageSize=20&workerName=Role')
        for r in search.get('rows', []):
            if r.get('workerName','').startswith(f'Role{rid}_'):
                workers.append({'id': r['workerId'], 'name': r['workerName'], 'role': rid, 'unit': utype})
                break
print(f'    Created {len(workers)} workers')
check(f'Created {len(workers)} workers', len(workers) == 9, f'{len(workers)}/9')

# ============================================================
hr('Phase 2: Attendance - Check-in Tests')
# ============================================================
pid = plans[0]['id']

step('2.1 Normal check-in')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 1, 'userName': 'Tester01',
    'checkType': '0', 'checkMethod': '0', 'location': 'Near Guangzhou Tianhe'
})
check('Normal check-in success', ok(code, res), res.get('msg',''))

step('2.2 Duplicate check-in (same plan + same user)')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 1, 'userName': 'Tester01',
    'checkType': '0', 'checkMethod': '0', 'location': 'Same Place'
})
check('Duplicate check-in BLOCKED', not ok(code, res), res.get('msg',''))

step('2.3 Different user check-in on same plan')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 2, 'userName': 'Tester02',
    'checkType': '0', 'checkMethod': '1', 'location': 'WeChat GPS'
})
check('Different user check-in OK', ok(code, res), res.get('msg',''))

step('2.4 Same user check-in on DIFFERENT plan')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': plans[1]['id'], 'userId': 1, 'userName': 'Tester01',
    'checkType': '0', 'checkMethod': '0', 'location': 'Shenzhen Nanshan'
})
check('Same user different plan OK', ok(code, res), res.get('msg',''))

step('2.5 Check-in with non-existent plan')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': 99999, 'userId': 5, 'userName': 'Ghost',
    'checkType': '0', 'checkMethod': '0', 'location': 'Nowhere'
})
check('Non-existent plan BLOCKED', not ok(code, res), res.get('msg',''))

step('2.6 WeChat method check-in')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': plans[2]['id'], 'userId': 3, 'userName': 'WeChatUser',
    'checkType': '0', 'checkMethod': '1', 'location': 'Dongguan GPS'
})
check('WeChat check-in OK', ok(code, res), res.get('msg',''))

# ============================================================
hr('Phase 3: Attendance - Check-out Tests')
# ============================================================

step('3.1 Normal check-out after check-in')
time.sleep(1.5)  # Wait 1.5s to cross second boundary (MySQL DATETIME is second-precision)
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 1, 'userName': 'Tester01',
    'checkType': '1', 'checkMethod': '0', 'location': 'Leaving Site'
})
check('Normal check-out OK', ok(code, res), res.get('msg',''))

step('3.2 Check-out without prior check-in')
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 99, 'userName': 'Stranger',
    'checkType': '1', 'checkMethod': '0', 'location': 'Unknown'
})
check('No-prior-check-in BLOCKED', not ok(code, res), res.get('msg',''))

step('3.3 Check-out after already checked out (duplicate check-out)')
time.sleep(0.3)
# Check out Tester02 who checked in earlier
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 2, 'userName': 'Tester02',
    'checkType': '1', 'checkMethod': '0', 'location': 'Done'
})
check('Tester02 check-out OK', ok(code, res))
# Try to check out again
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': 2, 'userName': 'Tester02',
    'checkType': '1', 'checkMethod': '0', 'location': 'Again'
})
check('Duplicate check-out BLOCKED', not ok(code, res), res.get('msg',''))

step('3.4 Cross-plan check-out (check-in on plan A, check-out on plan B)')
# Already checked in on plan[1] with user 1
time.sleep(0.3)
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': plans[2]['id'], 'userId': 1, 'userName': 'Tester01',
    'checkType': '1', 'checkMethod': '0', 'location': 'Wrong Plan'
})
check('Cross-plan check-out BLOCKED', not ok(code, res), res.get('msg',''))

step('3.5 Correct plan check-out (fix the cross-plan)')
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': plans[1]['id'], 'userId': 1, 'userName': 'Tester01',
    'checkType': '1', 'checkMethod': '0', 'location': 'Shenzhen Exit'
})
check('Correct plan check-out OK', ok(code, res), res.get('msg',''))

# ============================================================
hr('Phase 4: Query & Filter Tests')
# ============================================================

step('4.1 Query all attendance records')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50')
total = res.get('total', 0)
check('Query all records', ok(code, res), f'{total} records found')

step('4.2 Filter by plan')
code, res = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=50&planId={pid}')
check(f'Filter by planId={pid}', ok(code, res), f'{len(res.get("rows",[]))} records')

step('4.3 Filter by checkType=0 (check-in only)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkType=0')
check('Filter check-in only', ok(code, res), f'{len(res.get("rows",[]))} records')

step('4.4 Filter by checkType=1 (check-out only)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkType=1')
check('Filter check-out only', ok(code, res), f'{len(res.get("rows",[]))} records')

step('4.5 Filter by checkMethod=0 (face)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkMethod=0')
check('Filter face recognition', ok(code, res), f'{len(res.get("rows",[]))} records')

step('4.6 Filter by checkMethod=1 (WeChat)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkMethod=1')
check('Filter WeChat', ok(code, res), f'{len(res.get("rows",[]))} records')

step('4.7 Filter by checkStatus=0 (success only)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkStatus=0')
check('Filter success only', ok(code, res), f'{len(res.get("rows",[]))} records')

step('4.8 Filter by userName')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&userName=Tester01')
check('Filter by userName', ok(code, res), f'{len(res.get("rows",[]))} records')

# ============================================================
hr('Phase 5: Edge Cases')
# ============================================================

step('5.1 Check-in on DISABLED plan')
# First get a plan, then... actually we can't disable via this API, skip

step('5.2 Missing required fields')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': None, 'userId': 1, 'userName': 'NoPlan',
    'checkType': '0', 'checkMethod': '0', 'location': 'X'
})
check('Missing planId rejected', not ok(code, res), str(res.get('msg',''))[:50])

code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid, 'userId': None, 'userName': '',
    'checkType': '1', 'checkMethod': '0', 'location': ''
})
check('Missing userId rejected', not ok(code, res), str(res.get('msg',''))[:50])

step('5.3 Delete attendance record')
code, res = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=50&planId={plans[1]["id"]}')
records = res.get('rows', [])
if records:
    aid = records[0]['attendanceId']
    code, res = api('DELETE', f'/homework/attendance/{aid}')
    check(f'Delete record {aid}', ok(code, res), res.get('msg',''))

step('5.4 Batch delete')
code, res = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=50&planId={plans[2]["id"]}')
records = res.get('rows', [])
if len(records) >= 1:
    ids = ','.join(str(r['attendanceId']) for r in records)
    code, res = api('DELETE', f'/homework/attendance/{ids}')
    check(f'Batch delete {len(records)} records', ok(code, res), res.get('msg',''))

step('5.5 Verify record count after deletes')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=100')
check('Query all after cleanup', ok(code, res), f'Remaining: {res.get("total",0)}')

# ============================================================
hr('Phase 6: Quick Frontend Check')
# ============================================================
try:
    req = urllib.request.Request('http://localhost:80/')
    resp = urllib.request.urlopen(req)
    html = resp.read().decode()
    has_app = 'app' in html.lower() or 'ruoyi' in html.lower() or 'vue' in html.lower()
    check('Frontend serves index.html', resp.getcode() == 200 and has_app)
    req = urllib.request.Request('http://localhost:80/index.html')
    resp = urllib.request.urlopen(req)
    check('Frontend static file access', resp.getcode() == 200)
except Exception as e:
    print(f'    [INFO] Frontend check: {e}')

# ============================================================
hr('TEST SUMMARY')
# ============================================================
print(f"""
  Seed Data:    5 plans + 9 workers created
  Check-in:     normal / duplicate-blocked / multi-user / multi-plan / bad-plan / WeChat
  Check-out:    normal / no-prior-blocked / duplicate-blocked / cross-plan-blocked
  Query:        all / by-plan / by-type / by-method / by-status / by-name
  Edge:         missing-fields / single-delete / batch-delete

  Please also verify in browser at http://localhost:80:
  1. Login as admin/admin123
  2. Navigate to 作业管理 > 作业打卡
  3. Click 进场打卡 button and submit a test record
  4. Click again to verify duplicate block message
  5. Click 离场打卡 for the same user
  6. Try 离场打卡 on a user with no check-in
  """)

# Clean up test data
step('Cleanup: Removing test data')
for p in plans:
    # Delete attendance for this plan first
    code, res = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=100&planId={p["id"]}')
    for r in res.get('rows', []):
        api('DELETE', f'/homework/attendance/{r["attendanceId"]}')
    api('DELETE', f'/homework/plan/{p["id"]}')
code, res = api('GET', '/homework/worker/list?pageNum=1&pageSize=100&workerName=Role')
for r in res.get('rows', []):
    api('DELETE', f'/homework/worker/{r["workerId"]}')
print('    Test data cleaned up.')
