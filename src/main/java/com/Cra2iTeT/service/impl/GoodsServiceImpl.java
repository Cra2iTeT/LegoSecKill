package com.Cra2iTeT.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.Cra2iTeT.entity.pojo.Goods;
import com.Cra2iTeT.entity.vo.GoodsVo;
import com.Cra2iTeT.mapper.GoodsMapper;
import com.Cra2iTeT.service.GoodsService;
import com.Cra2iTeT.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author Cra2iTeT
 * @since 2023/1/26 16:14
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {
    private final StringRedisTemplate stringRedisTemplate;

    public GoodsServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成抢购商品缓存
     *
     * @param goodsId
     */
    @Override
    public void genSecKillInfo(Long goodsId) {
        String goodsValue = stringRedisTemplate.opsForValue().get("secKill:info:" + goodsId);
        if (StringUtils.isEmpty(goodsValue)) {
            throw new RuntimeException("系统错误");
        }
        Goods goods = JSON.parseObject(goodsValue, Goods.class);
        GoodsVo goodsVo = BeanUtil.copyProperties(goods, GoodsVo.class);
        String token = NumberUtil.genToken(goodsId);
        goodsVo.setToken(token);
        // 缓存token
        stringRedisTemplate.opsForHash().put("goods:secKill", String.valueOf(goodsId), token);
        // 缓存抢购商品信息
        stringRedisTemplate.opsForHash().put("goods:stock", token, String.valueOf(goodsVo.getStock()));
        stringRedisTemplate.opsForHash().put("goods:secKill", String.valueOf(goodsId), JSON.toJSONString(goodsVo));
        // 设置商品抢购结束时间缓存信息
        Date timeNow = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).getTime();
        long timeBetweenMs = DateUtil.betweenMs(timeNow, goodsVo.getEndTime());
        stringRedisTemplate.opsForValue().set("secKill:overdue:" + goodsId + ":" + token,
                "", timeBetweenMs + 1200000, TimeUnit.MILLISECONDS);
    }

    /**
     * 删除抢购信息缓存
     *
     * @param goodsIdAndGoodsToken
     */
    @Override
    public void deleteSecKillInfo(String goodsIdAndGoodsToken) {
        String[] split = goodsIdAndGoodsToken.split(":");
        long goodsId = Long.parseLong(split[0]);
        long goodsToken = Long.parseLong(split[1]);
        Integer stock = Integer.valueOf((String) Objects.requireNonNull(stringRedisTemplate
                .opsForHash().get("goods:stock", goodsToken)));
        LambdaUpdateWrapper<Goods> goodsUpdateWrapper = new LambdaUpdateWrapper<>();
        goodsUpdateWrapper.set(Goods::getStock, stock);
        update(goodsUpdateWrapper);
        stringRedisTemplate.opsForHash().delete("goods:secKill", goodsId);
        stringRedisTemplate.opsForHash().delete("goods:stock", goodsToken);
        stringRedisTemplate.opsForHash().delete("goodsSecKillMap:" + goodsId);
    }
}
