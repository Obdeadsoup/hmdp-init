package com.hmdp.dto;

import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

/**
 * 后台创建普通优惠券时允许客户端提供的字段。
 */
@Data
public class VoucherCreateDTO {

    @NotNull(message = "商铺ID不能为空")
    @Positive(message = "商铺ID必须为正数")
    private Long shopId;

    @NotBlank(message = "优惠券标题不能为空")
    @Size(max = 255, message = "优惠券标题不能超过255个字符")
    private String title;

    @Size(max = 255, message = "优惠券副标题不能超过255个字符")
    private String subTitle;

    @Size(max = 1024, message = "优惠券规则不能超过1024个字符")
    private String rules;

    @NotNull(message = "支付金额不能为空")
    @PositiveOrZero(message = "支付金额不能为负数")
    private Long payValue;

    @NotNull(message = "抵扣金额不能为空")
    @PositiveOrZero(message = "抵扣金额不能为负数")
    private Long actualValue;

    @AssertTrue(message = "抵扣金额不能小于支付金额")
    public boolean isActualValueValid() {
        return payValue == null || actualValue == null || actualValue >= payValue;
    }
}
