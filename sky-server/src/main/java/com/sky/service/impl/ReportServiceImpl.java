package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private WorkspaceService workspaceService; // 在service中也可以调用其他service

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

    /**
     * 导出excel报表接口
     * @param response
     */
    public void export(HttpServletResponse response){
        // 1.查询数据库,获取营业数据---最近30天的数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN), LocalDateTime.of(end,LocalTime.MAX));


        // 2.通过POI,将数据集写入excel
        // 获得文件输入流
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/exportTemplate.xlsx");
        // 写入数据
        try {
            // 基于末班文件,在内存中,创建一个新的excel
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1"); // 获得sheet

            // 填充数据(概览数据)
            sheet.getRow(1).getCell(1).setCellValue("时间: " + begin + "至" + end); // 时间(第二行第二格)
            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover()); // 营业额
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate()); // 订单完成率
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers()); // 新增用户数
            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount()); // 有效订单数
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice()); // 平均客单价

            // 填充数据(明细数据)
            List<LocalDate> dateList = getDatelist(begin, LocalDate.now()); // 日期列表
            int i = 7; // 从第八行开始填充
            for (LocalDate date : dateList) {
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                // 循环填充(2-7列)
                XSSFRow row = sheet.getRow(i); // 获取当前行
                row.getCell(1).setCellValue(String.valueOf(date));
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
                i++; // 填充完,行数+1
            }

            // 3.通过输出流,将excel下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 4.关闭资源
            out.close();
            excel.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
