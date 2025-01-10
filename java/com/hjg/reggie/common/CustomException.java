package com.hjg.reggie.common;

/*
* 自定义业务异常
* 继承了运行时异常
* 1.查询当前分类是否关联了套餐或者菜品，如果已经关联不允许删除，抛出这个业务异常
*   该异常将在全局异常处理器中被捕获，并获取它携带的的信息
* */
public class CustomException extends RuntimeException{
    public CustomException(String message){
        super(message);
    }
}
