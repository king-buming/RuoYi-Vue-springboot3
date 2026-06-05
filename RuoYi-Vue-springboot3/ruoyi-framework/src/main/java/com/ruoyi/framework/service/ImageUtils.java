package com.ruoyi.framework.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageUtils {

    public static class BgrImage {
        public final byte[] pixels; // length = h * w * 3
        public final int h, w;

        public BgrImage(byte[] pixels, int h, int w) {
            this.pixels = pixels;
            this.h = h;
            this.w = w;
        }
    }

    /**
     * 从文件读取图像，返回 flat BGR 格式（与 Python cv2.imread 一致）
     */
    public static BgrImage readImageBGR(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("无法读取图像: " + file.getPath());
        int h = img.getHeight(), w = img.getWidth();
        byte[] pixels = new byte[h * w * 3];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int idx = (y * w + x) * 3;
                pixels[idx]     = (byte) (rgb & 0xFF);         // Blue
                pixels[idx + 1] = (byte) ((rgb >> 8) & 0xFF);  // Green
                pixels[idx + 2] = (byte) ((rgb >> 16) & 0xFF); // Red
            }
        }
        return new BgrImage(pixels, h, w);
    }

    /**
     * 双线性插值缩放 BGR 图像
     */
    public static byte[] resizeBGR(byte[] src, int srcH, int srcW, int dstW, int dstH) {
        byte[] dst = new byte[dstH * dstW * 3];
        float scaleX = (float) srcW / dstW;
        float scaleY = (float) srcH / dstH;

        for (int y = 0; y < dstH; y++) {
            float srcY = y * scaleY;
            int y0 = (int) srcY;
            int y1 = Math.min(y0 + 1, srcH - 1);
            float dy = srcY - y0;

            for (int x = 0; x < dstW; x++) {
                float srcX = x * scaleX;
                int x0 = (int) srcX;
                int x1 = Math.min(x0 + 1, srcW - 1);
                float dx = srcX - x0;

                int dstIdx = (y * dstW + x) * 3;
                for (int c = 0; c < 3; c++) {
                    int i00 = (y0 * srcW + x0) * 3 + c;
                    int i01 = (y0 * srcW + x1) * 3 + c;
                    int i10 = (y1 * srcW + x0) * 3 + c;
                    int i11 = (y1 * srcW + x1) * 3 + c;

                    float top  = lerp(src[i00] & 0xFF, src[i01] & 0xFF, dx);
                    float bot  = lerp(src[i10] & 0xFF, src[i11] & 0xFF, dx);
                    dst[dstIdx + c] = (byte) Math.round(lerp(top, bot, dy));
                }
            }
        }
        return dst;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
