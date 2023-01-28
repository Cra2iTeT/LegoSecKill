package com.Cra2iTeT.server;

import com.Cra2iTeT.entity.vo.UserVo;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 18:00
 */
public class UserInfoThread {
    private UserInfoThread() {
    }

    private static final ThreadLocal<UserVo> USER_VO_THREAD_LOCAL = new ThreadLocal<>();

    public static void put(UserVo userVo) {
        USER_VO_THREAD_LOCAL.set(userVo);
    }

    public static UserVo get() {
        return USER_VO_THREAD_LOCAL.get();
    }

    public static void remove() {
        USER_VO_THREAD_LOCAL.remove();
    }
}
