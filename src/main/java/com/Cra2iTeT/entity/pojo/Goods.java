package com.Cra2iTeT.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:06
 */
@TableName("goods")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Goods {
    private Long goodsId;
    private Integer stock;
    private Date startTime;
    private Date endTime;
}
