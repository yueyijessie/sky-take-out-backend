package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
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
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO){

        // 处理业务异常（1.地址为空，购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 处理业务异常（2.购物车为空）
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        if (cartList == null && cartList.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // orders表插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders); // cpoy properties value

        orders.setOrderTime(LocalDateTime.now()); // ordertime
        orders.setPayStatus(Orders.UN_PAID); // paystatus = 0
        orders.setStatus(Orders.PENDING_PAYMENT); // status = 1
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // number订单号
        orders.setPhone(addressBook.getPhone()); // phone, address, consignee
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId); // userid, username


        orderMapper.insert(orders);

        // order_detail表插入数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : cartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        // 清空这个用户的购物车
        shoppingCartMapper.delete(userId);

        // 组建并返回orderVO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                                        .id(orders.getId())
                                        .orderAmount(orders.getAmount())
                                        .orderNumber(orders.getNumber())
                                        .orderTime(orders.getOrderTime())
                                        .build();
        return orderSubmitVO;

    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        // JSONObject jsonObject = weChatPayUtil.pay(
        //         ordersPaymentDTO.getOrderNumber(), //商户订单号
        //         new BigDecimal(0.01), //支付金额，单位 元
        //         "苍穹外卖订单", //商品描述
        //         user.getOpenid() //微信用户的openid
        // );
        //
        // if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
        //     throw new OrderBusinessException("该订单已支付");
        // }
        //


        paySuccess(ordersPaymentDTO.getOrderNumber()); // 假设订单支付成功
        JSONObject jsonObject = new JSONObject();

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

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
    }

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        // 设置userid
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        // 查询全部订单
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        // 创建orderDetail list用于返回
        List<OrderVO> list = new ArrayList();

        // 订单存在
        if (page != null && page.getTotal() > 0){
            // 查询订单的order_details
            List<Orders> orders = page.getResult();
            for (Orders order : orders) {
                Long orderId = order.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO getDetailsById(Long id){
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }


}
