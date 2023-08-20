/**

 * 在开发代码之前，需要梳理一下登录时前端页面和服务端的交互过程:
 * 1、在登录页面(front/page/login.html)输入手机号，点击【获取验证码】按钮，页面发送ajax请求，在服务端调用短信服务API给指定手机号发送验证码短信
 * 2、在登录页面输入验证码，点击【登录】按钮，发送ajax请求，在服务端处理登录请求
 * 开发手机验证码登录功能，其实就是在服务端编写代码去处理前端页面发送的这2次请求即可。
 * 在开发代码之前，需要梳理一下登录时前端页面和服务端的交互过程:
 * 1、在登录页面(front/page/login.html)输入手机号，点击【获取验证码】按钮，页面发送ajax请求，在服务端调用短信服务API给指定手机号发送验证码短信
 * 2、在登录页面输入验证码，点击【登录】按钮，发送ajax请求，在服务端处理登录请求
 * 开发手机验证码登录功能，其实就是在服务端编写代码去处理前端页面发送的这2次请求即可。
 *
 *
 * 在开发业务功能前，先将需要用到的类和接口基本结构创建好:
 * 实体类User(直接从课程资料中导入即可)
 * Mapper接口UserMapper
 * ;
 * 业务层接口UserService
 * 业务层实现类UserServicelmpl
 * 控制层UserController
 * 工具类SMSutils、ValidateCodeUtils(直接从课程资料中导入即可)
 */



/*
缓存短信验证码
实现思路
前面我们已经实现了移动端手机验证码登录，随机生成的验证码我们是保存在HttpSession中的。现在需要改造为将验证码缓存在Redis中，具体的实现思路如下:
1、在服务端UserController中注入RedisTemplate对象，用于操作Redis
2、在服务端UserController的sendMsg方法中，将随机生成的验证码缓存到Redis中，并设置有效期为5分钟
3、在服务端UserController的login方法中，从Redis中获取缓存的验证码，如果登录成功则删除Redis中的验证码
* */
package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}",code);

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //需要将生成的验证码保存到Session
            //session.setAttribute(phone,code);

            //将生成的验证码缓冲到Redis中，并且设置有效期为5分钟
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);

            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();



        //从Session中获取保存的验证码
        //Object codeInSession = session.getAttribute(phone);

        //从Redis中获取保存的验证码
        Object codeInRedis = redisTemplate.opsForValue().get(phone);


        //进行验证码的比对（页面提交的验证码和Session中保存的验证码比对）
        if(codeInRedis != null && codeInRedis.equals(code)){
            //如果能够比对成功，说明登录成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());

            //如果用户登录成功，删除Redis中缓冲的验证码
            redisTemplate.delete(phone);

            return R.success(user);
        }
        return R.error("登录失败");
    }

}

