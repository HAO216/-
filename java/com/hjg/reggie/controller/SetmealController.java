package com.hjg.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjg.reggie.common.R;
import com.hjg.reggie.dto.DishDto;
import com.hjg.reggie.dto.SetmealDto;
import com.hjg.reggie.entity.Category;
import com.hjg.reggie.entity.Dish;
import com.hjg.reggie.entity.Setmeal;
import com.hjg.reggie.entity.SetmealDish;
import com.hjg.reggie.service.CategoryService;
import com.hjg.reggie.service.DishService;
import com.hjg.reggie.service.SetmealDishService;
import com.hjg.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishService dishService;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true) //清除setmealCache名称下,所有的缓存数据
    public R<String> save(@RequestBody SetmealDto setmealDto){
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 套餐信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        Page<Setmeal> setmealPage=new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage=new Page<>();

        //条件构造器，添加过滤条件，添加排序条件
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.like(name!=null,Setmeal::getName,name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //执行分页查询
        setmealService.page(setmealPage,queryWrapper);

        //对象拷贝
        //页面数据只传了分类id，没显示分类名称，要显示还需以下操作
        //排除records对象，自己写records。records是page对象的属性，存放查询到的数据
        BeanUtils.copyProperties(setmealPage,setmealDtoPage,"records");

        List<Setmeal> setmealList=setmealPage.getRecords();
        List<SetmealDto> setmealDtoList=setmealList.stream().map((item)->{
            SetmealDto setmealDto=new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);

            //根据id查询分类对象
            long categoryId=item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if(category!=null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }

            return setmealDto;
        }).collect(Collectors.toList());
        setmealDtoPage.setRecords(setmealDtoList);

        return R.success(setmealDtoPage);
    }

    /**
     * 根据套餐id查询套餐中的菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    /**
     * 修改套餐
     */
    @PutMapping
    @CacheEvict(value = "setmealCache",allEntries = true) //清除setmealCache名称下,所有的缓存数据
    public R<String> update(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐成功");
    }

    /**
     * 套餐批量删除和单个删除
     * 1.要先判断要删除的菜品是否在售卖，如果在售卖不能删除套餐
     * 2.如果可以删除套餐，则将setmeal_dish表中保存的信息一并删除
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true) //清除setmealCache名称下,所有的缓存数据
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        setmealService.deleteWithDish(ids);
        return R.success("删除套餐成功");
    }

    /**
     * 套餐批量启售停售和单个启售停售
     * 请求 URL: http://localhost:8080/setmeal/status/1?ids=1415580119015145474,1579663716218183682
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "setmealCache",allEntries = true) //清除setmealCache名称下,所有的缓存数据
    public R<String> status(@PathVariable("status") Integer status,@RequestParam List<Long> ids){
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.in(ids!=null,Setmeal::getId,ids);
        List<Setmeal> setmealList = setmealService.list(queryWrapper);

        for (Setmeal item:setmealList) {
            if(item!=null){
                item.setStatus(status);
                setmealService.updateById(item);
            }
        }
        return R.success("售卖状态修改成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     * 请求 URL: http://localhost:8080/dish/list?categoryId=1397844263642378242&status=1
     *
     * Cacheable： 在方法执行前，spring先查看Redis缓存中是否有数据，
     * 如果有数据，则直接返回缓存数据；若没有数据，调用方法并将方法返回值放到缓存中
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal){
        //移动端的前端传过来套餐分类id，将其包装成Setmeal对象
        //移动端的前端传过来的数据携带了status=1的数据，封装查询后保证展示的都是启售的套餐
        LambdaQueryWrapper<Setmeal> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId()!=null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus()!=null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> setmealList = setmealService.list(queryWrapper);
        return R.success(setmealList);
    }


    /**
     * 移动端点击套餐图片查看套餐具体内容
     * 这里返回的是dto 对象，因为前端需要copies这个属性
     * 前端主要要展示的信息是:套餐中菜品的基本信息，图片，菜品描述，以及菜品的份数
     * @param SetmealId
     * @return
     */
    @GetMapping("/dish/{id}")
    public R<List<DishDto>> dish(@PathVariable("id") Long SetmealId){
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,SetmealId);
        //获取套餐里面的所有菜品，这个就是SetmealDish表里面的数据
        List<SetmealDish> SetmealDish = setmealDishService.list(queryWrapper);

        List<DishDto> dishDtos = SetmealDish.stream().map((setmealDish) -> {
            DishDto dishDto = new DishDto();
            //通过setmealDish表中的菜品id去dish表中查询菜品，从而获取菜品的各类数据
            Long dishId = setmealDish.getDishId();
            Dish dish = dishService.getById(dishId);
            BeanUtils.copyProperties(dish, dishDto);

            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtos);
    }
}
