package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */

    public void add(ShoppingCartDTO shoppingCartDTO){

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 获取当前userId
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        // 判断当前用户的购物车中，该商品是否存在
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);

        if (cartList != null && cartList.size() > 0){
            // 存在，update，修改number
            ShoppingCart cart = cartList.get(0); // 只可能有一条数据
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else {
            // 不存在，insert
            // 判断当前是dish还是setmeal
            Long dishId = shoppingCart.getDishId();
            if (dishId != null){
                // 添加的是dish
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                // 添加的是setmeal
                Long setmealId = shoppingCartDTO.getSetmealId();
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> listCart(){
        // 获取当前userId
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                                    .userId(userId)
                                    .build();
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        return cartList;
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    public void delete(ShoppingCartDTO shoppingCartDTO){
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        // 获取当前userId
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        // 查询要修改的购物车数据
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        if (cartList != null && cartList.size() > 0) {
            shoppingCart = cartList.get(0);

            Integer number = shoppingCart.getNumber();
            if (number == 1){
                shoppingCartMapper.deleteById(shoppingCart.getId());
            } else {
                shoppingCart.setNumber(shoppingCart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }


    /**
     *  清空购物车
     */
    public void clean(){
        // 获取当前userId
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.delete(userId);
    }

}

