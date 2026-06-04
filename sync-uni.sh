#!/bin/bash
# Windows HBuilderX 改完页面后运行这个脚本同步回 WSL
cp /mnt/c/Users/1/Desktop/RuoYi-App/pages/worker/*.vue /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-App/pages/worker/
cp /mnt/c/Users/1/Desktop/RuoYi-App/config.js /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-App/config.js
cp /mnt/c/Users/1/Desktop/RuoYi-App/App.vue /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-App/App.vue
cp /mnt/c/Users/1/Desktop/RuoYi-App/utils/*.js /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-App/utils/
cp /mnt/c/Users/1/Desktop/RuoYi-App/permission.js /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-App/permission.js
echo "✅ uni-app 改动已同步回 WSL git 仓库"
cp /mnt/c/Users/1/Desktop/RuoYi-App/开发日志.md /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-App/开发日志.md
cp /home/zzz/project/RuoYi-Vue-springboot3/RuoYi-Vue-springboot3/ruoyi-ui/public/mobile.html /mnt/c/Users/1/Desktop/RuoYi-App/static/index.html
