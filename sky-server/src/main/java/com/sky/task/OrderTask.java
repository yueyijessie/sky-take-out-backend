package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自定义定时任务类,定时处理订单状态
 */
@Component // 实例化，交给spring容器管理
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 用户待支付,处理超时未支付订单的方法
     */
    @Scheduled(cron = "0 * * * * ? ") // 每分钟触发一次
    public void processTimeoutOrder(){
        log.info("定时处理[待支付]超时订单:{}", LocalDateTime.now());
        // select * from orders where status = ? and order_time < (当前时间-15分钟)
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getTimeoutOrders(Orders.PENDING_PAYMENT, time);
        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时,自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 派送中,超时未处理的订单
     */
    @Scheduled(cron = "0 0 1 * * ? ") // 每天凌晨一点触发
    public void processDeliveryOrder(){
        log.info("定时处理[派送中]超时订单:{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getTimeoutOrders(Orders.DELIVERY_IN_PROGRESS, time);
        if (ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }

}
