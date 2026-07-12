package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;
// 这里的@Service注解是为了让Spring能够扫描到这个类，并将其作为一个Bean进行管理。这样在其他地方需要使用IUserService的时候，Spring就能够自动注入这个实现类。
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    // 
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone){
        // 利用工具类RegexUtils来验证手机号是否合法
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("Invalid phone");
        }
        // 生成验证码并将手机号拼接成key ,将key和验证码存入Redis,设置过期时间为2分钟;
        String code=RandomUtil.randomNumbers(6);

        String key=LOGIN_CODE_KEY+phone;

        stringRedisTemplate.opsForValue().set(key,code,LOGIN_CODE_TTL,TimeUnit.MINUTES);

        // 这个地方等7.12白天再实现发送验证码功能,这里先打个框架
        /**
         * 
         * 
         */
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm){
        String phone=loginForm.getPhone();
        String code=loginForm.getCode();

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("Invalid phone");
        }

        String cacheCode=stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);

        if(StrUtil.isBlank(cacheCode)||!cacheCode.equals(code)){
            return Result.fail("Code error");
        }

        User user=query().eq("phone",phone).one();

        if(user==null){
            user=createUserWithPhone(phone);
        }

        // 将User 转成UserDTO ,避免保存敏感字段
        UserDTO userDTO=BeanUtil.copyProperties(user,UserDTO.class);
        // 生成用户专属token,作为登录令牌
        String token=UUID.randomUUID().toString(true);


        // 利用BeanUtil 将用户信息UserDTO转为Map<String,Object>类,方便存入Hash型Redis
        Map<String,Object> userMap=BeanUtil.beanToMap(
            userDTO,
            new HashMap<>(),
            CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString())
        );
        String tokenKey=LOGIN_USER_KEY+token;

        // 将用户信息存入Redis,key为tokenKey,value为userMap,设置过期时间
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);

        // 删除验证码避免重复使用
        stringRedisTemplate.delete(LOGIN_CODE_KEY+phone);

        // 将token返回前端,后续前端在发送请求时携带该token作为令牌
        return Result.ok(token);
    }

    @Override
    public Result logout(String token){
        // 发起的logout请求中也会携带token,为空直接返回,不为空则将Redis中缓存的用户登录信息删除再返回;
        if(StrUtil.isBlank(token)){
            return Result.ok();
        }
        
        stringRedisTemplate.delete(LOGIN_USER_KEY+token);
        return Result.ok();
    }

    // 私有方法,在服务内部调用根据手机号创建用户
    private User createUserWithPhone(String phone){
        User user=new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}

