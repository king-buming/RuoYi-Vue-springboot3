import socket
import json
import urllib.request
import re

# Step 1: Get captcha UUID
print("=== Login Test ===")
req = urllib.request.Request('http://localhost:8080/captchaImage')
resp = urllib.request.urlopen(req)
captcha_data = json.loads(resp.read())
uuid = captcha_data['uuid']

# Step 2: Get captcha code from Redis
s = socket.socket()
s.settimeout(5)
s.connect(('172.25.157.5', 6379))
s.send(f'GET captcha_codes:{uuid}\r\n'.encode())
resp = b''
while True:
    try:
        chunk = s.recv(4096)
        if not chunk: break
        resp += chunk
        if resp.endswith(b'\r\n'): break
    except: break
s.close()
match = re.match(rb'\$(\d+)\r\n(.+)\r\n', resp, re.DOTALL)
raw_value = match.group(2).decode() if match else ''
captcha = raw_value.strip('"')  # Remove JSON wrapping quotes
print(f'Captcha: {captcha}')

# Step 3: Login
login_data = json.dumps({
    'username': 'admin',
    'password': 'admin123',
    'code': captcha,
    'uuid': uuid
}).encode()
req = urllib.request.Request('http://localhost:8080/login', data=login_data, headers={'Content-Type': 'application/json'})
resp = urllib.request.urlopen(req)
result = json.loads(resp.read())
token = result.get('token', '')
if token:
    with open('C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/test_token.txt', 'w') as f:
        f.write(token)
    print(f'Login SUCCESS, token saved')
else:
    print(f'Login FAILED: {result.get("msg")}')
