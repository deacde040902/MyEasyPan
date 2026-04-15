package com.easypan.entity.enums;

/**
 * 简化版VIP等级枚举（仅普通/普通VIP）
 */
public enum UserVipLevelEnum {
    NORMAL(0, "普通用户", 5368709120L),       // 5GB
    VIP(1, "VIP会员", 107374182400L);        // 100GB

    private final Integer level;
    private final String name;
    private final Long totalSpace; // 对应total_space字段

    UserVipLevelEnum(Integer level, String name, Long totalSpace) {
        this.level = level;
        this.name = name;
        this.totalSpace = totalSpace;
    }

    // 根据等级获取枚举
    public static UserVipLevelEnum getByLevel(Integer level) {
        for (UserVipLevelEnum enumItem : values()) {
            if (enumItem.getLevel().equals(level)) {
                return enumItem;
            }
        }
        return NORMAL;
    }

    // Getter
    public Integer getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public Long getTotalSpace() {
        return totalSpace;
    }
}