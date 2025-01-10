package com.hjg.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hjg.reggie.entity.Employee;
import com.hjg.reggie.mapper.EmployeeMapper;
import com.hjg.reggie.service.EmployeeService;
import org.springframework.stereotype.Service;
/*
 * 此操作Mybatis-plus提供
 * */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
}
