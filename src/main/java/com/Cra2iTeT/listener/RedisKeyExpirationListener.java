package com.Cra2iTeT.listener;

import com.Cra2iTeT.service.GoodsService;
import com.Cra2iTeT.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * @author Cra2iTeT
 * @since 2023/1/28 12:55
 */
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        // 提前一小时生成商品抢购信息
        if (expiredKey.startsWith("secKill:store:")) {
            Long goodsId = Long.valueOf(expiredKey.substring("secKill:store:".length()));
            goodsService.genSecKillInfo(goodsId);
        }
        // 抢购时间结束
        else if (expiredKey.startsWith("secKill:overdue:")) {
            String goodsIdAndGoodsToken = expiredKey.substring("secKill:overdue:".length());
            goodsService.deleteSecKillInfo(goodsIdAndGoodsToken);
        }
        // 订单超时自动取消
        else if (expiredKey.startsWith("orderSeckill:overdue:")) {
            Long orderId = Long.valueOf(expiredKey.substring("orderSeckill:overdue:".length()));
            orderService.cancelOrder(orderId);
        }
    }
}
