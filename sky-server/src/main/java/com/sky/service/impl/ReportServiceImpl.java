package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        // 拼接dateList
        List<LocalDate> dateList = getDatelist(begin, end);
        String dateListString = StringUtils.join(dateList, ",");

        // 查询日期间订单(当前date下,全部"已完成"订单,的sum)
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 设置查询日期的起始值
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 取date日期的最小值,无限趋近与前一天24点
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 创建map,将参数组合进行查询
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover; // 如果当天营业额为0,显示值为0.0,而不是null
            turnoverList.add(turnover);
        }
        String turnoverListString = StringUtils.join(turnoverList, ",");

        // 拼接返回数据
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListString)
                .build();
        return turnoverReportVO;
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 拼接dateList
        List<LocalDate> dateList = getDatelist(begin, end);
        String dateListString = StringUtils.join(dateList, ",");

        List<Integer> newUserList = new ArrayList<>(); // newUserlist
        List<Integer> totalUserList = new ArrayList<>(); // totaluserlist

        for (LocalDate date : dateList) {
            // 设置查询日期的起始值
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 取date日期的最小值,无限趋近与前一天24点
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 创建map,将参数组合进行查询
            Map map = new HashMap();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateListString)
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
        return userReportVO;
    }

    /**
     * 根据begin和end日期封装datelist
     * @param begin
     * @param end
     * @return
     */
    private List<LocalDate> getDatelist(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        do {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        while (!begin.equals(end));
        return dateList;
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getordersStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = getDatelist(begin, end);
        String dateListString = StringUtils.join(dateList, ",");

        // 每天的数据详情
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 设置查询日期的起始值
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            // 订单总数
            Integer orderCount = orderMapper.countByMap(map);
            orderCountList.add(orderCount);
            // 有效订单数=已完成订单数
            map.put("status", Orders.COMPLETED);
            Integer validOrder = orderMapper.countByMap(map);
            validOrderCountList.add(validOrder);
        }

        // 订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        // 有效订单数=已完成订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        // 订单完成率
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(dateListString)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ",")).build();

    }

    /**
     * 根据时间选择区间,查询销量排名top10(包括菜品和套餐),降序展示
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end){
        Map map = new HashMap();
        map.put("begin", LocalDateTime.of(begin, LocalTime.MIN));
        map.put("end", LocalDateTime.of(end, LocalTime.MAX));
        map.put("status", Orders.COMPLETED);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getTop10Dish(map);

        // List<String> nameList = new ArrayList<>();
        // List<Integer> numberList = new ArrayList<>();
        // for (GoodsSalesDTO goodsSalesDTO : goodsSalesDTOList) {
        //     nameList.add(goodsSalesDTO.getName());
        //     numberList.add(goodsSalesDTO.getNumber());
        // }

        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();

    }

}
