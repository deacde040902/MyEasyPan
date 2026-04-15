package com.easypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.easypan.entity.enums.UserVipLevelEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息实体类（完全匹配数据库表结构 + 保留头像/创建时间功能）
 */
@Data
@TableName("user_info")
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    // ========== 数据库表中存在的字段（完全匹配，无修改） ==========
    /**
     * 用户ID（主键，非自增，UUID生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像路径（数据库字段avatar_path，头像功能核心）
     */
    private String avatarPath; // MP自动驼峰映射：avatarPath → avatar_path

    /**
     * QQ开放ID
     */
    private String qqOpenId;

    /**
     * QQ头像
     */
    private String qqAvatar;

    /**
     * 密码
     */
    private String password;

    /**
     * VIP等级（0-普通，1-VIP）
     */
    private Integer vipLevel = 0; // 默认普通用户

    /**
     * VIP过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date vipExpireTime;

    /**
     * 是否VIP（0-否，1-是）
     * 数据库字段is_vip，用注解强制映射（避免驼峰错误）
     */
    @TableField(value = "is_vip")
    private Integer vip = 0; // 默认普通用户

    /**
     * 总空间（字节）
     */
    private Long totalSpace = UserVipLevelEnum.NORMAL.getTotalSpace(); // 默认5GB

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status = 1; // 默认启用

    /**
     * 已使用空间（字节）
     */
    private Long useSpace = 0L; // 默认0

    /**
     * 创建时间（数据库字段create_time，基础字段）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // ========== 空值处理 & 业务方法（适配表结构） ==========
    /**
     * 空值处理：已用空间默认0，避免NPE
     */
    public Long getUseSpace() {
        return useSpace == null ? 0 : useSpace;
    }

    /**
     * 判断是否为有效VIP
     * 规则：is_vip=1 + vipLevel=1 + vipExpireTime未过期
     */
    public boolean isEffectiveVip() {
        return vip != null && vip == 1
                && vipLevel != null && vipLevel.equals(UserVipLevelEnum.VIP.getLevel())
                && vipExpireTime != null
                && vipExpireTime.after(new Date());
    }

    /**
     * 同步VIP空间（普通→VIP：5GB→100GB；VIP→普通：100GB→5GB）
     */
    public void syncVipSpace() {
        this.totalSpace = isEffectiveVip()
                ? UserVipLevelEnum.VIP.getTotalSpace()
                : UserVipLevelEnum.NORMAL.getTotalSpace();
    }

    /**
     * 获取VIP等级名称（普通用户/VIP会员）
     */
    public String getVipLevelName() {
        return UserVipLevelEnum.getByLevel(vipLevel).getName();
    }

    // ========== 重写toString（完整输出所有字段，含头像/创建时间） ==========
    @Override
    public String toString() {
        return "UserInfo{" +
                "userId='" + userId + '\'' +
                ", nickName='" + nickName + '\'' +
                ", email='" + email + '\'' +
                ", avatarPath='" + avatarPath + '\'' + // 保留头像路径输出
                ", qqOpenId='" + qqOpenId + '\'' +
                ", qqAvatar='" + qqAvatar + '\'' +
                ", password='" + password + '\'' +
                ", vipLevel=" + vipLevel +
                ", vipExpireTime=" + vipExpireTime +
                ", isVip=" + vip +
                ", totalSpace=" + totalSpace +
                ", loginTime=" + loginTime +
                ", lastLoginTime=" + lastLoginTime +
                ", status=" + status +
                ", useSpace=" + useSpace +
                ", createTime=" + createTime + // 保留创建时间输出
                '}';
    }
}