package com.hmdp.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScrollResult {
    /**
     * 用于存放当前页数据
     */
    private List<?> list;
    /**
     * 当前页的最小时间戳
     */
    private Long minTime;
    /**
     * 下一页在最小时间戳上需要跳过的数据数量
     */
    private Integer offset;
}
