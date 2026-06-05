package com.ruoyi.framework.service;

import java.nio.FloatBuffer;
import java.util.Collections;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class OnnxFaceEmbedding implements AutoCloseable {

    private static final int INPUT_SIZE = 112;
    private static final int EMBEDDING_DIM = 512;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final String inputName;

    public OnnxFaceEmbedding(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
    }

    /**
     * 提取 512 维特征向量（已 L2 归一化）
     */
    public float[] extract(byte[] bgr, int imgH, int imgW) throws OrtException {
        float[] blob = preprocess(bgr, imgH, imgW);

        long[] inputShape = {1, 3, INPUT_SIZE, INPUT_SIZE};
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(blob), inputShape)) {
            OrtSession.Result result = session.run(
                Collections.singletonMap(inputName, tensor));

            float[][] raw = (float[][]) result.get(0).getValue();
            float[] embedding = raw[0];

            l2Normalize(embedding);
            return embedding;
        }
    }

    /**
     * BGR flat → resize 112×112 → BGR→RGB → normalize [-1,1] → NCHW float[]
     */
    private float[] preprocess(byte[] bgr, int srcH, int srcW) {
        byte[] resized = ImageUtils.resizeBGR(bgr, srcH, srcW, INPUT_SIZE, INPUT_SIZE);

        float[] data = new float[3 * INPUT_SIZE * INPUT_SIZE];
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int idx = (y * INPUT_SIZE + x) * 3;
                float r = norm(resized[idx + 2] & 0xFF);
                float g = norm(resized[idx + 1] & 0xFF);
                float b = norm(resized[idx]     & 0xFF);

                int base = y * INPUT_SIZE + x;
                data[0 * INPUT_SIZE * INPUT_SIZE + base] = r;
                data[1 * INPUT_SIZE * INPUT_SIZE + base] = g;
                data[2 * INPUT_SIZE * INPUT_SIZE + base] = b;
            }
        }
        return data;
    }

    private static float norm(int pixel) { return (pixel - 127.5f) / 127.5f; }

    private static void l2Normalize(float[] v) {
        float sum = 0;
        for (float x : v) sum += x * x;
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }

    public int getEmbeddingDim() { return EMBEDDING_DIM; }

    @Override
    public void close() throws OrtException {
        session.close();
    }
}
