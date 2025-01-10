package com.hjg.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hjg.reggie.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {

    /**
     * 添加菜品套餐到购物车
     */
    public ShoppingCart add(ShoppingCart shoppingCart);

    /**
     * 减少菜品套餐到购物车
     */
    public ShoppingCart sub(ShoppingCart shoppingCart);

    /**
     * 清空购物车
     */
    public void clean();
}
