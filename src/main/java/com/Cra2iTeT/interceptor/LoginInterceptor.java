package com.Cra2iTeT.interceptor;

import com.Cra2iTeT.entity.vo.UserVo;
import com.Cra2iTeT.server.UserInfoThread;
import com.alibaba.fastjson.JSON;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 19:57
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        String token = request.getHeader("token");
        String userVoValue = stringRedisTemplate.opsForValue().get("user:login:token:" + token);
        if (StringUtils.isEmpty(userVoValue)) {
            response.setStatus(401);
            response.setContentType("Application/json;charset=utf-8");
            return false;
        }
        UserVo userVo = JSON.parseObject(userVoValue, UserVo.class);
        UserInfoThread.put(userVo);
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request,
                                @NotNull HttpServletResponse response,
                                @NotNull Object handler, Exception ex) {
        UserInfoThread.remove();
    }
}
