package com.zm.docmind.security;

import com.zm.docmind.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 从 Authorization 请求头中提取 Bearer JWT 令牌并验证；
 * 认证通过后以用户标识（sys_user.id 的字符串形式）作为 SecurityContext 的 principal。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        // 单次解析：解析失败（非法/过期/篡改）直接放行，交由 Security 鉴权链处理 401
        if (token != null) {
            jwtService.parse(token).ifPresent(claims -> {
                String userId = claims.getSubject();
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 不跳过异步 dispatch，确保 SSE 等异步场景也能正确认证
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * 从 Authorization 请求头中提取 Bearer 令牌
     */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
