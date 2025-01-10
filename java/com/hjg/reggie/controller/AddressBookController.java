package com.hjg.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hjg.reggie.common.BaseContext;
import com.hjg.reggie.common.R;
import com.hjg.reggie.entity.AddressBook;
import com.hjg.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿管理
 */
@Slf4j
@RestController
@RequestMapping("/addressBook")
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增
     */
    @PostMapping
    public R<AddressBook> save(@RequestBody AddressBook addressBook) {
        //BaseContext.getCurrentId()从线程中获取保存的登录用户id
        //不能从session中获取，可能一台电脑登录多个用户session冲突
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook:{}", addressBook);

        //如果用户新增第一条地址，直接将其设为默认地址
        LambdaQueryWrapper<AddressBook> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId,addressBook.getUserId());
        int count = addressBookService.count(queryWrapper);
        if(count==0){
            //将第一条地址设为默认
            addressBook.setIsDefault(1);
            addressBookService.save(addressBook);
        }
        else{
            addressBookService.save(addressBook);
        }

        return R.success(addressBook);
    }

    /**
     * 设置默认地址
     */
    @PutMapping("default")
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook) {
        log.info("addressBook:{}", addressBook);
        LambdaUpdateWrapper<AddressBook> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        //1.先将所有的地址都设为不是(0)默认地址
        wrapper.set(AddressBook::getIsDefault, 0);
        //SQL:update address_book set is_default = 0 where user_id = ?
        addressBookService.update(wrapper);

        //2.将前端传过来的地址唯一修改为默认地址(1)
        addressBook.setIsDefault(1);
        //SQL:update address_book set is_default = 1 where id = ?
        //3.进行该默认地址的更新操作
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }

    /**
     * 根据id查询地址
     */
    @GetMapping("/{id}")
    public R get(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("没有找到该对象");
        }
    }

    /**
     * 查询默认地址
     */
    @GetMapping("default")
    public R<AddressBook> getDefault() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null == addressBook) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }

    /**
     * 查询指定用户的全部地址
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("addressBook:{}", addressBook);

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(null != addressBook.getUserId(), AddressBook::getUserId, addressBook.getUserId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        //SQL:select * from address_book where user_id = ? order by update_time desc
        return R.success(addressBookService.list(queryWrapper));
    }

    /**
     * 根据地址id删除用户地址
     * @param id
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") Long id){

        if (id == null){
            return R.error("请求异常");
        }
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        //通过查询用户id和要删除的地址id，删除地址
        queryWrapper.eq(AddressBook::getId,id).eq(AddressBook::getUserId,BaseContext.getCurrentId());
        addressBookService.remove(queryWrapper);

        return R.success("删除地址成功");
    }

    /**
     * 修改收货地址
     * @param addressBook
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody AddressBook addressBook){

        if (addressBook == null){
            return R.error("请求异常");
        }
        addressBookService.updateById(addressBook);

        return R.success("修改成功");
    }
}
