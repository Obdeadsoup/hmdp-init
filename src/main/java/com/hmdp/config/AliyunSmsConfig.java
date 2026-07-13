/*
package com.hmdp.config;

import com.aliyun.dysmsapi20170525.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunSmsConfig {

    @Bean
    public Client aliyunSmsClient(AliyunSmsProperties properties)
            throws Exception {

        String accessKeyId =
                System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");

        String accessKeySecret =
                System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");

        if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "缺少环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID"
            );
        }

        if (accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "缺少环境变量 ALIBABA_CLOUD_ACCESS_KEY_SECRET"
            );
        }

        com.aliyun.teaopenapi.models.Config config =
                new com.aliyun.teaopenapi.models.Config()
                        .setAccessKeyId(accessKeyId)
                        .setAccessKeySecret(accessKeySecret);

        config.endpoint = properties.getEndpoint();

        return new Client(config);
    }
}
*/