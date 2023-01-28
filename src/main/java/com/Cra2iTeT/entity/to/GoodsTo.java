package com.Cra2iTeT.entity.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 17:41
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoodsTo {
    private Integer stock;
    private Date startTime;
    private Date endTime;
}
