package com.easypan.entity.enums;

import lombok.Getter;

/**
 * 文件状态枚举
 */
@Getter
public enum FileStatusEnum {

    NORMAL(0, "正常"),
    RECYCLE(1, "回收站"),
    DELETED(2, "已删除");

    private final Integer code;
    private final String desc;

    FileStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据code获取枚举
    public static FileStatusEnum getByCode(Integer code) {
        for (FileStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null; // 无匹配返回null，也可抛异常
    }

    // 快捷获取"正常"状态（简化代码调用）
    public static FileStatusEnum normal() {
        return NORMAL;
    }

    // 快捷获取"回收站"状态
    public static FileStatusEnum recycle() {
        return RECYCLE;
    }

    // 快捷获取"已删除"状态
    public static FileStatusEnum deleted() {
        return DELETED;
    }
}