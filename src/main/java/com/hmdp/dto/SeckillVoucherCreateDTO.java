package com.hmdp.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * 后台创建秒杀优惠券时允许客户端提供的字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillVoucherCreateDTO extends VoucherCreateDTO {

    @NotNull(message = "秒杀库存不能为空")
    @Positive(message = "秒杀库存必须为正数")
    private Integer stock;

    @NotNull(message = "秒杀开始时间不能为空")
    private LocalDateTime beginTime;

    @NotNull(message = "秒杀结束时间不能为空")
    private LocalDateTime endTime;

    @AssertTrue(message = "秒杀结束时间必须晚于开始时间")
    public boolean isTimeRangeValid() {
        return beginTime == null || endTime == null || endTime.isAfter(beginTime);
    }
}
