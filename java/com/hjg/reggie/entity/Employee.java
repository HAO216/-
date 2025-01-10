package com.hjg.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/*
* 员工实体类
* */
@Data
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;

    private String idNumber;//驼峰命名法 ---> 映射的字段名为 id_number

    private Integer status;

    /*
    * 为什么要完成字段自动填充？
    * 因为每次添加修改操作都要设置更新人员id和更新时间等，很麻烦
    *
    * 实现步骤：
        1、在实体类的属性上加入@TableField注解，指定自动填充的策略。
        2、按照框架要求编写元数据对象处理器，在此类中统一为公共字段赋值，此类需要实现MetaObjectHandler接口。
    * */
    @TableField(fill = FieldFill.INSERT)//完成字段自动填充，插入时填充该属性值
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)//完成字段自动填充，插入和更新时填充该属性值
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)//完成字段自动填充，插入时填充该属性值
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)//完成字段自动填充，插入和更新时填充该属性值
    private Long updateUser;

}
