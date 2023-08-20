package com.itheima.reggie.controller;

/*
Spring Cache
Spring Cache介绍
Spring cache是一个框架，实现了基于注解的缓存功能，只需要简单地加一个注解，就能实现缓存功能。
Spring Cache提供了一层抽象，底层可以切换不同的cache实现。具体就是通过CacheManager接口来统一不同的缓存技术。
CacheManager是Spring提供的各种缓存技术抽象接口。
针对不同的缓存技术需要实现不同的CacheManager:CacheManager
描述
EhcacheCacheManager：使用EhCache作为缓存技术
GuavacacheManager：使用Google的GuavaCache作为缓存技术
RedisCacheManager：使用Redis作为缓存技术


Spring Cache常用注解
注解
说明
@EnableCaching	开启缓存注解功能
@Cacheable		在方法执行前spring先查看缓存中是否有数据，如果有数据，则直接返回缓存数据;若没有数据，调用方法并将方法返回值放到缓存中
@cachePut		将方法的返回值放到缓存中
@CacheEvict	将一条或多条数据从缓存中删除

在spring boot项目中，使用缓存技术只需在项目中导入相关缓存技术的依赖包，并在启动类上使用@EnableCaching开启缓存支持即可。
例如，使用Redis作为缓存技术，只需要导入Spring data Redis的maven坐标即可。

* */


/*
缓存套餐数据
实现思路
前面我们已经实现了移动端套餐查看功能，对应的服务端方法为SetmealController的list方法，此方法会根据前端提交的查询条件进行数据库查询操作。在高并发的情况下，频繁查询数据库会导致系统性能下降，服务端响应时间增长。现在需要对此方法进行缓存优化，提高系统的性能。
具体的实现思路如下:
1、导入Spring Cache和Redis相关maven坐标
2、在application.yml中配置缓存数据的过期时间
3、在启动类上加入@EnableCaching注解，开启缓存注解功能
4、在SetmealController的list方法上加入@Cacheable注解
5、在SetmealController的save和delete方法上加入CacheEvict注解

* */

/*
新增套餐
代码开发-准备工作
在开发业务功能前，先将需要用到的类和接口基本结构创建好:
实体类SetmealDish(直接从课程资料中导入即可，Setmeal实体前面课程中已经导入过了)DTo SetmealDto(直接从课程资料中导入即可)
Mapper接口SetmealDishMapper
业务层接口SetmealDishService
业务层实现类SetmealDishServicelmpl
控制层SetmealController
* */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /*
    在开发代码之前，需要梳理一下新增套餐时前端页面和服务端的交互过程:
    1、页面(backend/page/comboladd.html)发送ajax请求，请求服务端获取套餐分类数据并展示到下拉框中
    2、页面发送ajax请求，请求服务端获取菜品分类数据并展示到添加菜品窗口中
    3、页面发送ajax请求，请求服务端，根据菜品分类查询对应的菜品数据并展示到添加菜品窗口中
    4、页面发送请求进行图片上传，请求服务端将图片保存到服务器
    5、页面发送请求进行图片下载，将上传的图片进行回显
    6、点击保存按钮，发送ajax请求，将套餐相关数据以json形式提交到服务端
    开发新增套餐功能，其实就是在服务端编写代码去处理前端页面发送的这6次请求即可。
    * */


    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    //@CacheEvict	将一条或多条数据从缓存中删除
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto){


        log.info(setmealDto.toString());
        setmealService.saveWithDish(setmealDto);

        return R.success("套餐添加成功");
    }

    /**
     * 套餐信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     * 在开发代码之前，需要梳理一下菜品分页查询时前端页面和服务端的交互过程:
     * 1、页面(backend/page/food/list.html)发送ajax请求，将分页查询参数(page、pageSize、name)提交到服务端，获取分页数据
     * 2、页面发送请求，请求服务端进行图片下载，用于页面图片展示
     * 开发菜品信息分页查询功能，其实就是在服务端编写代码去处理前端页面发送的这2次请求即可。
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        //构造分页构造器
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);

        //页面中有个categoryname，dish中没有，所以用dishdto
        Page<SetmealDto> setmealDtoPage = new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper();
        //添加过滤条件
        lambdaQueryWrapper.like(name!=null,Setmeal::getName,name);

        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Setmeal::getUpdateTime);

        //执行查询
        //传进对象和查询条件，然后MP会自动处理将total和records等赋好值
        setmealService.page(pageInfo,lambdaQueryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");

        List<Setmeal> records = pageInfo.getRecords();
        //stream流方式遍历
        List<SetmealDto> list = records.stream().map((item) ->{
            SetmealDto setmealDto = new SetmealDto();

            BeanUtils.copyProperties(item,setmealDto);

            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }


            return setmealDto;

        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(list);

        //将Page类对象的数据返回
        return R.success(setmealDtoPage);
    }

    /*
    在开发代码之前，需要梳理一下修改菜品时前端页面（add.html)和服务端的交互过程:
    1、页面发送ajax请求，请求服务端获取分类数据，用于菜品分类下拉框中数据展示
    2、页面发送ajax请求，请求服务端，根据id查询当前菜品信息，用于菜品信息回显
    3、页面发送请求，请求服务端进行图片下载，用于页图片回显
    4、点击保存按钮，页面发送ajax请求，将修改后的菜品相关数据以json形式提交到服务端
    开发修改菜品功能，其实就是在服务端编写代码去处理前端页面发送的这4次请求即可。

    */
    /**
     * 根据id查询套餐信息和对应的套餐信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    /**
     * 修改菜品
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){


        log.info(setmealDto.toString());
        setmealService.updateWithDish(setmealDto);


        return R.success("套餐添加成功");
    }


    /**
     * 套餐批量删除和单个删除
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        //删除菜品  这里的删除是逻辑删除
        setmealService.deleteByIds(ids);
        //删除菜品对应的口味  也是逻辑删除
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(queryWrapper);
        return R.success("菜品删除成功");
    }

    /**
     * 对套餐批量或者是单个 进行停售或者是起售
     * @return
     */
    @PostMapping("/status/{status}")
    //这个参数这里一定记得加注解才能获取到参数，否则这里非常容易出问题
    public R<String> status(@PathVariable("status") Integer status,@RequestParam List<Long> ids){
        //log.info("status:{}",status);
        //log.info("ids:{}",ids);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.in(ids !=null,Setmeal::getId,ids);
        //根据数据进行批量查询
        List<Setmeal> list = setmealService.list(queryWrapper);

        for (Setmeal setmeal : list) {
            if (setmeal != null){
                setmeal.setStatus(status);
                setmealService.updateById(setmeal);
            }
        }
        return R.success("售卖状态修改成功");
    }

    /**
     * 根据条件查询对应的套餐 数据
     * @param setmeal
     * @return
     */
    //可以设置传入 Long categeryid 但dish中有这个参数，为了通用用dish
    @GetMapping("/list")
    //在方法执行前spring先查看缓存中是否有数据，如果有数据，则直接返回缓存数据;若没有数据，调用方法并将方法返回值放到缓存中
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_'+ #setmeal.status" )
    public R<List<Setmeal>> list(Setmeal setmeal){
        //条件构造器
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        lambdaQueryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        lambdaQueryWrapper.eq(Setmeal::getStatus,1);
        //添加排序条件
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(lambdaQueryWrapper);
        return R.success(list);
    }



}
