package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */

// 这里的@Service注解是为了让Spring能够扫描到这个类，并将其作为一个Bean进行管理。这样在其他地方需要使用IUserService的时候，Spring就能够自动注入这个实现类。
@Service

// 这里的继承的ServiceImpl<UserMapper,User>是MyBatis-Plus通用实现类
// 表明该类自动实现CRUD操作
// UserMapper对应操作数据库的mapper,User是数据库表的实体类

//implements实现自己定义的IUserService接口,支持自行在IUserService接口里添加扩展
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}

