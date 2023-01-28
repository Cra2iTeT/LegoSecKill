package com.Cra2iTeT.service.impl;

import com.Cra2iTeT.entity.pojo.User;
import com.Cra2iTeT.mapper.UserMapper;
import com.Cra2iTeT.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:12
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{
}
