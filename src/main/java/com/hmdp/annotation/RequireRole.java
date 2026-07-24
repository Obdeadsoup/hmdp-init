package com.hmdp.annotation;

import java.lang.annotation.*;

import com.hmdp.utils.UserRoles;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String value() default UserRoles.USER;
}
