package com.itheima.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//输出日志，方便调试。通过调用它的变量log输出日志
@Slf4j
//springboot启动类
@SpringBootApplication
@ServletComponentScan
@EnableTransactionManagement//开启事务支持
@EnableCaching  //开启Spring Cache注解方式的缓存功能
public class  ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class,args);
        log.info("项目启动成功。。。");

        /*在开发代码之前，需要梳理一下整个程序的执行过程:
        1、页面发送ajax请求，将新增员工页面中输入的数据以json的形式提交到服务端
        2、服务端Controller接收页面提交的数据并调用Service将数据进行保存
        3、Service调用Mapper操作数据库，保存数据*/




    }
}


