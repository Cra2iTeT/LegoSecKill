package com.Cra2iTeT.service;

import com.Cra2iTeT.entity.pojo.Goods;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:12
 */
public interface GoodsService extends IService<Goods> {
    void genSecKillInfo(Long goodsId);

    void deleteSecKillInfo(String goodsIdAndGoodsToken);
}
