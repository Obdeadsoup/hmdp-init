package com.hmdp.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.ShopType;
import org.apache.ibatis.annotations.Param;


/*
    继承 MyBatis-Plus的基本类BaseMapper(MyBatis没有这些), 
    这样就可以直接使用BaseMapper提供的基本方法,比如增删改查等

    使用BaseMapper的话,基础的增删改查不需要写XML,
*/ 
public interface ShopTypeMapper extends BaseMapper<ShopType> {

    //
    List<ShopType> selectAllBySort();

    // 这里其实不需要自行定义selectById方法,因为MyBatisPlus的BaseMapper已经提供了selectById方法,可以直接调用
    ShopType selectById(long id);

    // 动态查询商铺类型 , 对应xml文件里的动态sql
    List<ShopType> search(
        @Param("name") String name,
        @Param("minsort") Integer minsort,
        @Param("maxsort") Integer maxsort
    );
}
