package com.itheima.reggie.controller;





/*
缓存菜品数据
实现思路
前面我们已经实现了移动端菜品查看功能，对应的服务端方法为DishController的list方法，
此方法会根据前端提交的查询条件进行数据库查询操作。在高并发的情况下，频繁查询数据库会导致系统性能下降，
服务端响应时间增长。现在需要对此方法进行缓存优化，提高系统的性能。
具体的实现思路如下:
1、改造DishController的list方法，先从Redis中获取菜品数据，如果有则直接返回，无需查询数据库;如果没有则查询数据库，并将查询到的菜品数据放入Redis。
2、改造DishController的save和update方法，加入清理缓存的逻辑
注意事项
在使用缓存过程中，要注意保证数据库中的数据和缓存中的数据一致,如果数据库中的数据发生变化，需要及时清理缓存数据。
* */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@Slf4j
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     *
     * 后台系统中可以管理菜品信息，通过新增功能来添加一个新的菜品，在添加菜品时需要选择当前菜品所属的菜品分类,
     * 并且需要上传菜品图片，在移动端会按照菜品分类来展示对应的菜品信息。
     *
     * 新增菜品
     * 代码开发-准备工作
     * 在开发业务功能前，先将需要用到的类和接口基本结构创建好:
     * 实体类DishFlavor(直接从课程资料中导入即可，Dish实体前面课程中已经导入过了)
     * Mapper接口DishFlavorMapper
     * 业务层接口DishFlavorService
     * 业务层实现类DishFlavorServicelmpl
     * 控制层DishController;菜品和菜品口味操作都放在一个Controller
     *
     * 在开发代码之前，需要梳理一下新增菜品时前端页面和服务端的交互过程:
     * 1、页面(backend/page/food/add.html)发送ajax请求，请求服务端获取菜品分类数据并展示到下拉框中
     * 2、页面发送请求进行图片上传，请求服务端将图片保存到服务器
     * 3、页面发送请求进行图片下载，将上传的图片进行回显
     * 4、点击保存按钮，发送ajax请求，将菜品相关数据以json形式提交到服务端
     * 开发新增菜品功能，其实就是在服务端编写代码去处理前端页面发送的这4次请求即可。
     *
     *
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){


        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);

        //清楚所有菜品的缓冲数据
        /*Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/

        //清理某个分类下面的菜品缓存数据

        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("菜品添加成功");
    }

    /**
     * 菜品信息分页查询
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
    public R<Page> page(int page, int pageSize,String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page,pageSize);

        //页面中有个categoryname，dish中没有，所以用dishdto
        Page<DishDto> dishDtoPage = new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper();
        //添加过滤条件
        lambdaQueryWrapper.like(name!=null,Dish::getName,name);

        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Dish::getUpdateTime);

        //执行查询
        //传进对象和查询条件，然后MP会自动处理将total和records等赋好值
        dishService.page(pageInfo,lambdaQueryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();
        //stre流方式遍历
        List<DishDto> list = records.stream().map((item) ->{
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }


            return dishDto;

        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        //将Page类对象的数据返回
        return R.success(dishDtoPage);
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
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){


        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);

        //清楚所有菜品的缓冲数据
        /*Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);*/

        //清理某个分类下面的菜品缓存数据

        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("菜品添加成功");
    }


    /**
     * 根据id删除单个
     * @param ids
     * @return
     */
    /*@DeleteMapping
    //前端只需返回个code即可
    public R<String> getById(Long ids){
        log.info("根据id删除菜品信息");

        dishService.removeWithFlavor(ids);

        return R.success("菜品信息删除成功");

    }*/
    /**
     * 套餐批量删除和单个删除
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        //删除菜品  这里的删除是逻辑删除
        dishService.deleteByIds(ids);
        //删除菜品对应的口味  也是逻辑删除
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId,ids);
        dishFlavorService.remove(queryWrapper);
        return R.success("菜品删除成功");
    }




    /**
     * 对单个菜品进行停售或者是起售
     * @return
     * 前端发过来的请求（使用的是post方式）：http://localhost:8080/dish/status/1?ids=1516568538387079169
     */
/*    @PostMapping("/status/{status}")
    public R<String> status(@PathVariable("status") Integer status,Long ids){
        log.info("status:{}",status);
        log.info("ids:{}",ids);
        Dish dish = dishService.getById(ids);
        if (dish != null){
            dish.setStatus(status);
            dishService.updateById(dish);
            return R.success("开始启售");
        }
        return R.error("售卖状态设置异常");
    }*/

    /**
     * 对菜品批量或者是单个 进行停售或者是起售
     * @return
     */
    @PostMapping("/status/{status}")
    //这个参数这里一定记得加注解才能获取到参数，否则这里非常容易出问题
    public R<String> status(@PathVariable("status") Integer status,@RequestParam List<Long> ids){
        //log.info("status:{}",status);
        //log.info("ids:{}",ids);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.in(ids !=null,Dish::getId,ids);
        //根据数据进行批量查询
        List<Dish> list = dishService.list(queryWrapper);

        for (Dish dish : list) {
            if (dish != null){
                dish.setStatus(status);
                dishService.updateById(dish);
            }
        }
        return R.success("售卖状态修改成功");
    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
    //可以设置传入 Long categeryid 但dish中有这个参数，为了通用用dish
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        lambdaQueryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        lambdaQueryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(lambdaQueryWrapper);
        return R.success(list);
    }
*/
    //根据条件查询对应的菜品数据，由于前端需要显示口味规格，所以需传回给前端dishdto
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dishDtoList = null;
        //动态构造key
        String key = "dish_" +dish.getCategoryId() + "_" + dish.getStatus();

        //先从redis中获取缓冲数据
        dishDtoList = (List<DishDto>)redisTemplate.opsForValue().get(key);
        //如果存在，直接返回，无需查询数据库
        if(dishDtoList != null){
            return R.success(dishDtoList);
        }



        //条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        lambdaQueryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        lambdaQueryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        lambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> listrecoeds = dishService.list(lambdaQueryWrapper);

        dishDtoList = listrecoeds.stream().map((item) ->{
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long dishId = item.getId();

            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            //SQL: select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;

        }).collect(Collectors.toList());


        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到Redis
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }


}
