package com.hjg.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjg.reggie.common.BaseContext;
import com.hjg.reggie.common.CustomException;
import com.hjg.reggie.common.R;
import com.hjg.reggie.entity.ShoppingCart;
import com.hjg.reggie.mapper.ShoppingCartMapper;
import com.hjg.reggie.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
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
    @Transactional
    public ShoppingCart add(ShoppingCart shoppingCart) {
        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        if(dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else{
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }

        //查询当前菜品或者套餐是否在购物车中
        //SQL:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if(cartServiceOne != null){
            //如果已经存在，就在原来数量基础上加一
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        }else{
            //如果不存在，则添加到购物车，数量默认就是一
            shoppingCart.setNumber(1);
            //注意这个不能使用自动填充，因为这个实体类只有createTime，没有updateTime
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }
        return cartServiceOne;
    }

    /**
     * 减少菜品套餐到购物车
     */
    @Transactional
    public ShoppingCart sub(ShoppingCart shoppingCart) {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();

        Long dishId = shoppingCart.getDishId();
        //代表数量减少的是菜品数量
        if (dishId != null){
            //通过dishId查出购物车对象
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
            //这里必须要加两个条件，否则会出现用户互相修改对方与自己购物车中相同套餐或者是菜品的数量
            queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
            ShoppingCart cart1 = shoppingCartService.getOne(queryWrapper);
            cart1.setNumber(cart1.getNumber()-1);
            Integer LatestNumber = cart1.getNumber();
            if (LatestNumber > 0){
                //对数据进行更新操作
                shoppingCartService.updateById(cart1);
            }else if(LatestNumber == 0){
                //如果购物车的菜品数量减为0，那么就把菜品从购物车删除
                shoppingCartService.removeById(cart1.getId());
            }else if (LatestNumber < 0){
                throw new CustomException("操作异常");
            }

            return cart1;
        }

        Long setmealId = shoppingCart.getSetmealId();
        //代表是套餐数量减少
        if (setmealId != null){
            queryWrapper.eq(ShoppingCart::getSetmealId,setmealId).eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
            ShoppingCart cart2 = shoppingCartService.getOne(queryWrapper);
            cart2.setNumber(cart2.getNumber()-1);
            Integer LatestNumber = cart2.getNumber();
            if (LatestNumber > 0){
                //对数据进行更新操作
                shoppingCartService.updateById(cart2);
            }else if(LatestNumber == 0){
                //如果购物车的套餐数量减为0，那么就把套餐从购物车删除
                shoppingCartService.removeById(cart2.getId());
            }else if (LatestNumber < 0){
                throw new CustomException("操作异常");
            }
            return cart2;
        }
        //如果两个if判断都进不去
        throw new CustomException("操作异常");
    }

    /**
     * 清空购物车
     */
    public void clean() {
        LambdaQueryWrapper<ShoppingCart> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);
    }

}
