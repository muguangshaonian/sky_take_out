package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单表数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 用户订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据ID查询订单
     * @param id
     * @return
     */
    @Select("select *from orders where  id = #{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 处理订单超时
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> GetByOrderStautsTimeLt(Integer status, LocalDateTime orderTime);

    /**
     * 动态查询营业额
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 动态查询订单
     * @param begin
     * @param end
     * @param status
     * @return
     */
    Integer getOrdersCount(LocalDateTime begin, LocalDateTime end, Integer status);

    /**
     * 动态查询销量前10
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin,LocalDateTime end);

    /**
     * 动态查询订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
