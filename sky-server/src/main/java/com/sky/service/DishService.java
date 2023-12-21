package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    void addDish(DishDTO dishDTO);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据id查询菜品和对应口味
     * @param id
     * @return
     */
    DishVO getById(Long id);

    /**
     * 根据id修改菜品和口味
     * @param dishDTO
     */
    void updateDish(DishDTO dishDTO);

    /**
     * 菜品启售，停售
     * @param status
     * @param id
     */
    void editDishStatus(Integer status, Long id);
}
