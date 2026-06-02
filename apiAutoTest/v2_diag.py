"""Quick diagnostic for failing V2 endpoints"""
import json, urllib.request, urllib.error, redis, time

BASE = 'http://localhost:8080'
r = redis.Redis(host='172.25.157.5', port=6379, decode_responses=True)

# Get token
resp = json.loads(urllib.request.urlopen(urllib.request.Request(BASE + '/captchaImage')).read())
code = r.get(f'captcha_codes:{resp["uuid"]}').strip('"')
data = json.dumps({'username':'admin','password':'admin123','code':code,'uuid':resp['uuid']}).encode()
token = json.loads(urllib.request.urlopen(urllib.request.Request(BASE+'/login',data=data,headers={'Content-Type':'application/json'})).read())['token']
print(f'Token OK: {token[:20]}...')

def api(method, path, data=None):
    url = f'{BASE}{path}'
    h = {'Content-Type':'application/json','Authorization':f'Bearer {token}'}
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        resp = urllib.request.urlopen(req)
        return resp.getcode(), json.loads(resp.read())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        return e.code, body

# ---- Workers test ----
print('\n=== Workers endpoints ===')
code, res = api('POST', '/homework/plan/1/workers', [{'workerId':48,'workerName':'Test','roleType':'9'}])
print(f'POST /plan/1/workers: code={code}')
print(f'  Response: {res}')

code, res = api('GET', '/homework/plan/1/workers')
print(f'GET /plan/1/workers: code={code}')
print(f'  Response: {json.dumps(res,ensure_ascii=False) if isinstance(res,dict) else res}')

# Cleanup
code, res = api('DELETE', '/homework/plan/1/workers')
print(f'DELETE /plan/1/workers: code={code}')
print(f'  Response: {json.dumps(res,ensure_ascii=False) if isinstance(res,dict) else res}')

# ---- Video test ----
print('\n=== Video endpoints ===')
code, res = api('POST', '/homework/plan/1/videos', {'recordId':1001,'recordName':'test.mp4','startTime':'2026-07-01 08:00:00','endTime':'2026-07-01 10:00:00'})
print(f'POST /plan/1/videos: code={code}')
print(f'  Response: {json.dumps(res,ensure_ascii=False) if isinstance(res,dict) else res}')

code, res = api('GET', '/homework/plan/1/videos')
print(f'GET /plan/1/videos: code={code}')
print(f'  Response: {json.dumps(res,ensure_ascii=False) if isinstance(res,dict) else res}')
