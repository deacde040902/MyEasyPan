package com.easypan.annotation;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.exception.BusinessException;
import com.easypan.redis.RedisUtils;
import com.easypan.utils.StringTools;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 基于AOP的全局拦截器（仅对加了@GlobalInterceptor的方法生效）
 */
@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {
    @Resource
    private RedisUtils redisUtils;

    private static final Logger log = LoggerFactory.getLogger(GlobalOperationAspect.class);

    @Before("@annotation(com.easypan.annotation.GlobalInterceptor)")
    public void intercept(JoinPoint point) {
        try{
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if(interceptor == null){
                return;
            }
            if (interceptor.checkLogin() || interceptor.checkAdmin()){
                checkLogin(interceptor.checkAdmin());
            }
        }catch (BusinessException e){
            log.error("全局拦截异常",e);
            throw e;
        } catch (Throwable e){
            log.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private void checkLogin(Boolean checkAdmin) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("token");
        if(StringTools.isEmpty(token)){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
        if(tokenUserInfoDto == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        if (checkAdmin && !tokenUserInfoDto.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_403);
        }
    }
}