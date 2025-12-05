package com.dwinovo.ling_cloud.aop;

import com.dwinovo.ling_cloud.annotation.OperationLog;
import com.dwinovo.ling_cloud.service.LogService;
import com.dwinovo.ling_cloud.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Order(10)
public class OperationLogAspect {

    @Autowired
    private LogService logService;

    @Autowired
    private JwtUtil jwtUtil;

    @Around("@annotation(operationLog)")
    public Object handleLog(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        String action = operationLog.value();
        String status = "SUCCESS";
        HttpServletRequest request = currentRequest();
        String ip = request != null ? request.getRemoteAddr() : null;
        String userId = null;
        String username = null;
        if (request != null) {
            String token = jwtUtil.extractTokenFromCookies(request);
            if (token != null) {
                try {
                    Claims claims = jwtUtil.parse(token);
                    userId = claims.getSubject();
                    Object nickname = claims.get("nickname");
                    Object uname = claims.get("username");
                    if (nickname != null) {
                        username = nickname.toString();
                    } else if (uname != null) {
                        username = uname.toString();
                    }
                } catch (Exception ignored) { }
            }
        }

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            status = "FAIL";
            throw ex;
        } finally {
            logService.record(userId, username, action, status, ip);
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
