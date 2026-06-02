import json
import urllib.request
import urllib.error
import socket
import re

with open('C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/test_token.txt') as f:
    token = f.read().strip()

def api(method, path, data=None):
    url = f'http://localhost:8080{path}'
    headers = {'Content-Type': 'application/json', 'Authorization': f'Bearer {token}'}
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return resp.getcode(), json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())

def test(name, condition, detail=''):
    status = '[PASS]' if condition else '[FAIL]'
    print(f'  {status} {name}' + (f' - {detail}' if detail else ''))

def hr(title):
    print(f'\n{"="*60}')
    print(f'  {title}')
    print(f'{"="*60}')

# =====================================================
hr('1. Core CRUD: HwPlan')
# =====================================================

# 1a: Query list
code, res = api('GET', '/homework/plan/list?pageNum=1&pageSize=10')
test('GET /list status=200', code == 200, f'total={res.get("total",0)}')

# 1b: Create - returns AjaxResult with data=rows affected, not new ID
code, res = api('POST', '/homework/plan', {
    'cityCounty': 'Guangzhou',
    'constructionSite': 'TianheTest',
    'siteLatitude': 23.1234,
    'siteLongitude': 113.1234,
    'planWorkTime': '2026-06-20 09:00:00',
    'projectName': 'E2E Test Project',
    'workType': '动土',
    'constructionUnit': 'Test Corp',
    'workers': 'Alice,Bob',
    'workContent': 'E2E test content',
    'status': '0'
})
create_ok = code == 200 and res.get('code') == 200
test('POST / (create) success', create_ok, str(res.get('msg','')))

# Since insert returns int not the ID, find the new plan by searching
code, search = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=E2E')
if search.get('rows'):
    new_id = search['rows'][0]['planId']
    test('Find created plan by search', True, f'planId={new_id}')
else:
    new_id = None
    test('Find created plan by search', False)

# 1c: Get detail
if new_id:
    code, res = api('GET', f'/homework/plan/{new_id}')
    test('GET by id', code == 200 and res.get('data',{}).get('planId') == new_id)

# 1d: Update
if new_id:
    code, res = api('PUT', '/homework/plan', {
        'planId': new_id, 'cityCounty': 'Shenzhen', 'constructionSite': 'UpdatedSite',
        'siteLatitude': 22.5, 'siteLongitude': 114.0,
        'planWorkTime': '2026-07-01 10:00:00', 'projectName': 'E2E Updated Project',
        'workType': '防腐', 'constructionUnit': 'Updated Corp',
        'workers': 'Charlie', 'workContent': 'Updated content', 'status': '0'
    })
    test('PUT (update)', code == 200 and res.get('code') == 200)

# 1e: Check update took effect
if new_id:
    code, res = api('GET', f'/homework/plan/{new_id}')
    updated_name = res.get('data',{}).get('projectName','')
    test('Verify update', updated_name == 'E2E Updated Project', updated_name)

# 1f: Delete
if new_id:
    code, res = api('DELETE', f'/homework/plan/{new_id}')
    test('DELETE', code == 200 and res.get('code') == 200)
    # Verify gone
    code, res = api('GET', f'/homework/plan/{new_id}')
    test('Verify deleted', res.get('data') is None)

# =====================================================
hr('2. Core CRUD: HwWorker')
# =====================================================

code, res = api('GET', '/homework/worker/list?pageNum=1&pageSize=10')
total_before = res.get('total', 0)
test('GET /list', code == 200)

# Create 3 different role types
created = []
for name, role, unit in [('TestGuardian','3','1'), ('TestPM','5','3'), ('TestWorker','9','3')]:
    code, res = api('POST', '/homework/worker', {
        'workerName': name, 'idCard': '440101199001010001',
        'phone': '13800138000', 'roleType': role, 'unitType': unit,
        'isFixedSite': '1', 'checkRule': 'point', 'status': '0'
    })
    if code == 200 and res.get('code') == 200:
        created.append(name)
test('Create 3 workers', len(created) == 3, f'{len(created)}/3')

# Search by role
code, search = api('GET', '/homework/worker/list?pageNum=1&pageSize=10&roleType=3')
test('Search roleType=3', code == 200 and any(r.get('roleType')=='3' for r in search.get('rows',[])))

# Find created workers by name prefix
code, search = api('GET', '/homework/worker/list?pageNum=1&pageSize=50&workerName=Test')
created_ids = [r['workerId'] for r in search.get('rows', []) if r.get('workerName','').startswith('Test')]

if created_ids:
    wid = created_ids[0]
    code, res = api('PUT', '/homework/worker', {
        'workerId': wid, 'workerName': 'TestGuardian-Mod', 'phone': '13900000001',
        'roleType': '3', 'unitType': '1', 'isFixedSite': '0', 'checkRule': 'checkInOut,hourly', 'status': '0'
    })
    test('PUT update', code == 200 and res.get('code') == 200)

# Delete all test workers
deleted = 0
for wid in created_ids:
    code, res = api('DELETE', f'/homework/worker/{wid}')
    if code == 200 and res.get('code') == 200:
        deleted += 1
test(f'Delete {len(created_ids)} workers', deleted == len(created_ids), f'{deleted}/{len(created_ids)}')

# =====================================================
hr('3. Business Logic: HwAttendance')
# =====================================================

# Create test plan
code, res = api('POST', '/homework/plan', {
    'cityCounty': 'GZ', 'constructionSite': 'CheckInTest',
    'siteLatitude': 23.12, 'siteLongitude': 113.25,
    'planWorkTime': '2026-08-01 08:00:00', 'projectName': 'CheckInOut Test',
    'workType': '检测', 'constructionUnit': 'Test', 'workers': 'Tester',
    'workContent': 'Test', 'status': '0'
})
code2, search = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=CheckInOut')
plan_id = search['rows'][0]['planId'] if search.get('rows') else None
test('Setup: create test plan', plan_id is not None, f'planId={plan_id}')

if plan_id:
    # Test 1: Check-in
    code, res = api('POST', '/homework/attendance/checkIn', {
        'planId': plan_id, 'userId': 99, 'userName': 'Tester99',
        'checkType': '0', 'checkMethod': '0', 'location': 'Near Site'
    })
    checkin_pass = code == 200 and res.get('code') == 200
    test('Check-in success', checkin_pass, str(res.get('msg',''))[:40])

    # Test 2: Duplicate check-in blocked
    code, res = api('POST', '/homework/attendance/checkIn', {
        'planId': plan_id, 'userId': 99, 'userName': 'Tester99',
        'checkType': '0', 'checkMethod': '0', 'location': 'Near Site'
    })
    dup_blocked = res.get('code') == 500
    test('Duplicate check-in blocked', dup_blocked, str(res.get('msg',''))[:50])

    # Test 3: Check-out
    import time; time.sleep(0.5)
    code, res = api('POST', '/homework/attendance/checkOut', {
        'planId': plan_id, 'userId': 99, 'userName': 'Tester99',
        'checkType': '1', 'checkMethod': '0', 'location': 'Near Site'
    })
    checkout_pass = code == 200 and res.get('code') == 200
    test('Check-out success', checkout_pass, str(res.get('msg',''))[:40])

    # Test 4: Check-out without check-in blocked
    code, res = api('POST', '/homework/attendance/checkOut', {
        'planId': plan_id, 'userId': 100, 'userName': 'Stranger',
        'checkType': '1', 'checkMethod': '0', 'location': 'Unknown'
    })
    no_checkin_blocked = res.get('code') == 500
    test('Check-out w/o check-in blocked', no_checkin_blocked, str(res.get('msg',''))[:50])

    # Query attendance
    code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=10&planId=' + str(plan_id))
    test('Query attendance by planId', code == 200 and len(res.get('rows',[])) >= 2, f'{len(res.get("rows",[]))} records')

    # Cleanup attendance
    all_ids = [r['attendanceId'] for r in res.get('rows', [])]
    if all_ids:
        api('DELETE', '/homework/attendance/' + ','.join(str(i) for i in all_ids))

    # Cleanup plan
    api('DELETE', f'/homework/plan/{plan_id}')

# =====================================================
hr('4. Frontend Verification')
# =====================================================

# Check frontend is serving pages
try:
    req = urllib.request.Request('http://localhost:80/')
    resp = urllib.request.urlopen(req)
    test('Frontend index loads', resp.getcode() == 200)
    # Check login page
    req = urllib.request.Request('http://localhost:80/login')
    resp = urllib.request.urlopen(req)
    test('Frontend login page', resp.getcode() == 200)
except Exception as e:
    test('Frontend accessible', False, str(e))

# =====================================================
hr('TEST SUMMARY')
# =====================================================
print()
print('  Backend API tests done. Key validations:')
print('  1. HwPlan:  list/create/read/update/delete')
print('  2. HwWorker: list/create/search/update/delete')
print('  3. HwAttendance: check-in, duplicate-block, check-out, no-in-block')
print()
print('  Frontend checks for manual verification:')
print('  1. Open http://localhost:80, login admin/admin123')
print('  2. Sidebar: verify "作业管理" appears after "系统工具"')
print('  3. Click each sub-menu: 作业计划, 作业打卡, 人员管理')
print('  4. Test CRUD operations on each page')
print('  5. Test check-in/out with business rule validation')
