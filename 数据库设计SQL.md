# 佘记日记项目 - 数据库建表 SQL

> 数据库类型：MySQL 8.0
> 字符集：utf8mb4 / utf8mb4_unicode_ci
> 存储引擎：InnoDB
> 使用方式：直接复制全文到 MySQL 客户端（如 Navicat、MySQL Workbench、命令行）执行即可。

---

## 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS `sheji_diary`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `sheji_diary`;
```

---

## 2. 删除旧表（重建前清理，注意依赖顺序）

```sql
DROP TABLE IF EXISTS `diary_media`;
DROP TABLE IF EXISTS `diary`;
DROP TABLE IF EXISTS `user`;
```

---

## 3. 数据表结构

### 3.1 用户表 `user`

```sql
CREATE TABLE `user` (
  `user_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '账号，唯一',
  `password`    VARCHAR(100) NOT NULL COMMENT '加密密码（建议 BCrypt）',
  `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像文件路径',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';
```

### 3.2 日记表 `diary`

```sql
CREATE TABLE `diary` (
  `diary_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '所属用户id，外键关联user',
  `title`       VARCHAR(100) NOT NULL COMMENT '日记标题',
  `content`     TEXT         COMMENT '日记富文本内容（HTML）',
  `permission`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0私有；1公开',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '日记创建时间',
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '日记修改时间',
  PRIMARY KEY (`diary_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_permission` (`permission`),
  CONSTRAINT `fk_diary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '日记表';
```

### 3.3 日记媒体文件表 `diary_media`

```sql
CREATE TABLE `diary_media` (
  `media_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `diary_id`    BIGINT       NOT NULL COMMENT '关联日记id',
  `media_type`  TINYINT      NOT NULL COMMENT '1图片；2视频',
  `file_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path`   VARCHAR(500) NOT NULL COMMENT '服务器存储路径/OSS地址',
  `file_size`   BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`media_id`),
  KEY `idx_diary_id` (`diary_id`),
  CONSTRAINT `fk_media_diary` FOREIGN KEY (`diary_id`) REFERENCES `diary` (`diary_id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '日记媒体文件表';
```

---

## 4. 表关系说明

```
user (1) ────< (N) diary (1) ────< (N) diary_media
   用户             日记                  日记媒体文件
```

- `diary.user_id` → `user.user_id`（一对多：一个用户多篇日记）
- `diary_media.diary_id` → `diary.diary_id`（一对多：一篇日记多个媒体资源，删除日记级联清理媒体）
- `user` 为 MySQL 保留字，建表/查询时需使用反引号包裹

---

## 5. 示例数据（可选，便于联调测试）

```sql
-- 插入测试用户（密码为 123456 的 BCrypt 密文，可直接登录）
INSERT INTO `user` (`username`, `password`, `nickname`, `avatar`)
VALUES ('test01', '$2a$10$ngxcVbQUuWXJhky2uhKExutPnidD.TLsY90YPjwPRVi.kSgyO8WWK', '测试用户', '');

-- 插入两篇日记：一篇公开、一篇私有
INSERT INTO `diary` (`user_id`, `title`, `content`, `permission`)
VALUES
  (1, '我的第一篇公开日记', '<p>今天天气真好！</p>', 1),
  (1, '私人心情记录', '<p>这是只有我能看的内容。</p>', 0);

-- 插入对应媒体资源
INSERT INTO `diary_media` (`diary_id`, `media_type`, `file_name`, `file_path`, `file_size`)
VALUES
  (1, 1, 'sunset.jpg', '/uploads/2026/01/sunset.jpg', 204800),
  (1, 2, 'video.mp4', '/uploads/2026/01/video.mp4', 5242880);
```

---

## 6. 常用查询示例

```sql
-- 公开日记广场：按时间倒序分页（首页）
SELECT * FROM diary
WHERE permission = 1
ORDER BY create_time DESC
LIMIT 0, 12;

-- 我的日记列表（个人中心）
SELECT * FROM diary
WHERE user_id = 1
ORDER BY create_time DESC;

-- 日记详情 + 作者信息（JOIN 用户表）
SELECT d.*, u.nickname, u.avatar
FROM diary d
LEFT JOIN user u ON d.user_id = u.user_id
WHERE d.diary_id = 1;

-- 某篇日记的全部媒体资源
SELECT * FROM diary_media
WHERE diary_id = 1
ORDER BY create_time ASC;

-- 统计某用户公开日记数量
SELECT COUNT(*) FROM diary
WHERE user_id = 1 AND permission = 1;

-- 下载权限校验：判断媒体归属日记是否公开或属于本人
SELECT dm.*, d.permission, d.user_id
FROM diary_media dm
JOIN diary d ON dm.diary_id = d.diary_id
WHERE dm.media_id = 1;
```

---

## 7. 字段枚举说明

| 表 | 字段 | 取值 | 含义 |
|----|------|------|------|
| diary | permission | 0 | 私有（仅本人可见） |
| diary | permission | 1 | 公开（所有人可见） |
| diary_media | media_type | 1 | 图片 |
| diary_media | media_type | 2 | 视频 |

---

## 8. 注意事项

1. **保留字**：`user` 是 MySQL 保留字，所有涉及该表的 SQL 必须用反引号包裹表名。
2. **级联删除**：`diary_media` 设置了 `ON DELETE CASCADE`，删除日记会自动清理媒体记录；如需保留介质文件请先迁移。
3. **密码安全**：`password` 字段务必存储加密后的密文（推荐 BCrypt），切勿明文保存。
4. **字符集**：全库使用 `utf8mb4`，可正确存储 emoji 等特殊字符。
5. **索引**：已针对 `user_id`、`permission`、`diary_id` 建立查询索引，高频字段可按需补充复合索引。
