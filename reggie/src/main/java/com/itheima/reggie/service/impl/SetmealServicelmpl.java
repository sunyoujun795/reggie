package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServicelmpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {


    @Autowired
    private SetmealDishService setmealDishService;

    @Transactional//开启事务；操作了多张表,保证数据的一致性
    public void saveWithDish(SetmealDto setmealDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(setmealDto);

        Long setmealId = setmealDto.getId();//菜品Id

        //菜品口味
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     *根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        //查询菜品的基本信息到菜品表dish
        Setmeal setmeal = this.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);

        //查询菜品口味数据到菜品口味表dish_flavor
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper= new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        List<SetmealDish> dishs = setmealDishService.list(setmealDishLambdaQueryWrapper);
        setmealDto.setSetmealDishes(dishs);

        return setmealDto;

    }

    /**
     * 修改套餐
     * @param setmealDto
     */
    @Transactional
    public void updateWithDish(SetmealDto setmealDto) {
        //更新dish表基本信息
        this.updateById(setmealDto);//因为dishDto继承了dish

        //清理当前菜品对应口味数据----dish_flavor表的delete操作
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());

        setmealDishService.remove(lambdaQueryWrapper);

        //更新dishflavors表基本信息
        //添加当前提交过来的口味数据----dish_flavor表的insert操作
        List<SetmealDish> dishes = setmealDto.getSetmealDishes();

        dishes = dishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(dishes);
    }

    /**
     *套餐批量删除和单个删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        //构造条件查询器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //先查询该菜品是否在售卖，如果是则抛出业务异常
        queryWrapper.in(ids!=null,Setmeal::getId,ids);
        List<Setmeal> list = this.list(queryWrapper);
        for (Setmeal setmeal : list) {
            Integer status = setmeal.getStatus();
            //如果不是在售卖,则可以删除
            if (status == 0){
                this.removeById(setmeal.getId());
            }else {
                //此时应该回滚,因为可能前面的删除了，但是后面的是正在售卖
                throw new CustomException("删除套餐中有正在售卖菜品,无法全部删除");
            }
        }
    }



}
