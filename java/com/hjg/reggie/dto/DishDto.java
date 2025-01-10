package com.hjg.reggie.dto;

import com.hjg.reggie.entity.Dish;
import com.hjg.reggie.entity.DishFlavor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/*
* | 实体模型 | 描述                                                         |
  | --------| ------------------------------------------------------------ |
  | DTO     | Data Transfer Object(数据传输对象)，一般用于展示层与服务层之间的数据传输。 |
  | Entity  | 最常用实体类，基本和数据表一一对应，一个实体类对应一张表。   |
  | VO      | Value Object(值对象), 主要用于封装前端页面展示的数据对象，用一个VO对象来封装整个页面展示所需要的对象数据 |
  | PO      | Persistant Object(持久层对象), 是ORM(Objevt Relational Mapping)框架中Entity，PO属性和数据库中表的字段形成一一对应关系 |
* */
//将对多表查询的数据封装成dto对象
@Data
public class DishDto extends Dish {
    private List<DishFlavor> flavors = new ArrayList<>();//菜品对应的口味数据

    private String categoryName;//菜品分类名称

    private Integer copies;
}