-- DB: masque

--
-- 消息
--

DROP TABLE IF EXISTS `a_masque_chat`;
CREATE TABLE `a_masque_chat` (
  `id` CHAR(16) NOT NULL,
  `mate_id` CHAR(36) NOT NULL,
  `meet_id` CHAR(36) NOT NULL,
  `ctime` BIGINT DEFAULT NULL, /* 注意: 这是毫秒 */
  `note` TEXT DEFAULT NULL,
  `data` TEXT DEFAULT NULL,
  `kind` CHAR(10) DEFAULT 'text',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_masque_chat_mate` ON `a_masque_chat` (`mate_id`);
CREATE INDEX `IK_a_masque_chat_meet` ON `a_masque_chat` (`meet_id`);
CREATE INDEX `IK_a_masque_chat_ctime` ON `a_masque_chat` (`ctime`);

--
-- 计数
--

DROP TABLE IF EXISTS `a_masque_clue`;
CREATE TABLE `a_masque_clue` (
  `id` CHAR(16) NOT NULL,
  `mate_id` CHAR(36) NOT NULL,
  `meet_id` CHAR(36) NOT NULL,
  `mtime` BIGINT DEFAULT NULL, /* 注意: 这是毫秒 */
  `unread` INTEGER DEFAULT '0',
  `unsend` INTEGER DEFAULT '0',
  PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX `UK_a_masque_clue` ON `a_masque_clue` (`mate_id`,`meet_id`);
CREATE INDEX `IK_a_masque_clue_mate` ON `a_masque_clue` (`mate_id`);
CREATE INDEX `IK_a_masque_clue_meet` ON `a_masque_clue` (`meet_id`);
CREATE INDEX `IK_a_masque_chue_mtime` ON `a_masque_clue` (`mtime`);
CREATE INDEX `IK_a_masque_chue_state` ON `a_masque_clue` (`state`);
