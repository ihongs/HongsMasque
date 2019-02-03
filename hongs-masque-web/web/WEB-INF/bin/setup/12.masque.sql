-- DB: masque

--
-- 站点
--

DROP TABLE IF EXISTS `a_masque_site`;
CREATE TABLE `a_masque_site` (
  `id` CHAR(16) NOT NULL,
  `sk` CHAR(32) NOT NULL,
  `user_id` CHAR(16) DEFAULT NULL, /* 关联的属主 */
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `room_url` VARCHAR(200) DEFAULT NULL, /* 获取 room 信息 */
  `mate_url` VARCHAR(200) DEFAULT NULL, /* 获取 mate 信息 */
  `note_url` VARCHAR(200) DEFAULT NULL, /* 自定义离线通知 */
  `ctime` UNSIGNED INTEGER(10) DEFAULT NULL,
  `mtime` UNSIGNED INTEGER(10) DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_masque_site_user` ON `a_masque_site` (`user_id`);
CREATE INDEX `IK_a_masque_site_state` ON `a_masque_site` (`state`);

--
-- 消息
--

DROP TABLE IF EXISTS `a_masque_chat`;
CREATE TABLE `a_masque_chat` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `room_id` VARCHAR(32) NOT NULL,
  `mate_id` VARCHAR(32) NOT NULL,
  `kind` VARCHAR(10) DEFAULT 'text',
  `note` TEXT DEFAULT NULL,
  `data` TEXT DEFAULT NULL,
  `ctime` BIGINT UNSIGNED DEFAULT NULL, /* 注意: 这里是毫秒 */
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_chat_site` ON `a_masque_chat` (`site_id`);
CREATE INDEX `IK_a_masque_chat_room` ON `a_masque_chat` (`room_id`);
CREATE INDEX `IK_a_masque_chat_mate` ON `a_masque_chat` (`mate_id`);
CREATE INDEX `IK_a_masque_chat_ctime` ON `a_masque_chat` (`ctime`);
CREATE INDEX `IK_a_masque_chat_state` ON `a_masque_chat` (`state`);

--
-- 计数
--

DROP TABLE IF EXISTS `a_masque_stat`;
CREATE TABLE `a_masque_stat` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `room_id` VARCHAR(32) NOT NULL,
  `mate_id` VARCHAR(32) NOT NULL,
  `fresh` UNSIGNED INTEGER(10) DEFAULT 0,
  `mtime` UNSIGNED INTEGER(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE UNIQUE INDEX `UK_a_masque_stat_mate` ON `a_masque_stat` (`site_id`,`room_id`,`mate_id`);
CREATE INDEX `IK_a_masque_stat_site` ON `a_masque_stat` (`site_id`);
CREATE INDEX `IK_a_masque_stat_room` ON `a_masque_stat` (`room_id`);
CREATE INDEX `IK_a_masque_stat_mate` ON `a_masque_stat` (`mate_id`);

--
-- 房间
--

DROP TABLE IF EXISTS `a_masque_room`;
CREATE TABLE `a_masque_room` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `room_id` VARCHAR(32) DEFAULT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `ctime` UNSIGNED INTEGER(10) DEFAULT NULL,
  `mtime` UNSIGNED INTEGER(10) DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE UNIQUE INDEX `UK_a_masque_room` ON `a_masque_room` (`site_id`,`room_id`);
CREATE INDEX `IK_a_masque_room_site` ON `a_masque_room` (`site_id`);
CREATE INDEX `IK_a_masque_room_code` ON `a_masque_room` (`room_id`);
CREATE INDEX `IK_a_masque_room_state` ON `a_masque_room` (`state`);

--
-- 人员
--

DROP TABLE IF EXISTS `a_masque_mate`;
CREATE TABLE `a_masque_mate` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `mate_id` VARCHAR(32) DEFAULT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `ctime` UNSIGNED INTEGER(10) DEFAULT NULL,
  `mtime` UNSIGNED INTEGER(10) DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE UNIQUE INDEX `UK_a_masque_mate` ON `a_masque_mate` (`site_id`,`mate_id`);
CREATE INDEX `IK_a_masque_mate_site` ON `a_masque_mate` (`site_id`);
CREATE INDEX `IK_a_masque_mate_code` ON `a_masque_mate` (`mate_id`);
CREATE INDEX `IK_a_masque_mate_state` ON `a_masque_mate` (`state`);

--
-- 管理员权限
--

INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/update');
