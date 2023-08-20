package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import sun.security.mscapi.CPublicKey;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {


    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    //获取Session对象,使用的是request对象
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        //处理逻辑如下:

        //1、将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> employeeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        employeeLambdaQueryWrapper.eq(Employee::getUsername,employee.getUsername());
        //数据库中username索引类型设置了unique，是唯一的，所以用getone
        Employee emp = employeeService.getOne(employeeLambdaQueryWrapper);
        //3、如果没有查询到则返回登录失败结果
        if(emp == null){
            return R.error("登录失败");
        }
        //4、密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }
        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已被禁用");
        }
        //6、登录成功，将员工id存入Session并返回登录成功结果
        //知识点：
        //(1)客户端会话跟踪技术：Cookie
        //(2)服务端会话跟踪技术：Session
        //Cookie是存储在浏览器端而Session是存储在服务器端
        //一次会话的多次请求之间数据共享功能

        //存储数据到 session 域中
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);

    }


    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        //1、清理Session中保存的当前登录员工的用户id
        request.getSession().removeAttribute("employee");
        //2、返回结果
        return R.success("退出成功");//成功code=1，前端判断做处理

    }


    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工，员工信息：{}",employee.toString());

        //设置初始密码132456，需要进行md5加密处理
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //使用了自动填充功能
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());

        //获得当前登录用户的id
        //Long empId = (Long) request.getSession().getAttribute("employee");


        //employee.setCreateUser(empId);
        //employee.setUpdateUser(empId);

        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /*员工信息分页查询
    在开发代码之前，需要梳理一下整个程序的执行过程:
    1、页面发送ajax请求，将分页查询参数(page、pageSize、name)提交到服务端
    2、服务端Controller接收页面提交的数据并调用Service查询数据
    3、Service调用Mapper操作数据库，查询分页数据
    4、Controller将查询到的分页数据响应给页面
    5、页面接收到分页数据并通过ElementUI的Table组件展示到页面上*/

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    //MP封装的Page类，使用MP提供的分页插件
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        //构造分页构造器
        Page pageInfo = new Page(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> lambdaQueryWrapper = new LambdaQueryWrapper();
        //添加过滤条件
        lambdaQueryWrapper.like(StringUtils.isNotEmpty(name),Employee::getName,name);
        //添加排序条件
        lambdaQueryWrapper.orderByDesc(Employee::getUpdateTime);

        //执行查询
        //传进对象和查询条件，然后MP会自动处理将total和records等赋好值
        employeeService.page(pageInfo,lambdaQueryWrapper);
        //将Page类对象的数据返回
        return R.success(pageInfo);
    }

    //启用/禁用员工账号
    //1、页面发送ajax请求，将参数(id、status)提交到服务端
    //2、服务端Controller接收页面提交的数据并调用Service更新数据
    //3、Service调用Mapper操作数据库

    //网页上需要一个code，所以String类型即可

    /**
     * 根据id修改员工信息
     * @param employee
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee){
        log.info(employee.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);

        //js对long型数据进行处理时丢失精度，导致提交的id和数据库中的id不一致。
        //可以在服务端给页面响应json数据时进行处理，将long型数据统一转为String字符串
        //Session中设置的保存的用户id
        //自动填充
        //Long empId = (Long)request.getSession().getAttribute("employee");
        //employee.setUpdateTime(LocalDateTime.now());
        //employee.setUpdateUser(empId);
        employeeService.updateById(employee);
        return R.success("员工信息修改成功");
    }

    /*编辑员工信息
    代码开发
    在开发代码之前需要梳理一下操作过程和对应的程序的执行流程:
    1、点击编辑按钮时，页面跳转到add.html，并在url中携带参数[员工id]
    2、在add.html页面获取url中的参数[员工id]
    3、发送ajax请求，请求服务端，同时提交员工id参数
    4、服务端接收请求，根据员工id查询员工信息，将员工信息以json形式响应给页面
    5、页面接收服务端响应的json数据，通过VUE的数据绑定进行员工信息回显
    6、点击保存按钮，发送ajax请求，将页面中的员工信息以json方式提交给服务端
    7、服务端接收员工信息，并进行处理，完成后给页面响应
    8、页面接收到服务端响应信息后进行相应处理*/

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */

    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);
        }
        return R.error("未找到此员工");

    }



}
