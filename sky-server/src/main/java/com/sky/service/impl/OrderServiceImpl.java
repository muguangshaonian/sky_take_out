package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    //    @Autowired
//    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO orderSubmit(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常（地址簿为空，购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //检查用户的收获地址是否超出范围
        String newAddress = addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail();
        checkOutOfRange(newAddress);
        //购物车
        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(newAddress);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orderMapper.insert(orders);
        //向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);
        //封装vo返回结果
        OrderSubmitVO build = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .build();
        return build;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 模拟调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("return_code", "SUCCESS");
        jsonObject.put("result_code", "SUCCESS");
        jsonObject.put("prepay_id", "mock_prepay_id");
        jsonObject.put("nonce_str", "mock_nonce_str");
        jsonObject.put("sign", "mock_sign");

        // 创建一个模拟的 OrderPaymentVO 对象，并设置必要的字段
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr(jsonObject.getString("nonce_str"));
        vo.setPaySign(jsonObject.getString("sign"));
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        vo.setSignType("MD5");
        vo.setPackageStr("prepay_id=" + jsonObject.getString("prepay_id"));

        return vo;
    }
    /*public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }
*/

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        //通过websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap<>();
        map.put("type",1);//1表示为来电提醒,2表示客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号为："+outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQueryUser(int pageNum, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.size() > 0) {
            for (Orders orders : page) {
                Long ordersId = orders.getId();//订单ID
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(ordersId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param id
     */
    public void cancel(Long id) {
        //根据id查询订单
        Orders orderDB = orderMapper.getById(id);
        //没有该订单，抛出异常
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //判断订单属于什么状态，已接单之后不可以取消订单
        Integer status = orderDB.getStatus();
        if (status > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        //退款功能做不了，直接修改支付状态为退款
        if (status.equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态，取消时间，取消原因
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消");
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    /**
     * 再来一单,这个id是订单id
     *
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        //查看订单的详细信息
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        //将他放在新的购物车
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        //将购物车放入数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        //需要转化菜品的信息，将Orders 转为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(),orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        //需要返回订单菜品信息
        List<OrderVO> ordersList = new ArrayList<>();
        List<Orders> pageResult = page.getResult();
        if (pageResult != null && pageResult.size() > 0) {
            for (Orders orders : pageResult) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                ordersList.add(orderVO);
            }
        }
        return ordersList;
    }

    /**
     * 根据订单ID获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        //将菜品信息返回为（宫保鸡丁*3）这种形式
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetails.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        //将订单所有菜品信息拼接在一起
        return String.join("",orderDishList);
    }
    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        //分别统计待接单、待派送、派送中的数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //- 只有订单处于“待接单”状态时可以执行拒单操作
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        Integer status = orders.getStatus();
        if (orders==null || !status.equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 商家拒单时需要指定拒单原因
        //- 商家拒单时，如果用户已经完成了支付，需要为用户退款
        Integer payStatus = orders.getPayStatus();
        if (payStatus==Orders.PAID){
            //用户已支付，需要退款（没操作）
            log.info("申请退款:退款的数目为....");
        }
        //拒绝需要更新时间，状态和拒绝原因
        Orders newOrders = new Orders();
        newOrders.setCancelTime(LocalDateTime.now());
        newOrders.setStatus(Orders.CANCELLED);
        newOrders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        newOrders.setId(orders.getId());
        orderMapper.update(newOrders);
    }

    /**
     * 取消订单，管理端
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //根据ID查询订单
        Orders orderDB = orderMapper.getById(ordersCancelDTO.getId());
        //支付状态
        Integer payStatus = orderDB.getPayStatus();
        //用户已支付需要退款（不操作）
        if (payStatus==1){
            log.info("申请退款。>>..");
        }
        //退款之后需要更新取消时间，订单状态，取消原因
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //只有待派送的才可以修改为已派送
        Orders orderDB = orderMapper.getById(id);
        Integer status = orderDB.getStatus();
        if (orderDB==null||!status.equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
//        orderDB.setStatus(Orders.CONFIRMED);
//        orderMapper.update(orderDB);这种版本容易造成空指针
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     */
    public void complete(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if (orderDB==null||!orderDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setStatus(Orders.COMPLETED);
        orders.setId(id);//orderDB.getId等同
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

//    * 检查客户的收货地址是否超出配送范围
//     * @param address
//     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 50000){
            //配送距离超过50000米
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if (orderDB==null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Map map = new HashMap();
        map.put("type",2);//2表示客户催单
        map.put("orderId",id);
        map.put("content","订单号为："+ orderDB.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
