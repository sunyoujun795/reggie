package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理
 */
//拦截类上加了RestController的注解的Controller类
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody//需要写一个方法，最终返回json数据
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理，一旦被拦截到此异常，就会统一在这处理
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHander(SQLIntegrityConstraintViolationException ex) {
        log.error(ex.getMessage());

        if (ex.getMessage().contains("Duplicate entry")) {
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }
        return R.error("未知错误");

    }

    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHander(CustomException ex) {
        log.error(ex.getMessage());


        return R.error(ex.getMessage());

    }

}
