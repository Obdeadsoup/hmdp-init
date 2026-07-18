package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.SetPasswordDTO;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.PasswordEncoder;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.connection.BitFieldSubCommands;

import javax.annotation.Resource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;
// 这里的@Service注解是为了让Spring能够扫描到这个类，并将其作为一个Bean进行管理。这样在其他地方需要使用IUserService的时候，Spring就能够自动注入这个实现类。
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    // 注入依赖
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Bitmap签到所需的常量
    private static final DateTimeFormatter SIGN_MONTH_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMM");

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
        // codex给出的路线是技术路线,很多重复性的部分就没有展开,我这里补一下密码登录业务
        String phone=loginForm.getPhone();
        String code=loginForm.getCode();
        String password=loginForm.getPassword();

        // 1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("Invalid phone");
        }
        boolean hasCode=StrUtil.isNotBlank(code);
        boolean hasPassword=StrUtil.isNotBlank(password);

        /**
         * hasCode==hasPassword有两种情况,但两种情况显然都不符合要求,直接pass
         */
        if(hasCode==hasPassword){
            return Result.fail("Error login form");
        }

        User user;
        // 2.验证码登录
        if(hasCode){
            String cacheCode=stringRedisTemplate
                .opsForValue().get(LOGIN_CODE_KEY+phone);

            if(StrUtil.isBlank(cacheCode)||!cacheCode.equals(code)){
                return Result.fail("验证码错误或已过期");
            }

            // User user=query().eq("phone",phone).one();
            user =query()
                .eq("phone",phone)
                .one();

            // 验证码可以验证手机号归属,当前情况下用户不存在则直接创建    
            if(user==null){
                user=createUserWithPhone(phone);
            }
            // 通过手机+密码登录时不需要存验证码,所以这里验证码用完可以直接删除防止重复使用
            stringRedisTemplate.delete(LOGIN_CODE_KEY+phone);
        }
        // 3.密码登录
        else{
            if(RegexUtils.isPasswordInvalid(password)){
                return Result.fail("密码应为4~32位的字母、数字、下划线");
            }
            user =query()
                .eq("phone",phone)
                .one();
            // 不允许直接根据密码和手机号直接注册,直接返回错误
            if(user==null){
                return Result.fail("当前手机号尚未注册账号");
            }

            // 用户存在,但密码为空,此时需要跳转验证码登录界面,并在验证码登录后设置密码
            if(StrUtil.isBlank(user.getPassword())){
                return Result.fail("当前账号尚未设置密码,请使用验证码登录后设置密码");
            }

            // 将前端输入密码原文与数据库中加密密码比较
            if(!PasswordEncoder.matches(user.getPassword(),password)){
                return Result.fail("手机号或密码错误");
            }
        }

        String token =createToken(user);
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

    @Override
    public Result setPassword(SetPasswordDTO form){
        if(form==null){
            return Result.fail("密码不能为空");
        }
        String password=form.getPassword();
        String confirmPassword=form.getConfirmPassword();

        if(RegexUtils.isPasswordInvalid(password)){
            return Result.fail("密码应为4～32位字母、数字或下划线");
        }
        if(!password.equals(confirmPassword)){
            return Result.fail("两次输入密码不一致");
        }

        // 通过UserHolder获取当前用户
        UserDTO currentUser=UserHolder.getUser();
        if(currentUser==null){
            return Result.fail("当前用户未登录");
        }

        // 加密密码明文并更新数据库(MyBatis-Plus版)
        String encodedPassword=PasswordEncoder.encode(password);

        boolean success=lambdaUpdate()
                .eq(User::getId,currentUser.getId())
                .set(User::getPassword,encodedPassword)
                .update();

        if(!success){
            return Result.fail("密码设置失败");
        }

        return Result.ok();
    }
    // 签到
    @Override
    public Result sign(){
        // 1. 获取当前登录用户
        UserDTO loginUser=UserHolder.getUser();
        if(loginUser==null){
            return Result.fail("请先登录");
        }

        // 2. 获取日期
        LocalDate today=LocalDate.now();

        // 3. 构造Bitmap key
        String key = USER_SIGN_KEY
                + loginUser.getId()
                + ":"
                + today.format(SIGN_MONTH_FORMATTER);
        
        /**
         * 4. 日期从1开始,Bitmap offset从开始
         */
        int offset =today.getDayOfMonth()-1;

        // 5. 将今天对应的bit位设置为1
        stringRedisTemplate.opsForValue()
                .setBit(key,offset,true);
        
        return Result.ok();
    }
    // 统计迄今连续签到天数
    @Override
    public Result signCount(){
        // 1. 获取当前登录用户
        UserDTO loginUser=UserHolder.getUser();
        if(loginUser==null){
            return Result.fail("请先登录");
        }
        // 2. 获取日期
        LocalDate today=LocalDate.now();
        // 3. 构造Bitmap key
        String key = USER_SIGN_KEY
                + loginUser.getId()
                + ":"
                + today.format(SIGN_MONTH_FORMATTER);
        // 4. 今天是本月第几天 ,就读取多少个bit
        int dayOfMonth = today.getDayOfMonth();

        /**
         * 5. 读取本月1号截至今天的所有签到位
         *    unsigned(dayOfMonth):将dayOfMonth个bit位解释成一个无符号整数
         *    valueAt(0):从bit 0开始读取
         */
        List<Long> result=stringRedisTemplate
                .opsForValue()
                .bitField(
                        key,
                        BitFieldSubCommands.create()
                                .get(
                                        BitFieldSubCommands
                                                .BitFieldType
                                                .unsigned(dayOfMonth)
                                )
                                .valueAt(0)
                );
        // 6. 没有签到数据
        if (result == null || result.isEmpty()) {
            return Result.ok(0);
        }
        Long number=result.get(0);

        if (number == null || number == 0L) {
            return Result.ok(0);
        }

        // 7. 从今天开始向前统计连续的1的个数
        int count=0;
        while((number&1l)==1l){
            count++;
            /**
             * 右移一位,让最低位与"1"与,当最低位变成0时循环终止
             */
            number>>>=1;
        }
        return Result.ok(count);
    }
    @Override
    public Result signTotal(){
        return Result.ok();
    }
    /**
     * 因为无论是验证码登录/注册还是密码登录,都需要使用token进行令牌申请并存入Redis
     * 故将该部分逻辑单独作为userServiceImpl的一个内部私有方法
     */ 
    private String createToken(User user){
        // 将User 转成UserDTO ,避免保存敏感字段进入Redis
        UserDTO userDTO=
            BeanUtil.copyProperties(user,UserDTO.class);
        // 生成用户专属token,作为登录令牌
        String token=UUID.randomUUID().toString(true);


        // 利用BeanUtil 将用户信息UserDTO转为Map<String,Object>类,方便存入Hash型Redis
        Map<String,Object> userMap=BeanUtil.beanToMap(
            userDTO,
            new HashMap<>(),
            CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldValueEditor(
                    (fieldName,fieldValue)->
                    fieldValue.toString())
        );
        // 加上项目Redis前缀形成完整key 
        String tokenKey=LOGIN_USER_KEY+token;

        // 将用户信息存入Redis,key为tokenKey,value为userMap,设置过期时间
        stringRedisTemplate
            .opsForHash()
            .putAll(tokenKey,userMap);
        stringRedisTemplate.expire(
            tokenKey,
            LOGIN_USER_TTL,
            TimeUnit.MINUTES
        );

        return token;
    }
}

