package com.hjg.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hjg.reggie.common.R;
import com.hjg.reggie.entity.User;
import com.hjg.reggie.service.UserService;
import com.hjg.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    //获取验证码
    @PostMapping("/sendMsg")
    public R<String> sendMsg(HttpSession session, @RequestBody User user){
        //获取邮箱号
        //相当于发送短信定义的String to
        String email = user.getPhone();
        String subject = "瑞吉外卖";
        //StringUtils.isNotEmpty字符串非空判断
        if (StringUtils.isNotEmpty(email)) {
            //发送一个四位数的验证码,把验证码变成String类型
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            String text = "【瑞吉外卖】您好，您的登录验证码为：" + code + "，请尽快登录，验证码有效时间5分钟。如非本人操作，请忽略此邮件。";
            log.info("验证码为：" + code);
            //发送短信
            userService.sendMsg(email,subject,text);

            /*
            //将验证码保存到session当中
            //将邮箱作为key，将code最为value保存到session中,，因此邮箱和验证码可以一一对应
            session.setAttribute(email,code);
            */

            //需要将生成的验证码保存到Redis,设置过期时间
            redisTemplate.opsForValue().set(email, code, 5, TimeUnit.MINUTES);

            return R.success("验证码发送成功");
        }
        return R.error("验证码发送异常，请重新发送");
    }

    //登录
    @PostMapping("/login")
    //Map存JSON数据
    public R<User> login(HttpSession session,@RequestBody Map map){
        //获取邮箱，用户输入的，这个phone就是输入的邮箱
        String phone = map.get("phone").toString();
        //获取验证码，用户输入的，这个code就是生成的验证码
        String code = map.get("code").toString();

        /**
         * 获取session中保存的验证码
         * 登录的邮箱作为session的key值，将code最为value
         * 因此邮箱和验证码可以一一对应，保证邮箱验证码数据一致完整性
         * */
        //Object sessionCode = session.getAttribute(phone);

        //改动：从Redis中获取缓存的验证码
        Object sessionCode = redisTemplate.opsForValue().get(phone);

        //将session的验证码和用户输入的验证码进行比对
        if (sessionCode != null && sessionCode.equals(code)) {
            //要是User数据库没有这个邮箱则自动注册,先看看输入的邮箱是否存在数据库
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);
            //获得唯一的用户，因为邮箱是唯一的
            User user = userService.getOne(queryWrapper);
            //要是User数据库没有这个邮箱则自动注册
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                //取邮箱的前五位为用户名
                user.setName("用户"+phone.substring(0,5));
                userService.save(user);
            }
            //不保存这个用户名就登不上去，因为过滤器需要得到这个user才能放行，程序才知道你登录了
            session.setAttribute("user", user.getId());

            //如果用户登陆成功，则删除Redis缓存中的验证码
            redisTemplate.delete(phone);

            return R.success(user);
        }
        return R.error("登录失败");
    }

    /**
     * 退出功能
     * ①在controller中创建对应的处理方法来接受前端的请求，请求方式为post；
     * ②清理session中的用户id
     * ③返回结果（前端页面会进行跳转到登录页面）
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //清理session中的用户id
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }
}
