package com.hjg.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hjg.reggie.dto.DishDto;
import com.hjg.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id);

    //更新菜品信息，同时更新对应的口味信息
    public void updateWithFlavor(DishDto dishDto);

    //根据传过来的id批量或者是单个的删除菜品，并判断是否是启售的
    public void deleteByIds(List<Long> ids);

    //菜品批量删除和单个删除，删除时用到deleteByIds方法删除菜品
    public boolean deleteInSetmeal(List<Long> ids);
}