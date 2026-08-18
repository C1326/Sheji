package com.sheji.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sheji.common.JwtUtil;
import com.sheji.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    // 用于将对象转换为 JSON 字符串
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 判断路径是否需要登录
     */
    private boolean needLogin(String path) {
        // 1. 明确放行的公开接口 (登录、注册)
        // 使用 equals 或更精确的匹配，防止 /user/login 匹配到了 /user/loginLog 之类
        if ("/user/login".equals(path) || "/user/register".equals(path)) {
            return false;
        }

        // 2. 需要登录的路径前缀
        return path.startsWith("/user/info")

                || path.startsWith("/user/update")
                || path.startsWith("/user/avatar")
                || path.startsWith("/diary/add")
                || path.startsWith("/diary/update")
                || path.startsWith("/diary/delete")
                || path.startsWith("/diary/my")
                || path.startsWith("/media/upload");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求直接放行，避免 OPTIONS 请求被登录校验拦截导致跨域失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // 去掉 context-path (如果有)
        String ctx = request.getContextPath();
        if (StringUtils.hasText(ctx) && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        // 公开接口也尝试解析 token：
        // 这样详情、下载等"公开或本人"类的接口能识别登录用户（本人访问私有资源时用得上）
        resolveUserFromToken(request);

        // 如果不需要登录，直接放行（已尝试写入用户上下文）
        if (!needLogin(path)) {
            return true;
        }

        // --- 需要登录的逻辑 ---

        String auth = request.getHeader("Authorization");

        // 情况1：没有 Token 或格式不对
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            // 【关键修改】不要 throw 异常，而是手动写入 JSON 响应
            writeErrorResponse(response, 401, "请先登录");
            return false; // 返回 false 终止请求
        }

        String token = auth.substring(7);

        // 情况2：Token 过期或无效
        if (jwtUtil.isExpired(token)) {
            writeErrorResponse(response, 401, "登录已过期，请重新登录");
            return false;
        }

        try {
            Long userId = jwtUtil.parseUserId(token);
            UserContext.setUserId(userId);
        } catch (Exception e) {
            // 解析失败（比如 Token 被篡改）
            writeErrorResponse(response, 401, "无效的 Token");
            return false;
        }

        return true;
    }

    /**
     * 从请求头解析 token 并写入用户上下文；token 缺失或无效时静默忽略（当作未登录），不拦截请求。
     */
    private void resolveUserFromToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            if (!jwtUtil.isExpired(token)) {
                try {
                    UserContext.setUserId(jwtUtil.parseUserId(token));
                } catch (Exception ignored) {
                    // token 无效时忽略，当作未登录
                }
            }
        }
    }

    /**
     * 辅助方法：手动写入 JSON 格式的错误信息
     */
    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code); // 设置 HTTP 状态码为 401

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);

        // 将 Map 转为 JSON 写入 response
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
