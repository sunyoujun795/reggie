package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealDishMapper;
import com.itheima.reggie.service.SetmealDishService;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

@Service
public class SetmealDishServicelmpl extends ServiceImpl<SetmealDishMapper, SetmealDish> implements SetmealDishService {
}
