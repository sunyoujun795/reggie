package com.itheima.reggie.controller;


    /*
    用户下单
    代码开发-梳理交互过程
    在开发代码之前，需要梳理一下用户下单操作时前端页面和服务端的交互过程:
    1、在购物车中点击去结算按钮，页面跳转到订单确认页面
    2、在订单确认页面，发送ajax请求，请求服务端获取当前登录用户的默认地址
    3、在订单确认页面，发送ajax请求，请求服务端获取当前登录用户的购物车数据
    4、在订单确认页面点击去支付按钮，发送ajax请求，请求服务端完成下单操作
    开发用户下单功能，其实就是在服务端编写代码去处理前端页面发送的请求即可。


    用户下单
    代码开发-准备工作
    在开发业务功能前，先将需要用到的类和接口基本结构创建好:
    实体类Orders、OrderDetail (直接从课程资料中导入即可)
    Mapper接口OrderMapper、OrderDetailMapper
    业务层接口OrderService、OrderDetailService
    业务层实现类OrderServicelmpl、OrderDetailServicelmpl
    控制层OrderController、OrderDetailController

    * */

import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }
}
