package com.hjg.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hjg.reggie.dto.SetmealDto;
import com.hjg.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 根据套餐id查询套餐中的菜品
     */
    public SetmealDto getByIdWithDish(long id);

    /**
     * 修改菜品
     */
    public void updateWithDish(SetmealDto setmealDto);

    /**
     * 套餐批量删除和单个删除
     */
    public void deleteWithDish(List<Long> ids);
}
