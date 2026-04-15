package com.easypan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easypan.entity.dto.*;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.VipStatusVO;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 用户信息 业务接口
 */
public interface UserInfoService extends IService<UserInfo> {

    /**
     * 核心：根据邮箱查询用户（保留原有方法）
     */
    UserInfo getUserInfoByEmail(String email);

    /**
     * 核心：新增用户（保留原有方法，可被 MP 的 save 替代）
     */
    Integer add(UserInfo bean);

    /**
     * 发送邮箱验证码（保留原有方法）
     */
    void sendEmailCode(SendEmailCodeDTO dto);

    /**
     * 用户注册（保留原有方法）
     */
    void register(RegisterDTO dto);

    /**
     * 用户登录（保留原有方法）
     */
    Map<String, String> login(LoginDTO dto);

    /**
     * 重置密码（保留原有方法）
     */
    void resetPwd(ResetPwdDTO dto);

    Integer updateUserInfoByUserId(UserInfo bean, String userId);

    /**
     * 获取用户头像
     * @param userId 用户ID
     * @param response HTTP响应
     */
    void getAvatar(String userId, HttpServletResponse response);

    /**
     * 上传/更新用户头像
     * @param dto 上传参数
     * @return 头像存储的相对路径
     */
    String uploadAvatar(AvatarUploadDTO dto);

    /**
     * 根据用户ID查询用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfo getUserInfoByUserId(String userId);

    /**
     * 获取用户空间信息
     * @param token 登录Token
     * @return 包含基础信息和空间信息的Map
     */
    Map<String, Object> getUseSpace(String token);

    void changePwd(String token, ChangePwdDTO dto);

    UserInfo getUserInfoByQQOpenId(String qqOpenId);

    void openVip(String userId, Integer months);

    void expireVip(String userId);

    /**
     * 管理员分页获取用户列表
     */
    IPage<UserInfo> getUserListPage(Page<UserInfo> page, FileListQueryDTO queryDTO);

    /**
     * 管理员修改用户状态
     */
    void changeUserStatus(String userId, Integer status);

    /**
     * 管理员修改用户空间
     */
    void changeUserSpace(String userId, Long totalSpace);

    /**
     * 获取用户VIP状态VO
     */
    VipStatusVO getVipStatusVO(String userId);

    public long countByVipLevel(Integer vipLevel);


}