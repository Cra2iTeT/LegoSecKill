package com.Cra2iTeT.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:05
 */
@TableName("t_order")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {
    private Long orderId;
    private Long userId;
    private Long goodsId;
    private Integer orderStatus;
}
