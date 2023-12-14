package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    @Transactional  // 需要操作两个表，需要保证事务一致性
    public void addDish(DishDTO dishDTO){

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        // 向菜品表插入1条数据
        dishMapper.addDish(dish);

        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()){
            // 遍历flavor插入dishId
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        // mybatis提供插件简化代码
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        // 将page中的结果封装成PageResult
        long total = page.getTotal();
        List<DishVO> records = page.getResult();
        return new PageResult(total, records);
    }


    /**
     * 批量删除菜品
     * @param ids
     */
    @Transactional
    public void deleteByIds(List<Long> ids){
        // 判断当前菜品是否在启售
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                // 当前菜品在启售
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断菜品是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品
        // for (Long id : ids) {
        //     dishMapper.deleteById(id);
        //     // 删除菜品对应的口味
        //     dishFlavorMapper.deleteByDishId(id);
        // }

        // 根据菜品id集合批量删除菜品数据
        // sql: delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);
        // 根据菜品id集合批量删除关联的口味
        dishFlavorMapper.deleteByDishIds(ids);

    }
}
