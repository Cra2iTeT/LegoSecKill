package com.Cra2iTeT;

import cn.hutool.Hutool;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.Cra2iTeT.entity.pojo.Goods;
import com.Cra2iTeT.entity.pojo.Order;
import com.Cra2iTeT.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class MiaoshaqianggouApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderService orderService;

    @Test
    void contextLoads() {
        for (int i = 0; i < 10; i++) {
            System.out.println(IdUtil.getSnowflakeNextId());
        }
    }

    @Test
    void test1() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.set(2023, Calendar.JANUARY, 26, 18, 0);
        Date time = calendar.getTime();
        Date now = new Date();
        System.out.println(time);
        System.out.println(now);
        Goods goods = new Goods();
        goods.setStartTime(time);
        System.out.println(now.before(time));
        System.out.println(DateUtil.betweenMs(now, time));
    }

    @Test
    void test2() {
        stringRedisTemplate.opsForValue().set("orderNo:" + 8, "test", 8, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set("orderNo:" + 9, "test", 8, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set("orderNo:" + 10, "test", 8, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set("orderNo:" + 11, "test", 8, TimeUnit.SECONDS);
    }

    @Test
    void test3() {
        String test = "sss:xxx";
        System.out.println(test.split(":")[0]);
        System.out.println(test.split(":")[1]);
    }

    @Test
    void test4() {
    }

}
