package com.hmdp;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.utils.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootTest
public class JMeterUserDataTest {

    @Resource
    private IUserService userService;

    @Test
    public void createUsers() throws Exception {
        int count = 1000;
        List<User> users = new ArrayList<>();
        List<String> csvLines = new ArrayList<>();
        List<String> phones = new ArrayList<>();

        csvLines.add("phone,password");

        for (int i = 1; i <= count; i++) {
            String phone =
                    // 项目手机号正则不接受 190 号段；198 是当前正则允许的测试号段。
                    String.valueOf(19800000000L + i);

            phones.add(phone);

            User user = new User();
            user.setPhone(phone);
            user.setNickName("jmeter_" + i);
            user.setPassword(
                    PasswordEncoder.encode("123456")
            );

            users.add(user);
            csvLines.add(phone + ",123456");
        }

        // 脚本可能因 CSV 写入失败而被再次执行；手机号有唯一索引，
        // 因此先找出已存在的用户，仅批量插入缺失部分，保证可重复运行。
        Set<String> existingPhones = new HashSet<>();
        userService.lambdaQuery()
                .in(User::getPhone, phones)
                .list()
                .forEach(user -> existingPhones.add(user.getPhone()));
        users.removeIf(user -> existingPhones.contains(user.getPhone()));

        if (!users.isEmpty()) {
            userService.saveBatch(users, 200);
        }

        // Files.write() 的目标必须是文件路径，而不能是 data 目录本身。
        Path output = Paths.get(
                "D:\\AppGallery\\JMeter\\apache-jmeter-5.6.3\\data",
                "users.csv"
        );

        Files.createDirectories(output.getParent());

        Files.write(
                output,
                csvLines,
                StandardCharsets.UTF_8
        );
    }
}
