package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 根据分类Id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类Id查询菜品")
    public Result<List<DishVO>> list(Long categoryId){
        log.info("根据分类Id查询菜品:{}",categoryId);
        String key = "dish_"+categoryId;
        //查询缓存是否有菜品
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        //如果存在，无需查询数据库，直接返回
        if(list!=null && list.size()>0){
            return Result.success(list);
        }
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        //如果不存在，查询数据库，并将查询到的数据加入缓存当中
        list = dishService.listWithFlavors(dish);
        redisTemplate.opsForValue().set(key,list);
        return Result.success(list);
    }


}
