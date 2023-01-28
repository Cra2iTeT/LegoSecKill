package com.Cra2iTeT.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 18:45
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderVo {
    private Long orderId;
    private Long goodsId;
    private Long userId;
    private Integer orderStatus;
}
