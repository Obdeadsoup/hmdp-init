package com.hmdp.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.validation.constraints.AssertTrue;

import lombok.Data;
@Data
public class ShopUpdateDTO {

    @NotNull(message = "商铺ID不能为空")
    @Positive(message = "商铺ID必须为正数")
    private Long id;

    @Size(min = 1, max = 128, message = "商铺名称长度必须在1到128个字符之间")
    private String name;

    @Positive(message = "商铺类型ID必须为正数")
    private Long typeId;

    @Size(min = 1, max = 1024, message = "商铺图片长度必须在1到1024个字符之间")
    private String images;

    @Size(max = 128, message = "商圈不能超过128个字符")
    private String area;

    @Size(min = 1, max = 255, message = "商铺地址长度必须在1到255个字符之间")
    private String address;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double x;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double y;

    @PositiveOrZero(message = "商铺均价不能为负数")
    private Long avgPrice;

    @Size(max = 32, message = "营业时间字段过长")
    private String openHours;

    @AssertTrue(message = "至少提供一个可更新字段")
    public boolean hasMutableField() {
        return name != null
                || typeId != null
                || images != null
                || area != null
                || address != null
                || x != null
                || y != null
                || avgPrice != null
                || openHours != null;
    }
}
