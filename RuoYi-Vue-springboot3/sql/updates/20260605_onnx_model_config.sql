-- =============================================================
-- 变更说明：AI算法仓 —— ONNX 真实模型文件路径配置
-- 依据    ：YOLOv12 + ArcFace ONNX 模型 Java 集成
-- 模型文件：yolov12m-face.onnx（人脸检测）+ w600k_mbf.onnx（人脸特征提取）
-- 部署路径：项目根目录/ai-models/
-- 日期    ：2026-06-05
-- 执行    ：mysql -u <本地账号> -p ry-vue < 本文件
-- =============================================================
USE `ry-vue`;

-- 1. 人脸检测模型（YOLOv12）—— 后台管理页面 model_code: yolo_v8_obj_001
UPDATE ai_model SET
  model_path = 'C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/RuoYi-Vue-springboot3/ai-models/yolov12m-face.onnx',
  framework = 'onnx',
  version = 'v12-m',
  config_json = '{"confidence_threshold":0.5,"iou_threshold":0.45,"input_size":"640x640"}',
  metrics = '{"mAP50":0.92,"fps":45}',
  status = '1'
WHERE model_code = 'yolo_v8_obj_001';

-- 2. 人脸特征提取模型（ArcFace w600k_mbf）—— 后台管理页面 model_code: arcface_r100_001
UPDATE ai_model SET
  model_path = 'C:/Users/ASUS/Desktop/RuoYi-Vue-springboot3-main/RuoYi-Vue-springboot3/ai-models/w600k_mbf.onnx',
  framework = 'onnx',
  version = 'w600k_mbf',
  config_json = '{"embedding_dim":512,"similarity_threshold":0.35,"input_size":"112x112"}',
  metrics = '{"accuracy":0.965,"far@1e-4":0.001}',
  status = '1'
WHERE model_code = 'arcface_r100_001';
