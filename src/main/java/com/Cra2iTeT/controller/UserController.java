package com.Cra2iTeT.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.Cra2iTeT.entity.pojo.User;
import com.Cra2iTeT.entity.to.LoginTo;
import com.Cra2iTeT.entity.vo.R;
import com.Cra2iTeT.entity.vo.UserVo;
import com.Cra2iTeT.service.UserService;
import com.Cra2iTeT.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:14
 */
@RestController
@Api("用户管理模块")
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final StringRedisTemplate stringRedisTemplate;

    private final UserService userService;

    public UserController(UserService userService, StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("/save")
    @ApiOperation("用户注册")
    public R saveUser(@ApiParam @RequestBody LoginTo loginTo) {
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(User::getPhone, loginTo.getPhone())
                .last("limit 1");
        User user = userService.getOne(userQueryWrapper);
        if (user != null) {
            return R.error("手机号已经注册");
        }
        user = new User();
        user.setUserId(IdUtil.getSnowflakeNextId());
        user.setPhone(loginTo.getPhone());
        user.setPwdMd5(loginTo.getPwdMd5());
        userService.save(user);
        return R.ok("用户注册成功");
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public R userLogin(@RequestBody LoginTo loginTo) {
        User user;
        String userValue = stringRedisTemplate.opsForValue().get("user:login:phone:"
                + loginTo.getPhone());
        if (userValue == null) {
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.eq(User::getPhone, loginTo.getPhone())
                    .last("limit 1");
            user = userService.getOne(userQueryWrapper);
            if (user == null) {
                stringRedisTemplate.opsForValue().set("user:login:phone:" + loginTo.getPhone(),
                        "", 5, TimeUnit.MINUTES);
                return R.error("用户不存在");
            }
        } else if ("".equals(userValue)) {
            return R.error("用户不存在");
        } else {
            user = JSON.parseObject(userValue, User.class);
        }
        if (!user.getPwdMd5().equals(loginTo.getPwdMd5())) {
            return R.error("用户名或密码错误");
        }
        String token = NumberUtil.genToken(user.getUserId());
        UserVo userVo = BeanUtil.copyProperties(user, UserVo.class);
        userVo.setToken(token);
        stringRedisTemplate.opsForValue().set("user:login:token:" + token, JSON.toJSONString(userVo),
                3, TimeUnit.DAYS);
        return R.ok(token);
    }
}
