package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                .totalUserList(StringUtils.join(totalUserList))
                .newUserList(StringUtils.join(newUserList))
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

}
