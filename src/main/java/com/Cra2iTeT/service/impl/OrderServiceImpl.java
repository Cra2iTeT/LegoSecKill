package com.Cra2iTeT.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.Cra2iTeT.entity.pojo.Order;
import com.Cra2iTeT.entity.vo.GoodsVo;
import com.Cra2iTeT.entity.vo.OrderVo;
import com.Cra2iTeT.mapper.OrderMapper;
import com.Cra2iTeT.service.OrderService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:13
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    public OrderServiceImpl(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    /**
     * 自动取消超时未支付订单
     *
     * @param orderId
     */
    @Override
    public void cancelOrder(Long orderId) {
        String orderValue = stringRedisTemplate.opsForValue().getAndDelete("order:" + orderId);
        if (StringUtils.isEmpty(orderValue)) {
            throw new RuntimeException("系统故障");
        }
        RLock orderLock = redissonClient.getLock("orderLock" + orderId);
        orderLock.lock();
        Order order = JSON.parseObject(orderValue, Order.class);
        order.setOrderStatus(2);
        try {
            String goodsVoValue = String.valueOf(stringRedisTemplate.opsForHash()
                    .get("goods:secKill", String.valueOf(order.getGoodsId())));
            cancelOrder(order, JSON.parseObject(goodsVoValue, GoodsVo.class).getToken());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringRedisTemplate.opsForHash().delete("goodsSecKillMap:" + order.getGoodsId(),
                    String.valueOf(order.getUserId()));
            unlock(orderLock);
        }
    }

    @Override
    public void cancelOrder(Order order, String goodsToken) {
        OrderVo orderVo = BeanUtil.copyProperties(order, OrderVo.class);
        RLock orderCancelLock = redissonClient.getLock("orderOperateLock");
        orderCancelLock.lock();
        RLock stockLock = redissonClient.getLock("goodsStockLock" + orderVo.getGoodsId());
        stockLock.lock();
        try {
            // 超过15分钟未支付的订单会被消息队列监听器监听到后保存进数据库，这里操作不会重复保存订单
            save(order);
            Integer stock = Integer.valueOf((String) Objects.requireNonNull(stringRedisTemplate
                    .opsForHash().get("goods:stock", goodsToken)));
            stock += 1;
            stringRedisTemplate.opsForHash().put("goods:stock", goodsToken, String.valueOf(stock));
            // 订单已取消结果缓存15分钟
            stringRedisTemplate.opsForValue().set("order:" + order.getOrderId(),
                    JSON.toJSONString(orderVo), 15, TimeUnit.MINUTES);
            Long userId = order.getUserId();
            RLock isSecKillRepeatLock = redissonClient.getLock("goodsSecKillRepeat" + order.getGoodsId() +
                    userId);
            isSecKillRepeatLock.lock();
            stringRedisTemplate.opsForHash().delete("goodsSecKillMap:" + order.getGoodsId(),
                    String.valueOf(userId));
            unlock(isSecKillRepeatLock);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            unlock(stockLock);
            unlock(orderCancelLock);
        }
    }

    private void unlock(RLock rLock) {
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlock();
        }
    }
}
