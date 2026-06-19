package io.oddsmaker.control.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全头过滤器
 * 添加额外的安全响应头
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 添加安全头
        addSecurityHeaders(httpRequest, httpResponse);

        // 继续过滤链
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁
    }

    /**
     * 添加安全响应头
     */
    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        // 防止点击劫持
        response.setHeader("X-Frame-Options", "DENY");

        // 防止MIME类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");

        // XSS防护
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 严格传输安全（HSTS）
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // Referrer策略
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 权限策略
        response.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=(), payment=(), usb=()");

        // 缓存控制（API响应不缓存）
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        // 移除服务器信息
        response.setHeader("Server", "");
        response.setHeader("X-Powered-By", "");

        // 内容安全策略
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "frame-ancestors 'none'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https:;");

        // 跨域策略
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
    }
}
