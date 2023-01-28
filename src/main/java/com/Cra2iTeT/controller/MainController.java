package com.Cra2iTeT.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.Cra2iTeT.entity.pojo.Goods;
import com.Cra2iTeT.entity.pojo.Order;
import com.Cra2iTeT.entity.to.GoodsTo;
import com.Cra2iTeT.entity.vo.GoodsVo;
import com.Cra2iTeT.entity.vo.OrderVo;
import com.Cra2iTeT.entity.vo.R;
import com.Cra2iTeT.server.UserInfoThread;
import com.Cra2iTeT.service.GoodsService;
import com.Cra2iTeT.service.OrderService;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 17:39
 */
@Api("秒杀系统主要功能模块")
@RestController
@RequestMapping("/main")
public class MainController {
    private final StringRedisTemplate stringRedisTemplate;

    private final OrderService orderService;

    private final RedissonClient redissonClient;

    private final GoodsService goodsService;

    public MainController(GoodsService goodsService,
                          OrderService orderService,
                          StringRedisTemplate stringRedisTemplate,
                          RedissonClient redissonClient) {
        this.goodsService = goodsService;
        this.orderService = orderService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    @PostMapping("/saveGoods")
    @ApiOperation("添加商品")
    public R saveGoods(@ApiParam @RequestBody GoodsTo goodsTo) {
        Date timeNow = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).getTime();
        if (timeNow.after(goodsTo.getStartTime()) ||
                timeNow.after(goodsTo.getEndTime()) ||
                goodsTo.getStartTime().after(goodsTo.getEndTime()) ||
                DateUtil.betweenMs(goodsTo.getStartTime(), goodsTo.getEndTime()) < 3600000) {
            return R.error("商品抢购时间格式不正确");
        }
        Goods goods = BeanUtil.copyProperties(goodsTo, Goods.class);
        goods.setGoodsId(IdUtil.getSnowflakeNextId());
        goodsService.save(goods);
        //  通过Redis发送一条死信队列，监听死信队列，
        //  死信队列监听器对Goods的抢购时间进行判断，不足一小时则将Goods信息(要多生成一个Token)
        //  存储到Redis中
        long timeBetweenMs = DateUtil.betweenMs(timeNow, goodsTo.getStartTime());
        // 新增的抢购商品开始抢购时间不足1小时
        stringRedisTemplate.opsForValue().set("secKill:info:" + goods.getGoodsId(),
                JSON.toJSONString(goods));
        if (timeBetweenMs < 3600000) {
            goodsService.genSecKillInfo(goods.getGoodsId());
        } else {
            timeBetweenMs -= 3600000;
            stringRedisTemplate.opsForValue().set("secKill:store:" + goods.getGoodsId(),
                    "", timeBetweenMs, TimeUnit.MILLISECONDS);
        }
        return R.ok("抢购商品保存成功");
    }

    @GetMapping("/getGoodsUrlToken/{goodsId}")
    @ApiOperation("获取抢购商品token")
    public R getGoodsUrlToken(@ApiParam @PathVariable("goodsId") Long goodsId) {
        String hashValue = String.valueOf(stringRedisTemplate.opsForHash().get("goods:secKill",
                String.valueOf(goodsId)));
        if (StringUtils.isEmpty(hashValue)) {
            return R.error("未到此商品抢购时间，请稍后重试");
        }
        GoodsVo goodsVo = JSON.parseObject(hashValue, GoodsVo.class);
        return R.ok(goodsVo.getToken());
    }

    @PostMapping("/secKill/{goodsId}/{goodsToken}")
    @ApiOperation("商品抢购")
    public R secKill(@ApiParam @PathVariable("goodsToken") String goodsToken,
                     @ApiParam @PathVariable("goodsId") Long goodsId) {
        // 验证商品抢购url的token是否存在且正确
        String hashValue = String.valueOf(stringRedisTemplate.opsForHash().get("goods:secKill",
                String.valueOf(goodsId)));
        if (StringUtils.isEmpty(hashValue)) {
            return R.error("抢购未开始");
        }
        GoodsVo goodsVo = JSON.parseObject(hashValue, GoodsVo.class);
        if (!goodsVo.getToken().equals(goodsToken) || !goodsVo.getGoodsId().equals(goodsId)) {
            return R.error("抢购链接有误");
        }
        Date now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).getTime();
        if (now.before(goodsVo.getStartTime()) || now.after(goodsVo.getEndTime())) {
            return R.error("当前商品不在抢购时效内");
        }
        Integer stock = Integer.valueOf((String) Objects.requireNonNull(stringRedisTemplate
                .opsForHash().get("goods:stock", goodsToken)));
        if (stock == 0) {
            return R.error("商品被抢购完了");
        }
        // 判断是否重复购买
        Long userId = UserInfoThread.get().getUserId();
        RLock isSecKillRepeatLock = redissonClient.getLock("goodsSecKillRepeat" + goodsId + userId);
        isSecKillRepeatLock.lock();
        boolean isSecKillRepeat = !StringUtils.isEmpty(stringRedisTemplate.
                opsForHash().get("goodsSecKillMap:" + goodsId, String.valueOf(userId)));
        if (isSecKillRepeat) {
            unlock(isSecKillRepeatLock);
            return R.error("无法重复购买此商品");
        }
        RLock stockLock = redissonClient.getLock("goodsStockLock" + goodsId);
        stockLock.lock();
        try {
            // 精确获取库存容量
            stock = Integer.valueOf((String) Objects.requireNonNull(stringRedisTemplate
                    .opsForHash().get("goods:stock", goodsToken)));
            if (stock == 0) {
                unlock(stockLock);
                unlock(isSecKillRepeatLock);
//                isSecKillRepeatLock.unlock();
                return R.error("商品被抢购完了");
            }
            stock = stock - 1;
            Order order = new Order();
            order.setOrderId(IdUtil.getSnowflakeNextId());
            order.setOrderStatus(0);
            order.setGoodsId(goodsId);
            order.setUserId(userId);
            // 存储订单
            stringRedisTemplate.opsForValue().set("order:" + order.getOrderId(), JSON.toJSONString(order));
            // 发送一条死信队列，监听器处理把订单添加到数据库（要判断订单状态)
            // 删减库存
            stringRedisTemplate.opsForValue().set("orderSeckill:overdue:" + order.getOrderId(), "",
                    17, TimeUnit.MINUTES);
            stringRedisTemplate.opsForHash().put("goods:stock", goodsToken, String.valueOf(stock));
            stringRedisTemplate.opsForHash().put("goodsSecKillMap:" + order.getGoodsId(),
                    String.valueOf(userId), "1");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            unlock(stockLock);
            unlock(isSecKillRepeatLock);
            // 释放锁
//            stockLock.unlock();
//            isSecKillRepeatLock.unlock();
        }
        return R.ok("商品抢购成功");
    }

    @PostMapping("/orderPay/{orderId}")
    @ApiOperation("订单支付")
    public R orderPay(@ApiParam @PathVariable("orderId") Long orderId) {
        RLock orderLock = redissonClient.getLock("orderLock" + orderId);
        orderLock.lock();
        String orderValue = stringRedisTemplate.opsForValue().get("order:" + orderId);
        OrderVo orderVo;
        if (StringUtils.isEmpty(orderValue)) {
            // 走到这里说明缓存中没有订单，则订单已经被手动取消或者超时未支付取消
            // 从数据库查一次
            Order order = orderService.getById(orderId);
            orderVo = BeanUtil.copyProperties(order, OrderVo.class);
            // 数据库中也不存在及订单不存在
            if (order == null) {
                // 空缓存五分钟
                stringRedisTemplate.opsForValue().set("order:" + orderId, "", 5, TimeUnit.MINUTES);
                unlock(orderLock);
                return R.error("订单不存在");
            }
            if (!order.getOrderId().equals(orderId)) {
                unlock(orderLock);
                return R.error("无法操作非本人订单");
            }
            // 订单已经被取消或者已经被支付
            if (order.getOrderStatus() != 0) {
                orderVo = BeanUtil.copyProperties(order, OrderVo.class);
                stringRedisTemplate.opsForValue().set("order:" + orderId,
                        JSON.toJSONString(orderVo), 15, TimeUnit.MINUTES);
                stringRedisTemplate.delete("orderSeckill:overdue:" + orderId);
                unlock(orderLock);
                return R.error("订单已经被取消或支付完成");
            }
        } else {
            orderVo = JSON.parseObject(orderValue, OrderVo.class);
        }
        RLock orderPayLock = redissonClient.getLock("orderOperateLock");
        // 加锁
        orderPayLock.lock();
        try {
            orderVo.setOrderStatus(1);
            // 超过15分钟未支付的订单会被消息队列监听器监听到后保存进数据库，这里操作不会重复保存订单
            Order order = BeanUtil.copyProperties(orderVo, Order.class);
            orderService.save(order);
            // 订单已支付结果缓存15分钟
            stringRedisTemplate.opsForValue().set("order:" + orderId,
                    JSON.toJSONString(orderVo), 15, TimeUnit.MINUTES);
            stringRedisTemplate.delete("orderSeckill:overdue:" + orderId);
            stringRedisTemplate.opsForHash().delete("goodsSecKillMap:" + order.getGoodsId(),
                    String.valueOf(order.getUserId()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            unlock(orderPayLock);
            unlock(orderLock);
        }
        return R.ok("订单支付完成");
    }

    @PostMapping("/OrderCancel/{orderId}/{goodsToken}")
    @ApiOperation("取消订单")
    public R orderCancel(@ApiParam @PathVariable("orderId") Long orderId,
                         @PathVariable("goodsToken") String goodsToken) {
        RLock orderLock = redissonClient.getLock("orderLock" + orderId);
        orderLock.lock();
        String orderValue = stringRedisTemplate.opsForValue().get("order:" + orderId);
        OrderVo orderVo;
        if (StringUtils.isEmpty(orderValue)) {
            // 走到这里说明缓存中没有订单，则订单已经被手动取消或者超时未支付取消
            // 从数据库查一次
            Order order = orderService.getById(orderId);
            orderVo = BeanUtil.copyProperties(order, OrderVo.class);
            // 数据库中也不存在及订单不存在
            if (order == null) {
                // 空缓存五分钟
                stringRedisTemplate.opsForValue().set("order:" + orderId, "", 5, TimeUnit.MINUTES);
                unlock(orderLock);
                return R.error("订单不存在");
            }
            if (!order.getOrderId().equals(orderId)) {
                unlock(orderLock);
                return R.error("无法操作非本人订单");
            }
            // 订单已经被取消或者已经被支付
            if (order.getOrderStatus() != 0) {
                orderVo = BeanUtil.copyProperties(order, OrderVo.class);
                stringRedisTemplate.opsForValue().set("order:" + orderId,
                        JSON.toJSONString(orderVo), 5, TimeUnit.MINUTES);
                unlock(orderLock);
                return R.error("订单已经被取消或支付完成");
            }
        } else {
            orderVo = JSON.parseObject(orderValue, OrderVo.class);
        }
        orderVo.setOrderStatus(2);
        Order order = BeanUtil.copyProperties(orderVo, Order.class);
        try {
            orderService.cancelOrder(order, goodsToken);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringRedisTemplate.opsForHash().delete("goodsSecKillMap:" + order.getGoodsId(),
                    String.valueOf(order.getUserId()));
            unlock(orderLock);
        }
        return R.ok("订单取消完成");
    }

    private void unlock(RLock rLock) {
        if (rLock.isHeldByCurrentThread()) {
            rLock.unlock();
        }
    }

}
