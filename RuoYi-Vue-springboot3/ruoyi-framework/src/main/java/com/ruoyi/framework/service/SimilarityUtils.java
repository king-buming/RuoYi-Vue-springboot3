package com.ruoyi.framework.service;

public class SimilarityUtils {

    /**
     * 两个已 L2 归一化的向量 → 余弦相似度 ∈ [-1, 1]
     */
    public static float cosine(float[] a, float[] b) {
        float dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot;
    }
}
