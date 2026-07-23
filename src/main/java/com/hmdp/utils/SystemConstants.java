package com.hmdp.utils;

public class SystemConstants {
    /**
     * 宿主机开发时沿用原图片目录；Compose 环境通过 HMDP_UPLOAD_DIR
     * 指向与 Nginx 共享的 /data/hmdp/imgs。
     */
    public static final String IMAGE_UPLOAD_DIR = System.getenv().getOrDefault(
            "HMDP_UPLOAD_DIR",
            "D:\\lesson\\nginx-1.18.0\\html\\hmdp\\imgs\\"
    );
    public static final String USER_NICK_NAME_PREFIX = "hmdp_user_";
    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final int MAX_PAGE_SIZE = 10;
}
