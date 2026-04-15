/*
 Navicat Premium Data Transfer

 Source Server         : SQL-connect
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44)
 Source Host           : localhost:3306
 Source Schema         : easypan

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44)
 File Encoding         : 65001

 Date: 27/03/2026 08:41:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_chunk
-- ----------------------------
DROP TABLE IF EXISTS `file_chunk`;
CREATE TABLE `file_chunk`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `upload_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '上传任务ID(唯一标识一次上传)',
  `user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '上传用户ID',
  `file_md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件MD5(用于秒传)',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `total_size` bigint(20) NOT NULL COMMENT '文件总大小',
  `chunk_size` int(11) NOT NULL COMMENT '分片大小',
  `total_chunks` int(11) NOT NULL COMMENT '总分片数',
  `uploaded_chunks` int(11) NOT NULL DEFAULT 0 COMMENT '已上传分片数',
  `file_pid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目标父目录ID',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态(0-上传中,1-已完成,2-已取消)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `expire_time` datetime NOT NULL COMMENT '过期时间(超时自动清理)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_upload_id`(`upload_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_file_md5`(`file_md5`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件分片上传记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_chunk
-- ----------------------------
INSERT INTO `file_chunk` VALUES (1, '1192b6958a114fd592687f52dad04a55', 'e392b7617eaf42c', '8d0dab7188bf2bceb23442b87bc6263c', 'test.jpg', 1024000, 104857600, 1, 1, '0', 0, '2026-03-25 22:08:27', '2026-03-25 22:12:29', '2026-03-26 22:08:27');

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件ID(主键)',
  `user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属用户ID',
  `file_pid` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '父文件ID(0表示根目录)',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件后缀',
  `file_size` bigint(20) NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `is_folder` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否是目录(0-文件,1-目录)',
  `file_md5` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件MD5(用于秒传)',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件存储路径',
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面图路径(图片/视频)',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态(0-正常,1-回收站,2-已删除)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `delete_time` datetime NULL DEFAULT NULL COMMENT '删除时间(回收站)',
  `last_op_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后操作时间',
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_file_pid`(`file_pid`) USING BTREE,
  INDEX `idx_user_pid`(`user_id`, `file_pid`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_info
-- ----------------------------
INSERT INTO `file_info` VALUES ('8268ae87b4c24e1', 'e392b7617eaf42c', '0', '改名-测试文件夹.txt', NULL, 0, 1, NULL, NULL, NULL, 0, '2026-03-25 21:56:51', '2026-03-25 21:56:50', NULL, '2026-03-25 22:17:28');
INSERT INTO `file_info` VALUES ('d4687124f7eb4fd', 'e392b7617eaf42c', '0', 'test.jpg', '.jpg', 1024000, 0, '8d0dab7188bf2bceb23442b87bc6263c', 'chunks/files/8d0dab7188bf2bceb23442b87bc6263c.jpg', NULL, 0, '2026-03-25 22:12:29', '2026-03-25 22:26:31', '2026-03-25 22:22:19', '2026-03-25 22:22:41');

-- ----------------------------
-- Table structure for file_share
-- ----------------------------
DROP TABLE IF EXISTS `file_share`;
CREATE TABLE `file_share`  (
  `share_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享ID(主键)',
  `user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享用户ID',
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享文件ID',
  `share_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提取码',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间(NULL表示永久)',
  `view_count` int(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `download_count` int(11) NOT NULL DEFAULT 0 COMMENT '下载次数',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态(0-正常,1-已取消)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`share_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_file_id`(`file_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件分享表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_share
-- ----------------------------
INSERT INTO `file_share` VALUES ('9e40f50a4d1643138479133875301fc0', 'e392b7617eaf42c', 'd4687124f7eb4fd', '989842', '2026-04-02 22:43:43', 0, 0, 0, '2026-03-26 22:43:43', '2026-03-26 22:43:43');
INSERT INTO `file_share` VALUES ('d565bb17090346ca95ae53fc548d0f7a', 'e392b7617eaf42c', 'd4687124f7eb4fd', '293004', '2026-04-02 21:24:09', 0, 0, 1, '2026-03-26 21:24:09', '2026-03-26 22:43:32');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键（如siteName、maxFileSize）',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置值',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key`) USING BTREE COMMENT '配置键唯一'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('1', 'siteName', 'EasyPan', '网站名称', '2026-03-24 17:18:37', '2026-03-24 17:18:37');
INSERT INTO `sys_config` VALUES ('2', 'maxFileSize', '107374182400', '普通用户单文件最大大小（字节，100MB）', '2026-03-24 17:18:37', '2026-03-24 17:18:37');
INSERT INTO `sys_config` VALUES ('3', 'defaultUserSpace', '5368709120', '新用户默认空间（字节，5GB）', '2026-03-24 17:18:37', '2026-03-24 17:18:37');
INSERT INTO `sys_config` VALUES ('4', 'vipUserSpace', '107374182400', 'VIP用户空间（字节，100GB）', '2026-03-24 17:18:37', '2026-03-24 17:18:37');
INSERT INTO `sys_config` VALUES ('5', 'vipPrice', '9.9', 'VIP月费（元）', '2026-03-24 17:18:37', '2026-03-24 17:18:37');

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `user_id` varchar(15) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户id',
  `nick_name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `email` varchar(150) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '邮箱 ',
  `avatar_path` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像存储路径',
  `qq_open_id` varchar(35) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'qqOpenId',
  `qq_avatar` varchar(150) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'qq头像',
  `password` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `login_time` datetime NULL DEFAULT NULL COMMENT '登录时间',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint(1) NOT NULL COMMENT '0:禁用 1：启用',
  `use_space` bigint(20) NULL DEFAULT NULL COMMENT '使用空间单位 byte类型',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户创建时间',
  `total_space` bigint(20) NULL DEFAULT NULL COMMENT '总空间',
  `vip_level` tinyint(4) NULL DEFAULT 0 COMMENT 'VIP等级 0-普通用户 1-VIP会员',
  `vip_expire_time` datetime NULL DEFAULT NULL COMMENT 'VIP到期时间',
  `is_vip` tinyint(4) NULL DEFAULT 0 COMMENT '是否为有效VIP 0-否 1-是',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `key_email`(`email`) USING BTREE,
  UNIQUE INDEX `key_qq_open_id`(`qq_open_id`) USING BTREE,
  UNIQUE INDEX `key_nick_name`(`nick_name`) USING BTREE,
  INDEX `idx_user_info_vip`(`vip_level`, `is_vip`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_info
-- ----------------------------
INSERT INTO `user_info` VALUES ('e392b7617eaf42c', 'lc', '2822867926@qq.com', 'avatar/e392b7617eaf42c_1774016201955.png', NULL, NULL, '39e133076ee76860066d974e4031f4b0', '2026-03-27 08:37:46', '2026-03-26 21:23:53', 1, 246759, '2026-03-20 16:52:55', 5368709120, 0, NULL, 0);

-- ----------------------------
-- Table structure for vip_order
-- ----------------------------
DROP TABLE IF EXISTS `vip_order`;
CREATE TABLE `vip_order`  (
  `order_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '订单ID(UUID)',
  `user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户ID',
  `product_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '网盘VIP月度会员' COMMENT '商品名称',
  `amount` decimal(10, 2) NOT NULL DEFAULT 20.00 COMMENT '支付金额',
  `pay_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0未支付 1已支付',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `months` int(11) NULL DEFAULT NULL COMMENT '购买月数',
  PRIMARY KEY (`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vip_order
-- ----------------------------
INSERT INTO `vip_order` VALUES ('08bc19bc7616499c9b08e8d8c9b30115', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 22:16:01', '2026-03-22 22:16:01', NULL);
INSERT INTO `vip_order` VALUES ('0b45726591654cdd9c5d736d8799c295', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 17:11:01', '2026-03-23 17:11:01', NULL);
INSERT INTO `vip_order` VALUES ('208f9f21b9464d3cbcb49e3e6a7fdad5', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:22:17', '2026-03-25 21:22:17', 1);
INSERT INTO `vip_order` VALUES ('24e39b8220e04b8b97bcea096b75b19c', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 18:14:25', '2026-03-23 18:14:25', NULL);
INSERT INTO `vip_order` VALUES ('32e743cd1b2b44109d51cd9c57cd010a', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 16:23:07', '2026-03-22 16:23:07', NULL);
INSERT INTO `vip_order` VALUES ('4143507ab0bb43d9b47cdd3dd7afc436', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 16:51:41', '2026-03-23 16:51:41', NULL);
INSERT INTO `vip_order` VALUES ('54c704f812044f97a6fb0607ae16448a', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 16:30:27', '2026-03-23 16:30:27', NULL);
INSERT INTO `vip_order` VALUES ('5904685337944bf28dd1466931b48016', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 20:52:17', '2026-03-25 20:52:16', 1);
INSERT INTO `vip_order` VALUES ('59c2fe67d85341fbbc6f2e3b24ec2700', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 17:08:28', '2026-03-23 17:08:28', NULL);
INSERT INTO `vip_order` VALUES ('6cc27c5b2e2c45dc8eec342a47059aed', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:55:04', '2026-03-25 21:55:04', 1);
INSERT INTO `vip_order` VALUES ('718b6ab4bfa64458b48cb436352d1337', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 22:45:45', '2026-03-22 22:45:45', NULL);
INSERT INTO `vip_order` VALUES ('7b09ef38018347dfb3efc5d565d9d7d1', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 17:52:57', '2026-03-23 17:52:57', NULL);
INSERT INTO `vip_order` VALUES ('810d256855c343cbb4c82af6d4d0f19c', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 17:34:20', '2026-03-22 17:34:20', NULL);
INSERT INTO `vip_order` VALUES ('8cdbba18a2e2492d9dcfa8b7ea548191', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 22:53:04', '2026-03-22 22:53:04', NULL);
INSERT INTO `vip_order` VALUES ('8db0cfedffdd4e088d693bfa315c4467', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 17:35:42', '2026-03-23 17:35:42', NULL);
INSERT INTO `vip_order` VALUES ('931fcfb94516459fa5a3e8dc611a3ee8', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 16:05:52', '2026-03-22 16:05:52', NULL);
INSERT INTO `vip_order` VALUES ('97e08f23d5aa4adc870da191b2bb27a3', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 17:45:42', '2026-03-23 17:45:42', NULL);
INSERT INTO `vip_order` VALUES ('9d945d10c2cc4a5cae4d1f6934fe5200', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 17:38:30', '2026-03-23 17:38:30', NULL);
INSERT INTO `vip_order` VALUES ('a7ac6ef652624711820277f3b9fcfce9', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:47:54', '2026-03-25 21:47:54', 1);
INSERT INTO `vip_order` VALUES ('aef6468601f844dcabd18d54fad5afc1', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-27 08:38:08', '2026-03-27 08:38:07', 1);
INSERT INTO `vip_order` VALUES ('bc301554f7654e90b7c5e613dbeabcdb', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-22 16:49:06', '2026-03-22 16:49:06', NULL);
INSERT INTO `vip_order` VALUES ('c79811e436554200bfbcd71bdad87fae', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:28:53', '2026-03-25 21:28:53', 1);
INSERT INTO `vip_order` VALUES ('dbeb605b553d476bad836e790562f90c', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 16:54:43', '2026-03-23 16:54:43', NULL);
INSERT INTO `vip_order` VALUES ('dcced43e0ec04cb7a1907cfe37802475', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:38:21', '2026-03-25 21:38:21', 1);
INSERT INTO `vip_order` VALUES ('e4a6768eeab247709bad09d9ee4f3a29', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 18:04:45', '2026-03-23 18:04:45', NULL);
INSERT INTO `vip_order` VALUES ('eb7f351243134cd0b7accebd8da5f8d0', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:27:44', '2026-03-25 21:27:44', 1);
INSERT INTO `vip_order` VALUES ('ed8be82e4c4a48cc816e74d6566601a5', 'e392b7617eaf42c', '月度VIP', 20.00, 0, NULL, '2026-03-23 16:40:58', '2026-03-23 16:40:58', NULL);
INSERT INTO `vip_order` VALUES ('f1827660d37f465681d43cdbc8512c2e', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:00:27', '2026-03-25 21:00:27', 1);
INSERT INTO `vip_order` VALUES ('f2b9654ee1ce43c09d944ecabc5e5f2c', 'e392b7617eaf42c', '1个月VIP', 20.00, 0, NULL, '2026-03-25 21:41:07', '2026-03-25 21:41:06', 1);

SET FOREIGN_KEY_CHECKS = 1;
