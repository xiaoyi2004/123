package org.example.onlineexam.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.example.onlineexam.entity.OperationLog;
import org.example.onlineexam.entity.User;
import org.example.onlineexam.repository.OperationLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    private final OperationLogRepository logRepository;

    public LogAspect(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    // 切点：除了登录注册等认证接口，其他所有controller方法
    @Pointcut("execution(* org.example.onlineexam.controller.*.*(..)) && !execution(* org.example.onlineexam.controller.AuthController.*(..))")
    public void controllerMethods() {}

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logSuccess(JoinPoint jp, Object result) {
        saveLog(jp, 1);
    }

    @AfterThrowing(pointcut = "controllerMethods()", throwing = "e")
    public void logError(JoinPoint jp, Throwable e) {
        saveLog(jp, 0);
    }

    private void saveLog(JoinPoint jp, int result) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);
            User user = session != null ? (User) session.getAttribute("user") : null;

            OperationLog log = new OperationLog();
            if (user != null) {
                log.setUserId(user.getId());
                log.setUsername(user.getUsername());
            }
            String path = request.getRequestURI();
            log.setRequestPath(path);
            // 模块取路径第一段
            String[] segments = path.split("/");
            log.setModule(segments.length > 1 ? segments[1] : "unknown");
            log.setAction(jp.getSignature().getName());
            log.setRequestParams(request.getQueryString());
            log.setIp(request.getRemoteAddr());
            log.setResult(result);
            log.setCreatedAt(LocalDateTime.now());
            logRepository.save(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响业务
        }
    }
}