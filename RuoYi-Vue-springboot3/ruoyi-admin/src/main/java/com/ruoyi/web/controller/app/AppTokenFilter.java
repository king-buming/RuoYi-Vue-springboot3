package com.ruoyi.web.controller.app;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * /app/* 请求统一校验 appToken，未登录返回 401。
 * 允许 /app/auth/login 和 /app/common/dicts 匿名访问。
 */
public class AppTokenFilter implements Filter
{
    private static final String[] ANON_PATHS = {"/app/auth/login", "/app/common/dicts", "/app/common/"};

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException
    {
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();
        // 匿名路径放行
        for (String p : ANON_PATHS) {
            if (path.startsWith(p)) { chain.doFilter(req, res); return; }
        }
        // 校验 appToken
        Long wid = AppTokenUtil.getWorkerId(request);
        if (wid == null) {
            HttpServletResponse resp = (HttpServletResponse) res;
            resp.setStatus(401);
            resp.setContentType("application/json;charset=utf-8");
            resp.getWriter().write("{\"code\":401,\"msg\":\"未登录或token已过期\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
