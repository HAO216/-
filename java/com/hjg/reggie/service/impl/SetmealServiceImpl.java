package com.hjg.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjg.reggie.common.CustomException;
import com.hjg.reggie.dto.SetmealDto;
import com.hjg.reggie.entity.Setmeal;
import com.hjg.reggie.entity.SetmealDish;
import com.hjg.reggie.mapper.SetmealMapper;
import com.hjg.reggie.service.SetmealDishService;
import com.hjg.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //stream流在高数据量下的效率比foreach高，所以使用它
        setmealDishes.stream().map((item) -> {
            //将套餐产品表setmeal_dish数据插入，其中表的套餐id设为SetmealDto数据传入后台后，通过雪花算法随机产生的
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //保存套餐和菜品的关联信息，操作setmeal_dish,执行insert操作
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 根据套餐id查询套餐中的菜品
     */
    public SetmealDto getByIdWithDish(long id) {
        Setmeal setmeal = this.getById(id);
        SetmealDto setmealDto=new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(setmealDishList);

        return setmealDto;
    }

    /**
     * 修改菜品
     * @param setmealDto
     */
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {
        //更新套餐表的套餐
        this.updateById(setmealDto);

        //查询并删除旧的套餐中的菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        //更新传过来的新的菜品，并将其赋予套餐的id
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes=setmealDishes.stream().map((item)->{
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        //将新传过来的套餐中的菜品其保存到setmeal_dish表中
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 套餐批量删除和单个删除
     */
    @Transactional
    public void deleteWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可用删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);

        //查询选中的并且在启售中的套餐数量
        int count = this.count(queryWrapper);
        if(count > 0){
            //count>0，说明有启售的套餐，全部不允许删除
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //如果可以删除，先删除套餐表中的数据---setmeal
        this.removeByIds(ids);

        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        //删除关系表中的数据----setmeal_dish
        setmealDishService.remove(lambdaQueryWrapper);
    }

}
