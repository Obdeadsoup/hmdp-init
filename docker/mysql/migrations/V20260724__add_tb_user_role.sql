-- SEC-01：面向已存在 MySQL 数据卷的幂等角色迁移。
-- docker/mysql/init/hmdp.sql 只在空数据卷首次启动时执行；已有环境需在发布应用前手动执行本文件。

ALTER TABLE `tb_user`
    ADD COLUMN IF NOT EXISTS `role` varchar(16) NOT NULL DEFAULT 'USER'
    COMMENT '用户角色: USER普通用户, ADMIN管理员' AFTER `icon`;

UPDATE `tb_user`
SET `role` = 'USER'
WHERE `role` IS NULL OR `role` = '';

ALTER TABLE `tb_user`
    MODIFY COLUMN `role` varchar(16) NOT NULL DEFAULT 'USER'
    COMMENT '用户角色: USER普通用户, ADMIN管理员';
