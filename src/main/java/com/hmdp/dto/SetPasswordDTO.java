package com.hmdp.dto;

import lombok.Data;

@Data
public class SetPasswordDTO {
    
    /**
     * 新密码
     */
    private String password;
    /**
     * 确认密码
     */
    private String confirmPassword;
}
