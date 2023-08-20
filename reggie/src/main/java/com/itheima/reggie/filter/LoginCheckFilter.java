package com.itheima.reggie.filter;


import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.HandshakeResponse;
import java.io.IOException;

/**
 * //完善登录功能(检查用户是否已经完成登录)
 *   //使用过滤器（web组件）或者拦截器（springmvc），在过滤器或者拦截器中判断用户是否已经完成登录，
 *   //如果没有登录则跳转到登录页面。
 * 实现步骤:
 * 1、创建自定义过滤器LoginCheckFilter
 * 2、在启动类上加入注解@ServletComponentScan
 * 3、完善过滤器的处理逻辑
 */

@Slf4j
//拦截路径全部
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //过滤器具体的处理逻辑如下:
        //1、获取本次请求的URI(统一资源标识符)
        String requestURI = request.getRequestURI();

        log.info("拦截请求： {}",requestURI);
        //定义不需要过滤的请求路径
        String[] urls = new String[]{
            "/employee/login",
            "/employee/logout",
            "/backend/**",//静态资源加载没关系，看不到数据
            "/front/**",
            "/common/**",
            "/user/sendMsg",
            "/user/login",
            "/doc.html",
            "/webjars/**",
            "/swagger-resources",
            "/v2/api-docs"
        };
        //2、判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        //3、如果不需要处理，则直接放行
        if(check){
            log.info("本次请求{}不需要处理",requestURI);
            filterChain.doFilter(request,response);
            return;
        }
        //4-1、判断服务端用户登录状态，如果已登录，则直接放行
        if(request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，用户的id为：{}",request.getSession().getAttribute("employee"));

            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            //long id = Thread.currentThread().getId();
            //log.info("线程id为：{}",id);

            filterChain.doFilter(request,response);
            return;
        }

        //4-2、判断移动端用户登录状态，如果已登录，则直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户的id为：{}",request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            //long id = Thread.currentThread().getId();
            //log.info("线程id为：{}",id);

            filterChain.doFilter(request,response);
            return;
        }

        log.info("用户未登录");
        //5、如果未登录则返回未登录结果,通过输出流的方式向客户端页面响应
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;



    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for (String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match) {
                return true;
            }
        }
        return false;
    }
}
