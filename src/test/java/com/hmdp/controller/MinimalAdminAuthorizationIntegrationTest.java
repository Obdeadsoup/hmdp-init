package com.hmdp.controller;

import com.hmdp.config.MvcConfig;
import com.hmdp.config.WebExceptionAdvice;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillVoucherCreateDTO;
import com.hmdp.dto.ShopCreateDTO;
import com.hmdp.dto.ShopUpdateDTO;
import com.hmdp.dto.VoucherCreateDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.interceptor.RoleInterceptor;
import com.hmdp.service.IShopService;
import com.hmdp.service.IUserService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.UserActiveRecorder;
import com.hmdp.utils.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-01 的 MVC 边界测试：真实执行 Refresh/Login/Role 三个拦截器，
 * 但不依赖 MySQL、Redis 容器。
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = MinimalAdminAuthorizationIntegrationTest.TestMvcConfiguration.class)
class MinimalAdminAuthorizationIntegrationTest {

    private static final String USER_TOKEN = "sec01-user-token";
    private static final String ADMIN_TOKEN = "sec01-admin-token";

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserActiveRecorder userActiveRecorder;
    @Autowired
    private IUserService userService;
    @Autowired
    private IShopService shopService;
    @Autowired
    private IVoucherService voucherService;

    private HashOperations<String, Object, Object> hashOperations;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        reset(stringRedisTemplate, userActiveRecorder, userService, shopService, voucherService);
        hashOperations = mock(HashOperations.class);
        given(stringRedisTemplate.opsForHash()).willReturn(hashOperations);

        mockLogin(USER_TOKEN, 101L);
        mockLogin(ADMIN_TOKEN, 102L);
        given(userService.getById(101L)).willReturn(user(101L, UserRoles.USER));
        given(userService.getById(102L)).willReturn(user(102L, UserRoles.ADMIN));
    }

    @Test
    void anonymousWriteRequestsAreUnauthorized() throws Exception {
        mockMvc.perform(post("/shop").contentType(MediaType.APPLICATION_JSON)
                        .content(validShopJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/shop").contentType(MediaType.APPLICATION_JSON)
                        .content(validShopUpdateJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/voucher").contentType(MediaType.APPLICATION_JSON)
                        .content(validVoucherJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/voucher/seckill").contentType(MediaType.APPLICATION_JSON)
                        .content(validSeckillVoucherJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserWriteRequestsAreForbidden() throws Exception {
        mockMvc.perform(post("/shop").header("authorization", USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validShopJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/shop").header("authorization", USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validShopUpdateJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/voucher").header("authorization", USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validVoucherJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/voucher/seckill").header("authorization", USER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validSeckillVoucherJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndUpdateShopAndVouchers() throws Exception {
        given(shopService.saveShop(any(ShopCreateDTO.class))).willReturn(Result.ok(201L));
        given(shopService.updateShop(any(ShopUpdateDTO.class))).willReturn(Result.ok());
        given(voucherService.addVoucher(any(VoucherCreateDTO.class)))
                .willReturn(Result.ok(301L));
        given(voucherService.addSeckillVoucher(any(SeckillVoucherCreateDTO.class)))
                .willReturn(Result.ok(302L));

        mockMvc.perform(post("/shop").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validShopJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(201));
        mockMvc.perform(put("/shop").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validShopUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/voucher").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validVoucherJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(301));
        mockMvc.perform(post("/voucher/seckill").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(validSeckillVoucherJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(302));
    }

    @Test
    void configuredPublicReadsDoNotRequireLogin() throws Exception {
        given(shopService.queryById(1L)).willReturn(Result.ok(new Shop().setId(1L)));
        given(shopService.queryShopByType(1, 1, null, null)).willReturn(Result.ok());
        given(voucherService.queryVoucherOfShop(1L)).willReturn(Result.ok());

        mockMvc.perform(get("/shop/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/shop/of/type").param("typeId", "1").param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/shop/of/name").param("name", "SEC-01").param("current", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(get("/voucher/list/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void dtoWhitelistPreventsClientFromSettingControlledFields() throws Exception {
        given(shopService.saveShop(any(ShopCreateDTO.class))).willReturn(Result.ok());
        given(shopService.updateShop(any(ShopUpdateDTO.class))).willReturn(Result.ok());
        given(voucherService.addVoucher(any(VoucherCreateDTO.class))).willReturn(Result.ok());
        given(voucherService.addSeckillVoucher(any(SeckillVoucherCreateDTO.class))).willReturn(Result.ok());

        mockMvc.perform(post("/shop").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validShopJson().replace("}",
                                ",\"id\":999,\"sold\":88,\"comments\":77,\"score\":66,\"createTime\":\"2020-01-01T00:00:00\"}")))
                .andExpect(status().isOk());
        ArgumentCaptor<ShopCreateDTO> createdShop = ArgumentCaptor.forClass(ShopCreateDTO.class);
        then(shopService).should().saveShop(createdShop.capture());
        assertThat(createdShop.getValue().getName()).isEqualTo("SEC-01 商铺");
        assertThat(Arrays.stream(ShopCreateDTO.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("id", "sold", "comments", "score", "createTime", "updateTime");

        mockMvc.perform(put("/shop").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validShopUpdateJson().replace("}",
                                ",\"sold\":88,\"comments\":77,\"score\":66,\"updateTime\":\"2020-01-01T00:00:00\"}")))
                .andExpect(status().isOk());
        ArgumentCaptor<ShopUpdateDTO> updatedShop = ArgumentCaptor.forClass(ShopUpdateDTO.class);
        then(shopService).should().updateShop(updatedShop.capture());
        assertThat(updatedShop.getValue().getId()).isEqualTo(1L);
        assertThat(Arrays.stream(ShopUpdateDTO.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("sold", "comments", "score", "createTime", "updateTime");

        mockMvc.perform(post("/voucher").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVoucherJson().replace("}",
                                ",\"id\":999,\"type\":1,\"status\":3,\"createTime\":\"2020-01-01T00:00:00\"}")))
                .andExpect(status().isOk());
        ArgumentCaptor<VoucherCreateDTO> normalVoucher = ArgumentCaptor.forClass(VoucherCreateDTO.class);
        then(voucherService).should().addVoucher(normalVoucher.capture());
        assertThat(normalVoucher.getValue().getShopId()).isEqualTo(1L);
        assertThat(Arrays.stream(VoucherCreateDTO.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("id", "type", "status", "createTime", "updateTime");

        mockMvc.perform(post("/voucher/seckill").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSeckillVoucherJson().replace("}",
                                ",\"id\":999,\"type\":0,\"status\":3,\"updateTime\":\"2020-01-01T00:00:00\"}")))
                .andExpect(status().isOk());
        ArgumentCaptor<SeckillVoucherCreateDTO> seckillVoucher = ArgumentCaptor.forClass(SeckillVoucherCreateDTO.class);
        then(voucherService).should().addSeckillVoucher(seckillVoucher.capture());
        assertThat(seckillVoucher.getValue().getStock()).isEqualTo(10);
        assertThat(Arrays.stream(SeckillVoucherCreateDTO.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("id", "type", "status", "createTime", "updateTime");
    }

    @Test
    void invalidWriteRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/shop").header("authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"typeId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void mockLogin(String token, Long userId) {
        Map<Object, Object> userMap = new HashMap<>();
        userMap.put("id", String.valueOf(userId));
        userMap.put("nickName", "sec01-user-" + userId);
        userMap.put("icon", "");
        given(hashOperations.entries(LOGIN_USER_KEY + token)).willReturn(userMap);
    }

    private User user(Long id, String role) {
        return new User().setId(id).setRole(role);
    }

    private String validShopJson() {
        return "{\"name\":\"SEC-01 商铺\",\"typeId\":1,\"images\":\"/imgs/shop.jpg\","
                + "\"area\":\"西湖区\",\"address\":\"测试路 1 号\",\"x\":120.1,\"y\":30.2,"
                + "\"avgPrice\":100,\"openHours\":\"10:00-22:00\"}";
    }

    private String validShopUpdateJson() {
        return "{\"id\":1,\"name\":\"SEC-01 更新商铺\"}";
    }

    private String validVoucherJson() {
        return "{\"shopId\":1,\"title\":\"SEC-01 普通券\",\"subTitle\":\"测试副标题\","
                + "\"rules\":\"测试规则\",\"payValue\":100,\"actualValue\":200}";
    }

    private String validSeckillVoucherJson() {
        return "{\"shopId\":1,\"title\":\"SEC-01 秒杀券\",\"subTitle\":\"测试副标题\","
                + "\"rules\":\"测试规则\",\"payValue\":100,\"actualValue\":200,\"stock\":10,"
                + "\"beginTime\":\"2030-01-01T10:00:00\",\"endTime\":\"2030-01-01T11:00:00\"}";
    }

    @Configuration
    @EnableWebMvc
    @Import({MvcConfig.class, WebExceptionAdvice.class})
    static class TestMvcConfiguration {

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        UserActiveRecorder userActiveRecorder() {
            return mock(UserActiveRecorder.class);
        }

        @Bean
        IUserService userService() {
            return mock(IUserService.class);
        }

        @Bean
        IShopService shopService() {
            return mock(IShopService.class);
        }

        @Bean
        IVoucherService voucherService() {
            return mock(IVoucherService.class);
        }

        @Bean
        RoleInterceptor roleInterceptor(IUserService userService) {
            RoleInterceptor interceptor = new RoleInterceptor();
            ReflectionTestUtils.setField(interceptor, "userService", userService);
            return interceptor;
        }

        @Bean
        ShopController shopController(IShopService shopService) {
            ShopController controller = new ShopController();
            controller.shopService = shopService;
            return controller;
        }

        @Bean
        VoucherController voucherController(IVoucherService voucherService) {
            VoucherController controller = new VoucherController();
            ReflectionTestUtils.setField(controller, "voucherService", voucherService);
            return controller;
        }
    }
}
