-- ----------------------------
-- RAG 文档切片表（权限感知文档问答模块）
-- chunk_id = file_id + "_" + chunk_index，与向量库中 Document id 一致
-- ----------------------------
DROP TABLE IF EXISTS `doc_chunk`;
CREATE TABLE `doc_chunk`  (
  `chunk_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '切片ID(主键,与向量库Document id一致)',
  `file_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属文件ID',
  `user_id` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属用户ID',
  `chunk_index` int(11) NOT NULL DEFAULT 0 COMMENT '切片序号(从0开始)',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '切片文本内容',
  `char_count` int(11) NOT NULL DEFAULT 0 COMMENT '字符数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`chunk_id`) USING BTREE,
  INDEX `idx_file_id`(`file_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'RAG文档切片表' ROW_FORMAT = Dynamic;
