package com.hjg.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hjg.reggie.common.R;
import com.hjg.reggie.dto.DishDto;
import com.hjg.reggie.entity.*;
import com.hjg.reggie.service.CategoryService;
import com.hjg.reggie.service.DishFlavorService;
import com.hjg.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
//注：增加，修改，删除，启售停售等操作均要删除已缓存的Redis数据
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);
        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝
        //页面数据只传了分类id，没显示分类名称，要显示还需以下操作
        //1.1这个操作时数据拷贝，并且不拷贝records对象，records单独操作，有dish对象变为dishDto对象
        //1.2排除records对象，自己写records。records是page对象的属性，存放查询到的数据
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        //获取List<Dish>保存的数据集合
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {

            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        //A. 清理所有分类下的菜品缓存
        //清理所有菜品的缓存数
        //注：修改菜品必须全部清理缓存，因为你可能修改它的菜品分类，使这个分类的菜品减少，同时使另一个增加，因此必须全部清理
        Set keys = redisTemplate.keys("dish_*"); //获取所有以dish_xxx开头的key
        redisTemplate.delete(keys); //删除这些key

        //B. 清理当前添加菜品分类下的缓存
        //清理某个分类下面的菜品缓存数据
        /*String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);*/

        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);
        return R.success("修改菜品成功");
    }

    /*
    * 对菜品进行停售或者是启售
    * 包括单个菜品和多个菜品一起解决
    */
    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status,@RequestParam List<Long> ids){
        LambdaQueryWrapper<Dish> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.in(ids!=null,Dish::getId,ids);
        List<Dish> list = dishService.list(queryWrapper);

        for (Dish dish:list) {
            if(dish!=null){
                //清理某个分类下面的菜品缓存数据
                String key = "dish_" + dish.getCategoryId() + "_1";
                redisTemplate.delete(key);

                dish.setStatus(status);
                dishService.updateById(dish);
            }
        }
        return R.success("售卖状态修改成功");
    }

    /**
     * 菜品批量删除和单个删除
     * 1.判断要删除的菜品在不在售卖的套餐中，如果在那不能删除
     * 2.要先判断要删除的菜品是否在售卖，如果在售卖也不能删除
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        boolean deleteInSetmeal = dishService.deleteInSetmeal(ids);
        if(deleteInSetmeal){
            //清理某个分类下面的菜品缓存数据，不过这个在deleteInSetmeal已经进行了

            return R.success("菜品删除成功");
        }
        else{
            return R.error("删除的菜品中有关联在售套餐,删除失败！");
        }
    }

    /*
    * 1.根据套餐id，查询相应套餐所对应的菜品
    * 2.根据name，模糊查询所有的正在启售的菜品
    */
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.like(dish.getName()!=null,Dish::getName,dish.getName());
        //添加条件，查询状态为1（启售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/

    /*//改造上面这个分类中查询菜品的操作。使它在移动端中也能使用，并显示出更多的比如口味等信息
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        List<DishDto> dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);

            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }*/

    /*继续改造这个分类中查询菜品的操作：
    * 原方法会根据前端提交的查询条件(categoryId)进行数据库查询操作。在高并发的情况下，
    * 频繁查询数据库会导致系统性能下降，服务端响应时间增长。现在需要对此方法进行缓存优化，提高系统的性能。
    *
    * 改造DishController的list方法，先从Redis中获取分类对应的菜品数据，如果有则直接返回，无需查询数据库;
    * 如果没有则查询数据库，并将查询到的菜品数据存入Redis。
    * */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList=null;

        //自己动态构造key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();//dish_1397844391040167938_1

        //先从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if(dishDtoList != null){
            //如果存在，直接返回，从Redis获取缓存的菜品数据，无需查询数据库
            return R.success(dishDtoList);
        }

        //如果不存在
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类id
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);

            return dishDto;
        }).collect(Collectors.toList());

        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis，并设置过期时间60分钟
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

}
