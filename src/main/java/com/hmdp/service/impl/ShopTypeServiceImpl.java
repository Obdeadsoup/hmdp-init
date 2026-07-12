package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.dto.Result;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

// 这里用 @Service 注解,给Spring容器里也生成了一个ShopTypeServiceImpl实现类的Bean;
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    
    // 注入Mapper对象,调用Mapper方法查询数据库(私有属性,只能通过方法访问)
    // !!! 记得加上注解注入Mapper对象,否则会报空指针异常
    @Resource
    private ShopTypeMapper shopTypeMapper;

    @Override
    public ShopType getShopTypeById(Long id){
        /*
        待补全
        1. 校验参数合法性
        2. 查Redis缓存,如果存在直接返回
        3. 缓存没有就调用Mapper查数据库
        4. 查到后的数据存入Redis方便下次使用
        */
        if(id==null||id<=0){
            throw new RuntimeException("Parameter Error");
        }
        return shopTypeMapper.selectById(id);
    }

    /* 
    其实上面犯傻了, ServiceImpl已经实现了IService接口,里面有一个getById方法,
    也不需要进行Mapper注入
    可以直接调用,不需要自己写一个getShopTypeById方法,直接调用getById(id)就行了
    或者在getShopTypeById里直接返回getById(id)就行
    */

    @Override
    public Result queryTypeList(){
        List<ShopType> typeList=query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }
}
