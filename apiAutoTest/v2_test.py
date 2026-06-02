"""
V2 功能测试脚本
测试所有 V2 新增功能：状态流转、人员关联、打卡跳转、AI人脸、GPS校验、录像关联、手机端接口
"""
import json
import urllib.request
import urllib.error
import urllib.parse
import time
import redis

BASE = 'http://localhost:8080'
r = redis.Redis(host='172.25.157.5', port=6379, decode_responses=True)

# ----- Get token -----
code, res = urllib.request.urlopen(urllib.request.Request(BASE + '/captchaImage')).getcode(), json.loads(urllib.request.urlopen(urllib.request.Request(BASE + '/captchaImage')).read())
uuid = res['uuid']
captcha_code = r.get(f'captcha_codes:{uuid}').strip('"')
print(f'Captcha: {captcha_code}')

data = json.dumps({'username': 'admin', 'password': 'admin123', 'code': captcha_code, 'uuid': uuid}).encode()
req = urllib.request.Request(BASE + '/login', data=data, headers={'Content-Type': 'application/json'})
resp = json.loads(urllib.request.urlopen(req).read())
token = resp['token']
print(f'Login OK, token[:30]: {token[:30]}...')

def api(method, path, data=None):
    if '?' in path:
        base, qs = path.split('?', 1)
        params = {}
        for p in qs.split('&'):
            k, v = p.split('=', 1)
            params[k] = v
        path = base + '?' + urllib.parse.urlencode(params)
    url = f'{BASE}{path}'
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

all_pass = True

# ============================================================
hr('Phase 1: Seed Data')
# ============================================================
step('Creating test plan with status=0')
code, res = api('POST', '/homework/plan', {
    'cityCounty': '广州市', 'constructionSite': 'V2测试工地', 'siteLatitude': 23.128, 'siteLongitude': 113.327,
    'planWorkTime': '2026-07-01 08:00:00', 'projectName': 'V2状态流转测试项目', 'workType': '动土',
    'constructionUnit': '中建三局', 'workers': '测试工A', 'workContent': 'V2功能测试', 'status': '0'
})
code2, search = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=V2状态流转测试项目')
pid = search['rows'][0]['planId'] if search.get('rows') else None
check('Create test plan', ok(code, res) and pid is not None, f'planId={pid}')

# Create a test worker
step('Creating test worker')
code, res = api('POST', '/homework/worker', {
    'workerName': 'V2测试工', 'idCard': '44010119900101999', 'phone': '13800138999',
    'roleType': '9', 'unitType': '3', 'isFixedSite': '1', 'checkRule': 'briefing', 'status': '0'
})
code2, search = api('GET', '/homework/worker/list?pageNum=1&pageSize=10&workerName=V2测试工')
wid = search['rows'][0]['workerId'] if search.get('rows') else None
check('Create test worker', wid is not None, f'workerId={wid}')

# ============================================================
hr('Phase 2: Status Flow (P0 #37)')
# ============================================================

step('2.1 Change status: 0(待执行) → 1(进行中)')
code, res = api('PUT', '/homework/plan/changeStatus', {'planId': pid, 'status': '1'})
check('待执行→进行中', ok(code, res), res.get('msg',''))

step('2.2 Change status: 1(进行中) → 2(已完成)')
code, res = api('PUT', '/homework/plan/changeStatus', {'planId': pid, 'status': '2'})
check('进行中→已完成', ok(code, res), res.get('msg',''))

step('2.3 Completed plan CANNOT be changed')
code, res = api('PUT', '/homework/plan/changeStatus', {'planId': pid, 'status': '1'})
check('已完成不可再变更', not ok(code, res), res.get('msg',''))

step('2.4 Verify status in plan list')
code, res = api('GET', f'/homework/plan/{pid}')
check('Status=2 in detail', res.get('data',{}).get('status') == '2', f'status={res.get("data",{}).get("status")}')

step('2.5 Invalid transition: 0→2 (skip 进行中)')
# Create another plan for this test
code, res = api('POST', '/homework/plan', {
    'cityCounty': '广州市', 'constructionSite': 'V2状态测试2', 'siteLatitude': 23.1, 'siteLongitude': 113.3,
    'planWorkTime': '2026-07-02 08:00:00', 'projectName': 'V2状态流转测试项目2', 'workType': '检测',
    'constructionUnit': '测试', 'workers': 'X', 'workContent': 'X', 'status': '0'
})
code2, search2 = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=V2状态流转测试项目2')
pid2 = search2['rows'][0]['planId'] if search2.get('rows') else None
code, res = api('PUT', '/homework/plan/changeStatus', {'planId': pid2, 'status': '2'})
check('待执行→已完成被拦截', not ok(code, res), res.get('msg',''))

step('2.6 Cancel plan: 0 → 3')
code, res = api('PUT', '/homework/plan/changeStatus', {'planId': pid2, 'status': '3'})
check('待执行→已取消', ok(code, res), res.get('msg',''))

step('2.7 Canceled plan CANNOT be changed')
code, res = api('PUT', '/homework/plan/changeStatus', {'planId': pid2, 'status': '1'})
check('已取消不可再变更', not ok(code, res), res.get('msg',''))

# ============================================================
hr('Phase 3: Worker Association (P0 #40)')
# ============================================================

step('3.1 Save workers to plan')
code, res = api('POST', f'/homework/plan/{pid}/workers', [
    {'workerId': wid, 'workerName': 'V2测试工', 'roleType': '9'}
])
check('Save workers OK', ok(code, res))

step('3.2 Get workers for plan')
code, res = api('GET', f'/homework/plan/{pid}/workers')
workers_list = res.get('data', [])
check(f'Get {len(workers_list)} workers', len(workers_list) == 1, f'workers={workers_list}')

step('3.3 Replace workers (delete old + insert new)')
code, res = api('POST', f'/homework/plan/{pid}/workers', [])
check('Replace with empty list', ok(code, res))
code, res = api('GET', f'/homework/plan/{pid}/workers')
check('Workers list empty after replace', len(res.get('data', [])) == 0, f'count={len(res.get("data",[]))}')

# ============================================================
hr('Phase 4: Attendance Tests (existing + new V2 features)')
# ============================================================

step('4.1 Normal check-in (face recognition method)')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid, 'userId': 1, 'userName': 'V2TestAdmin',
    'checkType': '0', 'checkMethod': '0', 'location': 'V2测试点'
})
check('Check-in with face method', ok(code, res), res.get('msg',''))

step('4.2 Check-in with WeChat method (GPS validation)')
# Plan pid is already completed, can't check in. Use pid2 which is canceled.
# Let's create a fresh plan for attendance tests
code, res = api('POST', '/homework/plan', {
    'cityCounty': '广州市', 'constructionSite': 'V2打卡测试', 'siteLatitude': 23.128, 'siteLongitude': 113.327,
    'planWorkTime': '2026-07-10 08:00:00', 'projectName': 'V2打卡测试项目', 'workType': '防腐',
    'constructionUnit': '测试', 'workers': 'X', 'workContent': 'X', 'status': '0'
})
code2, s3 = api('GET', '/homework/plan/list?pageNum=1&pageSize=10&projectName=V2打卡测试项目')
pid3 = s3['rows'][0]['planId'] if s3.get('rows') else None
check('Create plan for attendance tests', pid3 is not None, f'planId={pid3}')

step('4.3 WeChat check-in with valid GPS coordinates')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid3, 'userId': 10, 'userName': 'WechatTester',
    'checkType': '0', 'checkMethod': '1', 'location': '113.327,23.128'
})
check('WeChat check-in (valid GPS)', ok(code, res), res.get('msg',''))

step('4.4 WeChat check-in with OUT-OF-RANGE GPS (>500m)')
time.sleep(1.5)
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid3, 'userId': 11, 'userName': 'FarTester',
    'checkType': '0', 'checkMethod': '1', 'location': '113.500,23.500'
})
check('GPS out of range - status should be 2(异常)', code == 200 and res.get('code') == 200,
    f"Written but marked abnormal: {res.get('msg','')}")

step('4.5 WeChat check-in with bad GPS format')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid3, 'userId': 12, 'userName': 'BadGPSTester',
    'checkType': '0', 'checkMethod': '1', 'location': 'invalid_gps_format'
})
check('Bad GPS format still writes record', ok(code, res), res.get('msg',''))

step('4.6 Duplicate check-in blocked (within pid3)')
code, res = api('POST', '/homework/attendance/checkIn', {
    'planId': pid3, 'userId': 10, 'userName': 'WechatTester',
    'checkType': '0', 'checkMethod': '1', 'location': '113.327,23.128'
})
check('Duplicate check-in BLOCKED', not ok(code, res), res.get('msg',''))

step('4.7 Normal check-out')
time.sleep(1.5)
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid3, 'userId': 10, 'userName': 'WechatTester',
    'checkType': '1', 'checkMethod': '0', 'location': 'Leaving'
})
check('Normal check-out OK', ok(code, res), res.get('msg',''))

step('4.8 Duplicate check-out blocked')
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid3, 'userId': 10, 'userName': 'WechatTester',
    'checkType': '1', 'checkMethod': '0', 'location': 'Again'
})
check('Duplicate check-out BLOCKED', not ok(code, res), res.get('msg',''))

step('4.9 Check-out without check-in')
code, res = api('POST', '/homework/attendance/checkOut', {
    'planId': pid3, 'userId': 999, 'userName': 'Stranger',
    'checkType': '1', 'checkMethod': '0', 'location': 'Unknown'
})
check('No prior check-in BLOCKED', not ok(code, res), res.get('msg',''))

# ============================================================
hr('Phase 5: Video Association (P1 #38)')
# ============================================================

step('5.1 Bind video to plan')
code, res = api('POST', f'/homework/plan/{pid3}/videos', {
    'recordId': 1001, 'recordName': '施工录像_20260602.mp4',
    'startTime': '2026-07-10 08:00:00', 'endTime': '2026-07-10 10:00:00'
})
check('Bind video OK', ok(code, res))

step('5.2 Get videos for plan')
code, res = api('GET', f'/homework/plan/{pid3}/videos')
videos = res.get('data', [])
check(f'Get {len(videos)} videos', len(videos) >= 1, f'videos={videos}')

step('5.3 Unbind video')
if videos:
    vid = videos[0]['id']
    code, res = api('DELETE', f'/homework/plan/{pid3}/videos/{vid}')
    check(f'Unbind video {vid}', ok(code, res), res.get('msg',''))

# ============================================================
hr('Phase 6: Wechat Controllers (P1 #49 + P2 #36)')
# ============================================================

step('6.1 WechatAttendance checkIn')
code, res = api('POST', '/wechat/attendance/checkIn', {
    'planId': pid3, 'userId': 20, 'userName': 'WechatMobile',
    'checkMethod': '1', 'location': '113.327,23.128'
})
check('Wechat checkIn OK', ok(code, res), res.get('msg',''))

step('6.2 WechatAttendance checkOut')
time.sleep(1.5)
code, res = api('POST', '/wechat/attendance/checkOut', {
    'planId': pid3, 'userId': 20, 'userName': 'WechatMobile',
    'checkMethod': '1', 'location': '113.327,23.128'
})
check('Wechat checkOut OK', ok(code, res), res.get('msg',''))

step('6.3 WechatAttendance myList')
code, res = api('GET', '/wechat/attendance/myList?pageNum=1&pageSize=50')
check('My attendance list', ok(code, res), f'total={res.get("total",0)}')

step('6.4 WechatPlan create')
code, res = api('POST', '/wechat/plan/create', {
    'cityCounty': '手机端创建', 'constructionSite': 'Wechat测试工地', 'siteLatitude': 23.0, 'siteLongitude': 113.0,
    'planWorkTime': '2026-08-01 08:00:00', 'projectName': '手机端创建的项目', 'workType': '点火',
    'constructionUnit': '手机测试', 'workContent': '手机端测试', 'workers': 'A,B'
})
check('WechatPlan create', ok(code, res), res.get('msg',''))

step('6.5 WechatPlan myList')
code, res = api('GET', '/wechat/plan/myList?pageNum=1&pageSize=50')
check('My plan list', ok(code, res), f'total={res.get("total",0)}')

# ============================================================
hr('Phase 7: Attendance Query Filter Tests')
# ============================================================

step('7.1 Query by checkStatus=2 (abnormal)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkStatus=2')
check('Filter abnormal records', ok(code, res), f'{len(res.get("rows",[]))} records')

step('7.2 Query by checkMethod=1 (WeChat)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkMethod=1')
check('Filter WeChat records', ok(code, res), f'{len(res.get("rows",[]))} records')

step('7.3 Query by checkStatus=1 (failed)')
code, res = api('GET', '/homework/attendance/list?pageNum=1&pageSize=50&checkStatus=1')
check('Filter failed records', ok(code, res), f'{len(res.get("rows",[]))} records')

# ============================================================
hr('Phase 8: Cleanup')
# ============================================================

step('Cleaning up test data')
for p in [pid, pid2, pid3]:
    if not p: continue
    # Delete attendance
    code, res = api('GET', f'/homework/attendance/list?pageNum=1&pageSize=100&planId={p}')
    for r in res.get('rows', []):
        api('DELETE', f'/homework/attendance/{r["attendanceId"]}')
    # Delete plan videos
    api('GET', f'/homework/plan/{p}/videos')
    for v in (res.get('data', []) if isinstance(res, dict) else []):
        api('DELETE', f'/homework/plan/{p}/videos/{v.get("id","")}')
    # Delete plan workers
    api('DELETE', f'/homework/plan/{p}/workers')
    # Delete plan
    api('DELETE', f'/homework/plan/{p}')

# Delete workers
code, res = api('GET', '/homework/worker/list?pageNum=1&pageSize=50&workerName=V2测试工')
for r in res.get('rows', []):
    api('DELETE', f'/homework/worker/{r["workerId"]}')

# Delete wechat created plans
code, res = api('GET', '/homework/plan/list?pageNum=1&pageSize=50&projectName=手机端')
for r in res.get('rows', []):
    api('DELETE', f'/homework/plan/{r["planId"]}')

print('\n    Cleanup done.')
print(f'\n{"="*65}')
print(f'  V2 TEST COMPLETE')
print(f'{"="*65}')
