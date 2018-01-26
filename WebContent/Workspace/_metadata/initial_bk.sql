DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `nick_name` VARCHAR(255) NULL,
  `email` TEXT NULL,
  `password` VARCHAR(255) NOT NULL,
  `enable` TINYINT NOT NULL DEFAULT 1,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username_UNIQUE` (`username` ASC))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

LOCK TABLES `account` WRITE;
INSERT INTO `account` VALUES (1, 'admin', 'admin', 'example@ezScrum.tw', '21232f297a57a5a743894a0e4a801fc3', 1, 1379910191599, 1379910191599);
UNLOCK TABLES;

DROP TABLE IF EXISTS `project_role`;
CREATE TABLE `project_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `account_id` BIGINT UNSIGNED NOT NULL,
  `role` INT NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `scrum_role`;
CREATE TABLE `scrum_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `access_productBacklog` TINYINT NOT NULL DEFAULT 1,
  `access_sprintPlan` TINYINT NOT NULL DEFAULT 1,
  `access_taskboard` TINYINT NOT NULL DEFAULT 1,
  `access_sprintBacklog` TINYINT NOT NULL DEFAULT 1,
  `access_releasePlan` TINYINT NOT NULL DEFAULT 1,
  `access_retrospective` TINYINT NOT NULL DEFAULT 1,
  `access_unplan` TINYINT NOT NULL DEFAULT 1,
  `access_report` TINYINT NOT NULL DEFAULT 1,
  `access_editProject` TINYINT NOT NULL DEFAULT 1,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `role` INT NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `project`;
CREATE TABLE `project` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(255) NOT NULL,
  `comment` TEXT NULL,
  `product_owner` VARCHAR(255), 
  `attach_max_size` BIGINT UNSIGNED NOT NULL DEFAULT 2, 
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `sprint`;
CREATE TABLE `sprint` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `serial_id` BIGINT UNSIGNED NOT NULL,
  `goal` TEXT NOT NULL,
  `interval` INT NOT NULL,
  `team_size` INT NOT NULL,
  `available_hours` INT NOT NULL,
  `focus_factor` INT NOT NULL DEFAULT 100,
  `start_date` DATETIME NOT NULL,
  `end_date` DATETIME NOT NULL,
  `demo_date` DATETIME NOT NULL,
  `demo_place` TEXT NULL,
  `daily_info` TEXT NULL,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `story`;
CREATE TABLE `story` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `serial_id` BIGINT UNSIGNED NOT NULL,
  `sprint_id` BIGINT NULL,
  `name` TEXT NOT NULL,
  `status` TINYINT UNSIGNED NOT NULL,
  `estimate` INT NOT NULL DEFAULT 0,
  `importance` INT NOT NULL DEFAULT 0,
  `value` INT NOT NULL DEFAULT 0,
  `notes` TEXT NULL,
  `how_to_demo` TEXT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `serial_id` BIGINT UNSIGNED NOT NULL,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `story_id` BIGINT NULL,
  `name` TEXT NOT NULL,
  `handler_id` BIGINT,
  `status` TINYINT UNSIGNED NOT NULL,
  `estimate` INT NOT NULL DEFAULT 0,
  `remain` INT NOT NULL DEFAULT 0,
  /*`actual` INT NOT NULL DEFAULT 0,*/
  `notes` TEXT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `history`;
CREATE TABLE `history` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `issue_id` BIGINT UNSIGNED NOT NULL,
  `issue_type` INT NOT NULL,
  `type` INT NULL,
  `old_value` TEXT NULL,
  `new_value` TEXT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `issue_partner_relation`;
CREATE TABLE `issue_partner_relation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `issue_id` BIGINT UNSIGNED NOT NULL,
  `issue_type` INT NOT NULL,
  `account_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `story_tag_relation`;
CREATE TABLE `story_tag_relation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tag_id` BIGINT UNSIGNED NOT NULL,
  `story_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `release`;
CREATE TABLE `release` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `serial_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `start_date` DATETIME NOT NULL,
  `end_date` DATETIME NOT NULL,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `retrospective`;
CREATE TABLE `retrospective` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `serial_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `type` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `sprint_id` BIGINT UNSIGNED NOT NULL,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `unplan`;
CREATE TABLE `unplan` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `serial_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `handler_id` BIGINT NOT NULL,
  `estimate` INT NOT NULL,
  /*`actual` INT NOT NULL,*/
  `notes` TEXT NOT NULL,
  `status` TINYINT UNSIGNED NOT NULL,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `sprint_id` BIGINT UNSIGNED NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `attach_file`;
CREATE TABLE `attach_file` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` TEXT NOT NULL,
  `issue_id` BIGINT UNSIGNED NOT NULL,
  `issue_type` INT NOT NULL,
  `path` TEXT NOT NULL,
  `content_type` TEXT,
  `create_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `system`;
CREATE TABLE `system` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `account_id_UNIQUE` (`account_id` ASC))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `serial_number`;
CREATE TABLE `serial_number` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `release` BIGINT UNSIGNED NOT NULL,
  `sprint` BIGINT UNSIGNED NOT NULL,
  `story` BIGINT UNSIGNED NOT NULL,
  `task` BIGINT UNSIGNED NOT NULL,
  `unplan` BIGINT UNSIGNED NOT NULL,
  `retrospective` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `token`;
CREATE TABLE `token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `account_id` BIGINT UNSIGNED NOT NULL,
  `public_token` TEXT NOT NULL,
  `private_token` TEXT NOT NULL,
  `platform_type` TEXT NOT NULL,
  `create_time` BIGINT UNSIGNED NOT NULL,
  `update_time` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = MyISAM DEFAULT CHARSET = utf8;

LOCK TABLES `system` WRITE;
INSERT INTO `system` VALUES (1, 1);
UNLOCK TABLES;