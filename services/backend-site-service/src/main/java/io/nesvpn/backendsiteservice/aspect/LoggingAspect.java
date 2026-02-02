package io.nesvpn.backendsiteservice.aspect;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @PostConstruct
    public void init() {
        log.info("LoggingAspect bean init: {}", System.identityHashCode(this));
    }

    @Pointcut("execution(* io.kemalthes.vpnservice..*(..))")
    public void isPackage() {
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void isController() {
    }

    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void isService() {
    }

    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void isRepository() {
    }

    @Pointcut("this(org.springframework.data.repository.CrudRepository)")
    public void isCrudRepository() {
    }

    @Around("isPackage() && isService()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        log.debug("SERVICE START {} args={}", method, Arrays.toString(args));
        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            log.debug("SERVICE FINISH {} -> {} in {}ms", method, shortResult(result), time);
            return result;
        } catch (Throwable ex) {
            log.error("SERVICE ERROR {} failed: {}", method, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Around("isPackage() && (isCrudRepository() || isRepository())")
    public Object logRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        log.debug("REPOSITORY START {} args={}", method, Arrays.toString(args));
        try {
            Object result = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            log.debug("REPOSITORY FINISH {} -> {} in {}ms", method, shortResult(result), time);
            return result;
        } catch (Throwable ex) {
            log.error("REPOSITORY ERROR {} failed: {}", method, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Around("isPackage() && isController()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        log.info("CONTROLLER START {} args={}", method, Arrays.toString(args));
        try {
            Object result = joinPoint.proceed(args);
            long time = System.currentTimeMillis() - start;
            log.info("CONTROLLER FINISH {} result={} in {} ms", method, result, time);
            return result;
        } catch (Throwable e) {
            log.error("CONTROLLER ERROR {} exception={}", method, e.getMessage(), e);
            throw e;
        }
    }

    private String shortResult(Object result) {
        if (result == null) return "null";
        if (result instanceof Collection<?> col) return "Collection(size=" + col.size() + ")";
        return result.toString();
    }
}