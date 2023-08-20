package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/*
* 1、页面(backend/page/category/list.html)发送ajax请求，将新增分类窗口输入的数据以json形式提交到服务端
* 2、服务端Controller接收页面提交的数据并调用Service将数据进行保存
* 3、Service调用Mapper操作数据库，保存数据
*/

@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param category
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request, @RequestBody Category category){
        log.info("新增分类，分类信息：{}",category.toString());



        //使用了自动填充功能
        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());

        //获得当前登录用户的id
        //Long empId = (Long) request.getSession().getAttribute("employee");


        //employee.setCreateUser(empId);
        //employee.setUpdateUser(empId);

        categoryService.save(category);
        return R.success("新增分类成功");
    }


    /**
     * 分类信息分页查询
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize){
        log.info("page = {},pageSize = {},name = {}",page);

        //构造分页构造器
        Page<Category> pageInfo = new Page<>(page,pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper();

        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Category::getSort);

        //执行查询
        //传进对象和查询条件，然后MP会自动处理将total和records等赋好值
        categoryService.page(pageInfo,lambdaQueryWrapper);
        //将Page类对象的数据返回
        return R.success(pageInfo);
    }

    /**
     * 根据id修改菜品信息
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(HttpServletRequest request,@RequestBody Category category){
        log.info("修改分类"+category.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);

        //js对long型数据进行处理时丢失精度，导致提交的id和数据库中的id不一致。
        //可以在服务端给页面响应json数据时进行处理，将long型数据统一转为String字符串
        //Session中设置的保存的用户id
        //自动填充
        //Long empId = (Long)request.getSession().getAttribute("employee");
        //employee.setUpdateTime(LocalDateTime.now());
        //employee.setUpdateUser(empId);
        categoryService.updateById(category);
        return R.success("分类信息修改成功");
    }

    /**
     * 根据id删除分类
     * @param ids
     * @return
     */
    /*
    * 前面我们已经实现了根据id删除分类的功能，但是并没有检查删除的分类是否关联了菜品或者套餐，
    * 所以我们需要进行功能完善。要完善分类删除功能，需要先准备基础的类和接口:
    * 1、实体类Dish和Setmeal (从课程资料中复制即可)
    * 2、Mapper接口DishMapper和SetmealMapper
    * 3、Service接口DishService和SetmealService
    * 4、Service实现类DishServicelmpl和SetmealServicelmpl
    */

    @DeleteMapping
    //前端只需返回个code即可
    public R<String> getById(Long ids){
        log.info("根据id删除菜品信息");
        //categoryService.removeById(ids);
        categoryService.remove(ids);

        return R.success("分类信息删除成功");

    }

    /**
     * 根据条件查询分类数据
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        //条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        lambdaQueryWrapper.eq(category.getType() != null,Category::getType,category.getType());
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);
        List<Category> list = categoryService.list(lambdaQueryWrapper);
        return R.success(list);
    }
}
