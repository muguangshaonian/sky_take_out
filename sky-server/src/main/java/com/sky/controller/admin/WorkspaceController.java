package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作台接口
 */
@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台接口")
@Slf4j
public class WorkspaceController {
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 查询今日运营数据
     * @return
     */
    @GetMapping("/businessData")
    @ApiOperation("查询今日运营数据")
    public Result<BusinessDataVO> businessData(){
        //获取当天的开始时间和结束时间
        LocalDateTime begin  = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end  = LocalDateTime.now().with(LocalTime.MAX);
        return Result.success(workspaceService.getBusinessData(begin,end));
    }

    /**
     * 查询订单管理数据
     * @return
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("查询订单管理数据")
    public Result<OrderOverViewVO> overviewOrders(){
        return Result.success(workspaceService.getOverviewOrder());
    }

    /**
     * 查询套餐总览
     * @return
     */
    @GetMapping("/overviewSetmeals")
    @ApiOperation("查询套餐总览")
    public Result<SetmealOverViewVO> overviewSetmeals(){
        return Result.success(workspaceService.getSetmealsOverview());
    }
    /**
     * 查询菜品总览
     * @return
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("查询菜品总览")
    public Result<DishOverViewVO> overviewDishes(){
        return Result.success(workspaceService.getDishesoverview());
    }
}
