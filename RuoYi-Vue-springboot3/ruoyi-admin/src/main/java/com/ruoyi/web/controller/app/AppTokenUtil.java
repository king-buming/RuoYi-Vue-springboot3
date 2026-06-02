package com.ruoyi.web.controller.app;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 手机端轻量 Token（HMAC-SHA256）。
 * 格式：base64(workerId:expireTime).hmac
 */
public class AppTokenUtil {

    private static final String SECRET = "SmartSite2026@App";
    private static final long TTL_MS = 7 * 24 * 3600_000L; // 7 天

    /** 签发 token */
    public static String create(Long workerId) {
        long expire = System.currentTimeMillis() + TTL_MS;
        String payload = workerId + ":" + expire;
        String b64 = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = hmac(b64);
        return b64 + "." + sig;
    }

    /** 验证 token，返回 workerId；无效返回 null */
    public static Long verify(String token) {
        if (token == null || !token.contains(".")) return null;
        try {
            String[] parts = token.split("\\.", 2);
            if (!hmac(parts[0]).equals(parts[1])) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] pair = payload.split(":");
            long expire = Long.parseLong(pair[1]);
            if (System.currentTimeMillis() > expire) return null;
            return Long.parseLong(pair[0]);
        } catch (Exception e) { return null; }
    }

    /** 从请求 Header 中提取并验证 token，返回 workerId */
    public static Long getWorkerId(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return verify(header.substring(7));
        }
        // 兼容 ?token=xxx 参数
        String param = req.getParameter("token");
        return param != null ? verify(param) : null;
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
