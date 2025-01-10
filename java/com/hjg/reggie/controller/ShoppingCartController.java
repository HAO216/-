package com.hjg.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjg.reggie.common.BaseContext;
import com.hjg.reggie.common.R;
import com.hjg.reggie.entity.ShoppingCart;
import com.hjg.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车
 */
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     * 请求 URL: http://localhost:8080/shoppingCart/add
     * 负载：{"amount":138,"dishFlavor":"常温,不要蒜,微辣","dishId":"1397851370462687234",
     *       "name":"邵阳猪血丸子","image":"2a50628e-7758-4c51-9fbb-d37c61cdacad.jpg"}
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据:{}",shoppingCart);
        ShoppingCart cart = shoppingCartService.add(shoppingCart);
        return R.success(cart);
    }

    /**
     * 客户端的套餐或者是菜品数量减少设置
     * 没必要设置返回值
     * @param shoppingCart
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据:{}",shoppingCart);
        ShoppingCart cart = shoppingCartService.sub(shoppingCart);
        return R.success(cart);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        log.info("查看购物车...");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public R<String> clean(){
        log.info("清空购物车...");
        shoppingCartService.clean();
        return R.success("清空购物车成功");
    }

}
