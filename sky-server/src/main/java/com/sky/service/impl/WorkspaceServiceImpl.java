package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 查询今日运营数据
     *
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        //查询营业额
        Map map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;
        //查询有效订单数
        Integer validOrderCount = orderMapper.getOrdersCount(begin, end, Orders.COMPLETED);
        //订单完成率,先查询总订单
        Integer totalOrderCount = orderMapper.getOrdersCount(begin, end, null);
        Double orderCompleteRate = 0.0;
        Double unitPrice = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            orderCompleteRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }
        //新增用户数
        Map map2 = new HashMap<>();
        map2.put("begin", begin);
        map2.put("end", end);
        Integer newUsers = userMapper.countByMap(map2);

        return BusinessDataVO.builder()
                .orderCompletionRate(orderCompleteRate)
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .newUsers(newUsers)
                .unitPrice(unitPrice)
                .build();
    }

    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOverviewOrder() {
        //全部订单
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));
        map.put("status", null);
        Integer allOrders = orderMapper.countByMap(map);
        //已取消数量
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);
        //已完成数量
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);
        //待派送数量
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(map);
        //待接单数量
        map.put("status", Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.countByMap(map);
        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealsOverview() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);//起售
        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishesoverview() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);
        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);
        return DishOverViewVO.builder().sold(sold)
                .discontinued(discontinued).build();
    }
}
