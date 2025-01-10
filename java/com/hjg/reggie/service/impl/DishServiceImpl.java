package com.hjg.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjg.reggie.common.CustomException;
import com.hjg.reggie.dto.DishDto;
import com.hjg.reggie.entity.Dish;
import com.hjg.reggie.entity.DishFlavor;
import com.hjg.reggie.entity.Setmeal;
import com.hjg.reggie.entity.SetmealDish;
import com.hjg.reggie.mapper.DishMapper;
import com.hjg.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    /* 注意：
     * 由于在 saveWithFlavor 方法中，进行了两次数据库的保存操作，
     * 操作了两张表，那么为了保证数据的一致性，我们需要在方法上加上注解 @Transactional来控制事务。
     *
     * Service层方法上加的注解@Transactional要想生效，
     * 需要在引导类ReggieApplication上加上注解 @EnableTransactionManagement，开启对事务的支持。
     */
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        Long dishId = dishDto.getId();//菜品id
        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        /*
        * 将每个DishFlavor元素设置dishId，并重新赋予给自己
        * 因为新增时，flavor只有name和value属性，没有相应的菜品id，自己设置
        * */
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);

        //查询当前菜品对应的口味信息，从dish_flavor表查询
        //在该页面本来存在的口味具有dish_id，但如果新增的口味就没有，因此仍全部重新赋予dish_id
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);

        //清理当前菜品对应口味数据---dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据---dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        //构造条件查询器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //先查询该菜品是否在售卖，如果是则抛出业务异常
        queryWrapper.in(ids!=null,Dish::getId,ids);
        List<Dish> list = this.list(queryWrapper);
        for (Dish dish : list) {
            Integer status = dish.getStatus();
            //如果不是在售卖,则可以删除
            if (status == 0){
                //清理某个分类下面的菜品缓存数据
                String key = "dish_" + dish.getCategoryId() + "_1";
                redisTemplate.delete(key);

                this.removeById(dish.getId());
            }else {
                //此时应该回滚,因为可能前面的删除了，但是后面的是正在售卖
                throw new CustomException("删除菜品中有正在售卖菜品,无法全部删除");
            }
        }
    }

    @Transactional
    public boolean deleteInSetmeal(List<Long> ids) {
        boolean flag=true;

        //1.根据菜品id在stemeal_dish表中查出哪些套餐包含该菜品
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.in(SetmealDish::getDishId,ids);
        List<SetmealDish> SetmealDishList = setmealDishService.list(setmealDishLambdaQueryWrapper);
        //2.如果菜品没有关联套餐，直接删除就行  其实下面这个逻辑可以抽离出来，这里我就不抽离了
        if (SetmealDishList.size() == 0){
            //这个deleteByIds中已经做了菜品启售不能删除的判断力
            this.deleteByIds(ids);
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(DishFlavor::getDishId,ids);
            dishFlavorService.remove(queryWrapper);
            return flag;
        }

        //3.如果菜品有关联套餐，并且该套餐正在售卖，那么不能删除
        //3.1得到与删除菜品关联的套餐id
        ArrayList<Long> Setmeal_idList = new ArrayList<>();
        for (SetmealDish setmealDish : SetmealDishList) {
            Long setmealId = setmealDish.getSetmealId();
            Setmeal_idList.add(setmealId);
        }
        //3.2查询出与删除菜品相关联的套餐
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(Setmeal::getId,Setmeal_idList);
        List<Setmeal> setmealList = setmealService.list(setmealLambdaQueryWrapper);
        //3.3对拿到的所有套餐进行遍历，然后拿到套餐的售卖状态，如果有套餐正在售卖那么删除失败
        for (Setmeal setmeal : setmealList) {
            Integer status = setmeal.getStatus();
            if (status == 1){
                flag=false;
            }
        }

        //3.4要删除的菜品关联的套餐没有在售，可以删除
        //3.5这下面的代码并不一定会执行,因为如果前面的for循环中出现status == 1,那么下面的代码就不会再执行
        this.deleteByIds(ids);
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId,ids);
        dishFlavorService.remove(queryWrapper);

        return flag;
    }

}
