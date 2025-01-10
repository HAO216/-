package com.hjg.reggie.common;

/*
* 在 employee 表结构中，我们针对于username字段，建立了唯一索引，添加重复的username数据时，违背该约束，就会报错。
* 但是此时前端提示的信息并不具体，用户并不知道是因为什么原因造成的该异常，我们需要给用户提示详细的错误信息。
* 因此设置此全局异常处理器
* */

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/*
* ControllerAdvice这个代表拦截所有带RestController和Controller的类
* ResponseBody将结果封装成json数据并返回
* */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     * 目的：禁止重复添加具有唯一约束字段的数据(如：相同username的员工用户，相同sort的菜品等)
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error(ex.getMessage());
        //如果异常信息包括这个关键字，说明了已经违反了username的唯一约束
        //IDEA抛出异常信息例子：Duplicate entry 'zhangsan' for key 'employee.idx_username'
        if(ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }
        //如果产生其他异常信息，则输出"未知错误"
        return R.error("未知错误");
    }

    /**
     * 异常处理方法
     * 这是自己写的异常，捕获这个异常，并收到它携带的信息
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex){
        log.error(ex.getMessage());
        return R.error(ex.getMessage());
    }
}
