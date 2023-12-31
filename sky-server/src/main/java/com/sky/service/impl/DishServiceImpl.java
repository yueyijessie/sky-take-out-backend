package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Employee;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

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

    /**
     * 根据id查询菜品和对应口味
     * @param id
     * @return
     */
    public DishVO getById(Long id){
        Dish dish = dishMapper.getById(id);
        List<DishFlavor> flavorList = dishFlavorMapper.getByDishId(id);

        // 封装dishvo
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(flavorList);

        return dishVO;
    }

    /**
     * 根据id修改菜品和口味
     * @param dishDTO
     */
    @Transactional
    public void updateDish(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改dish基本信息
        dishMapper.update(dish);

        // 删除口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 重新插入口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()){
            // 遍历flavor插入dishId
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品启售，停售
     * @param status
     * @param id
     */
    @Transactional
    public void editDishStatus(Integer status, Long id){
        Dish dish = Dish.builder()
                .status(status)
                .id(id)
                .build();
        // 修改菜品
        dishMapper.update(dish);
        // 判断停售，停售响应套餐
        if (status == StatusConstant.DISABLE){
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            List<Long> setMealList = setmealDishMapper.getSetmealIdsByDishIds(ids);
            if (setMealList != null && setMealList.size() > 0){
                for (Long setMealId : setMealList) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setMealId)
                            .status(status)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

    /**
     * 根据categoryid查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> getByCategoryId(Integer categoryId){
        List<Dish> dishList = dishMapper.getByCategoryId(categoryId);
        return dishList;
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}
