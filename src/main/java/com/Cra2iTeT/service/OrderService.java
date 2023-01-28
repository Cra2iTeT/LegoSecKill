package com.Cra2iTeT.service;

import com.Cra2iTeT.entity.pojo.Order;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:12
 */
public interface OrderService extends IService<Order> {
    void cancelOrder(Long orderId);

    void cancelOrder(Order order, String goodsToken);
}
