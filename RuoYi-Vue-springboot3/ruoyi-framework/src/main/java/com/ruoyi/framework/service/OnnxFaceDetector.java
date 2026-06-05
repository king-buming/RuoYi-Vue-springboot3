package com.ruoyi.framework.service;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

public class OnnxFaceDetector implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;
    private final float confThreshold;
    private final float iouThreshold;
    private final int inputW, inputH;

    public OnnxFaceDetector(String modelPath, float confThreshold, float iouThreshold)
            throws OrtException {
        this.confThreshold = confThreshold;
        this.iouThreshold = iouThreshold;
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
        TensorInfo info = (TensorInfo) session.getInputInfo().get(inputName).getInfo();
        long[] shape = info.getShape();
        this.inputH = (int) shape[2];
        this.inputW = (int) shape[3];
    }

    public List<float[]> detect(File imageFile) throws IOException, OrtException {
        ImageUtils.BgrImage img = ImageUtils.readImageBGR(imageFile);
        return detect(img.pixels, img.h, img.w);
    }

    public List<float[]> detect(byte[] bgr, int imgH, int imgW) throws OrtException {
        LetterboxResult lb = letterbox(bgr, imgH, imgW);

        long[] inputShape = {1, 3, inputH, inputW};
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(lb.data), inputShape)) {
            OrtSession.Result result = session.run(
                Collections.singletonMap(inputName, tensor));

            float[][][] raw = (float[][][]) result.get(0).getValue();
            float[][] detections = raw[0];

            return postprocess(detections, lb.scale, lb.padX, lb.padY, imgW, imgH);
        }
    }

    private static class LetterboxResult {
        float[] data;
        float scale;
        int padX, padY;
    }

    private LetterboxResult letterbox(byte[] bgr, int imgH, int imgW) {
        float scale = Math.min((float) inputW / imgW, (float) inputH / imgH);
        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);

        byte[] resized = ImageUtils.resizeBGR(bgr, imgH, imgW, newW, newH);

        int padX = (inputW - newW) / 2;
        int padY = (inputH - newH) / 2;

        float[] data = new float[3 * inputH * inputW];
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < inputH; y++) {
                for (int x = 0; x < inputW; x++) {
                    int nchwIdx = c * inputH * inputW + y * inputW + x;
                    if (y >= padY && y < padY + newH && x >= padX && x < padX + newW) {
                        int srcIdx = ((y - padY) * newW + (x - padX)) * 3 + c;
                        data[nchwIdx] = (resized[srcIdx] & 0xFF) / 255.0f;
                    } else {
                        data[nchwIdx] = 114.0f / 255.0f;
                    }
                }
            }
        }

        LetterboxResult r = new LetterboxResult();
        r.data = data;
        r.scale = scale;
        r.padX = padX;
        r.padY = padY;
        return r;
    }

    private List<float[]> postprocess(float[][] detections, float scale,
                                       int padX, int padY, int imgW, int imgH) {
        List<float[]> faces = new ArrayList<>();

        for (float[] det : detections) {
            float conf = det[4];
            if (conf < confThreshold) continue;

            float x1 = Math.max(0, (det[0] - padX) / scale);
            float y1 = Math.max(0, (det[1] - padY) / scale);
            float x2 = Math.min(imgW, (det[2] - padX) / scale);
            float y2 = Math.min(imgH, (det[3] - padY) / scale);

            if (x2 > x1 && y2 > y1) {
                faces.add(new float[]{x1, y1, x2, y2, conf});
            }
        }
        return nms(faces);
    }

    private List<float[]> nms(List<float[]> faces) {
        if (faces.size() <= 1) return faces;

        faces.sort((a, b) -> Float.compare(b[4], a[4]));

        List<float[]> kept = new ArrayList<>();
        boolean[] suppressed = new boolean[faces.size()];

        for (int i = 0; i < faces.size(); i++) {
            if (suppressed[i]) continue;
            kept.add(faces.get(i));
            for (int j = i + 1; j < faces.size(); j++) {
                if (suppressed[j]) continue;
                if (iou(faces.get(i), faces.get(j)) > iouThreshold) {
                    suppressed[j] = true;
                }
            }
        }
        return kept;
    }

    private float iou(float[] a, float[] b) {
        float ix1 = Math.max(a[0], b[0]);
        float iy1 = Math.max(a[1], b[1]);
        float ix2 = Math.min(a[2], b[2]);
        float iy2 = Math.min(a[3], b[3]);
        if (ix2 <= ix1 || iy2 <= iy1) return 0;
        float inter = (ix2 - ix1) * (iy2 - iy1);
        float areaA = (a[2] - a[0]) * (a[3] - a[1]);
        float areaB = (b[2] - b[0]) * (b[3] - b[1]);
        return inter / (areaA + areaB - inter);
    }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
