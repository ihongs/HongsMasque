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
  `note_url` VARCHAR(200) DEFAULT NULL, /* 自定义离线通知 */
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_masque_site_user` ON `a_masque_site` (`user_id`);
CREATE INDEX `IK_a_masque_site_state` ON `a_masque_site` (`state`);

--
-- 人员
--

DROP TABLE IF EXISTS `a_masque_mate`;
CREATE TABLE `a_masque_mate` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `kind` VARCHAR(10) DEFAULT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `ctime` INTEGER(10) DEFAULT NULL,
  `mtime` INTEGER(10) DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_mate_site` ON `a_masque_mate` (`site_id`);
CREATE INDEX `IK_a_masque_mate_state` ON `a_masque_mate` (`state`);

--
-- 计数
--

DROP TABLE IF EXISTS `a_masque_clue`;
CREATE TABLE `a_masque_clue` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `mate_id` CHAR(36) NOT NULL,
  `room_id` CHAR(36) NOT NULL,
  `mtime`  BIGINT DEFAULT NULL, /* 注意: 这里是毫秒 */
  `state` TINYINT DEFAULT '1',
  `unread` INTEGER DEFAULT '0',
  `unsend` INTEGER DEFAULT '0',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE UNIQUE INDEX `UK_a_masque_clue_mate` ON `a_masque_stat` (`site_id`,`mate_id`,`room_id`);
CREATE INDEX `IK_a_masque_clue_site` ON `a_masque_clue` (`site_id`);
CREATE INDEX `IK_a_masque_clue_mate` ON `a_masque_clue` (`mate_id`);
CREATE INDEX `IK_a_masque_clue_room` ON `a_masque_clue` (`room_id`);

--
-- 消息
--

DROP TABLE IF EXISTS `a_masque_chat`;
CREATE TABLE `a_masque_chat` (
  `id` CHAR(16) NOT NULL,
  `site_id` CHAR(16) NOT NULL,
  `mate_id` CHAR(36) NOT NULL,
  `room_id` CHAR(36) NOT NULL,
  `kind` VARCHAR(10) DEFAULT 'text',
  `note` TEXT DEFAULT NULL,
  `data` TEXT DEFAULT NULL,
  `ctime`  BIGINT DEFAULT NULL, /* 注意: 这里是毫秒 */
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_chat_site` ON `a_masque_chat` (`site_id`);
CREATE INDEX `IK_a_masque_chat_mate` ON `a_masque_chat` (`mate_id`);
CREATE INDEX `IK_a_masque_chat_room` ON `a_masque_chat` (`room_id`);
CREATE INDEX `IK_a_masque_chat_ctime` ON `a_masque_chat` (`ctime`);
CREATE INDEX `IK_a_masque_chat_state` ON `a_masque_chat` (`state`);

--
-- 管理员权限
--

INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/site/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/site/create');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/site/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/site/delete');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/mate/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/mate/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/room/search');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/room/update');
INSERT INTO `a_master_user_role` VALUES ('1','centra/masque/token/create');
