package com.easypan.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.entity.config.AdminConfig;
import com.easypan.entity.dto.*;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.enums.UserVipLevelEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.VipStatusVO;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.redis.RedisComponent;
import com.easypan.redis.RedisUtils;
import com.easypan.service.CheckCodeService;
import com.easypan.service.EmailService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.MD5Util;
import com.easypan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户信息 业务实现（修正版：匹配UserInfo实体类 + 修复VIP/空间逻辑）
 */
@Service("userInfoService")
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    private static final Logger log = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Resource
    private UserInfoMapper userInfoMapper;
    @Resource
    private EmailService emailService;
    @Resource
    private CheckCodeService checkCodeService;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private AdminConfig adminConfig;

    private String avatarBasePath = "E:/Workspace-java/easypan/esaypan-backend/avatar";

    /**
     * 根据邮箱查询用户
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.lambdaQuery()
                .eq(UserInfo::getEmail, email)
                .one();
    }

    /**
     * 新增用户
     */
    @Override
    public Integer add(UserInfo bean) {
        this.save(bean);
        return 1;
    }

    /**
     * 发送邮箱验证码
     */
    @Override
    public void sendEmailCode(SendEmailCodeDTO dto) {
        boolean checkCodeValid = checkCodeService.verifyCheckCode(dto.getCheckCodeKey(), dto.getCheckCode());
        if (!checkCodeValid) {
            throw new BusinessException("图片验证码错误或已过期");
        }
        emailService.sendEmailCode(dto.getEmail(), dto.getType());
    }

    /**
     * 用户注册
     */
    @Override
    public void register(RegisterDTO dto) {
        // 1. 校验图片验证码
        boolean checkCodeValid = checkCodeService.verifyCheckCode(dto.getCheckCodeKey(), dto.getCheckCode());
        if (!checkCodeValid) {
            throw new BusinessException("图片验证码错误或已过期");
        }

        // 2. 校验邮箱是否已注册
        if (this.lambdaQuery().eq(UserInfo::getEmail, dto.getEmail()).count() > 0) {
            throw new BusinessException("该邮箱已注册，请直接登录");
        }

        // 3. 校验邮箱验证码
        boolean emailCodeValid = emailService.verifyEmailCode(dto.getEmail(), "0", dto.getEmailCode());
        if (!emailCodeValid) {
            throw new BusinessException("邮箱验证码错误或已过期");
        }

        // 4. 构建用户信息（修正：同步VIP默认空间）
        UserInfo user = new UserInfo();
        user.setUserId(UUID.randomUUID().toString().replace("-", "").substring(0, 15));
        user.setEmail(dto.getEmail());
        user.setNickName(dto.getNickName());
        user.setPassword(MD5Util.encrypt(dto.getPassword()));
        user.setStatus(Constants.ONE);
        user.setCreateTime(new java.util.Date());
        user.setTotalSpace(UserVipLevelEnum.NORMAL.getTotalSpace()); // 修正：用枚举默认空间
        user.setUseSpace(0L); // 初始化已用空间

        // 5. 保存用户
        this.save(user);
    }

    /**
     * 用户登录（修正：同步VIP状态到Redis）
     */
    @Override
    public Map<String, String> login(LoginDTO dto) {
        // 1. 校验图片验证码
        boolean checkCodeValid = checkCodeService.verifyCheckCode(dto.getCheckCodeKey(), dto.getCheckCode());
        if (!checkCodeValid) {
            throw new BusinessException("图片验证码错误或已过期");
        }

        // 2. 查询用户
        UserInfo user = this.getUserInfoByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException("邮箱或密码错误");
        }

        // 3. 校验密码
        if (!MD5Util.encrypt(dto.getPassword()).equals(user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
        }

        // 4. 校验用户状态
        boolean isAdmin = adminConfig.getAdminEmailList().contains(user.getEmail());
        if (!isAdmin && user.getStatus().equals(Constants.ZERO)) {
            throw new BusinessException("账号已禁用，请联系管理员");
        }

        // 5. 生成Token（先清理旧Token）
        redisComponent.cleanUserTokenByUserId(user.getUserId());
        String token = UUID.randomUUID().toString().replace("-", "");

        // 6. 更新登录时间
        this.lambdaUpdate()
                .eq(UserInfo::getUserId, user.getUserId())
                .set(UserInfo::getLoginTime, new java.util.Date())
                .set(UserInfo::getLastLoginTime, user.getLoginTime())
                .update();

        // 7. 封装Token并保存到Redis（修正：同步最新VIP状态）
        TokenUserInfoDto tokenDto = new TokenUserInfoDto();
        tokenDto.setToken(token);
        tokenDto.setUserId(user.getUserId());
        tokenDto.setNickName(user.getNickName());
        tokenDto.setAdmin(isAdmin);
        tokenDto.setEmail(user.getEmail());
        tokenDto.setAvatarPath(user.getAvatarPath());
        tokenDto.setVip(user.isEffectiveVip());
        tokenDto.setVipLevel(user.getVipLevel());
        tokenDto.setVipLevelName(user.getVipLevelName());
        tokenDto.setTotalSpace(user.getTotalSpace());
        redisComponent.saveTokenUserInfoDto(tokenDto);

        // 8. 返回结果
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("nickName", user.getNickName());
        result.put("userId", user.getUserId());
        result.put("isVip", user.isEffectiveVip() ? "1" : "0");
        result.put("isAdmin", isAdmin ? "1" : "0");
        return result;
    }

    /**
     * 重置密码
     */
    @Override
    public void resetPwd(ResetPwdDTO dto) {
        // 1. 校验新密码一致性
        if (!dto.getNewPwd().equals(dto.getConfirmPwd())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        // 2. 验证图片验证码
        boolean checkCodeValid = checkCodeService.verifyCheckCode(dto.getCheckCodeKey(), dto.getCheckCode());
        if (!checkCodeValid) {
            throw new BusinessException("图片验证码错误或已过期");
        }

        // 3. 验证邮箱验证码
        boolean emailCodeValid = emailService.verifyEmailCode(dto.getEmail(), "1", dto.getEmailCode());
        if (!emailCodeValid) {
            throw new BusinessException("邮箱验证码错误或已过期");
        }

        // 4. 查询用户是否存在
        UserInfo user = this.lambdaQuery().eq(UserInfo::getEmail, dto.getEmail()).one();
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        // 5. 加密新密码并更新
        user.setPassword(MD5Util.encrypt(dto.getNewPwd()));
        this.updateById(user);
    }

    /**
     * 根据userId更新用户信息
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        this.lambdaUpdate()
                .eq(UserInfo::getUserId, userId)
                .set(bean.getNickName() != null, UserInfo::getNickName, bean.getNickName())
                .set(bean.getEmail() != null, UserInfo::getEmail, bean.getEmail())
                .set(bean.getAvatarPath() != null, UserInfo::getAvatarPath, bean.getAvatarPath())
                .set(bean.getUseSpace() != null, UserInfo::getUseSpace, bean.getUseSpace())
                .set(bean.getVipLevel() != null, UserInfo::getVipLevel, bean.getVipLevel())
                .set(bean.getVipExpireTime() != null, UserInfo::getVipExpireTime, bean.getVipExpireTime())
                .set(bean.getVip() != null, UserInfo::getVip, bean.getVip())
                .update();
        return 1;
    }

    /**
     * 根据用户ID查询用户信息
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        if (StringTools.isEmpty(userId)) {
            throw new BusinessException("用户ID不能为空");
        }
        return this.getById(userId);
    }

    /**
     * 获取用户头像（无修改，保留原有逻辑）
     */
    @Override
    public void getAvatar(String userId, HttpServletResponse response) {
        if (StringTools.isEmpty(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        UserInfo userInfo = this.getById(userId);
        log.info("🔍 查询用户ID：{}，用户信息：{}，头像路径：{}",
                userId, userInfo, userInfo != null ? userInfo.getAvatarPath() : "用户不存在");

        if (userInfo == null || StringUtils.isEmpty(userInfo.getAvatarPath())) {
            returnDefaultAvatar(response);
            return;
        }

        String fullAvatarPath = avatarBasePath + File.separator + userInfo.getAvatarPath();
        log.info("🔍 头像完整路径：{}", fullAvatarPath);
        File avatarFile = new File(fullAvatarPath);

        if (!avatarFile.exists()) {
            log.error("❌ 头像文件不存在：{}", fullAvatarPath);
            returnDefaultAvatar(response);
            return;
        }

        try (InputStream is = new FileInputStream(avatarFile);
             OutputStream os = response.getOutputStream()) {
            String fileName = avatarFile.getName();
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

            switch (suffix) {
                case "jpg":
                case "jpeg":
                    response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                    break;
                case "png":
                    response.setContentType(MediaType.IMAGE_PNG_VALUE);
                    break;
                default:
                    response.setContentType(MediaType.IMAGE_PNG_VALUE);
            }
            FileCopyUtils.copy(is, os);
            log.info("✅ 成功返回用户头像：{}", fullAvatarPath);
        } catch (IOException e) {
            log.error("❌ 读取头像文件失败", e);
            throw new BusinessException("获取用户头像失败：" + e.getMessage());
        }
    }

    /**
     * 返回默认头像（无修改）
     */
    private void returnDefaultAvatar(HttpServletResponse response) {
        ClassPathResource defaultAvatarResource = new ClassPathResource("static/default_avatar.png");
        log.info("🔍 默认头像文件路径：{}，文件是否存在：{}",
                defaultAvatarResource.getPath(), defaultAvatarResource.exists());

        if (!defaultAvatarResource.exists()) {
            throw new BusinessException("默认头像文件不存在，请检查：src/main/resources/static/default_avatar.png");
        }

        try (InputStream defaultIs = defaultAvatarResource.getInputStream()) {
            response.setContentType(MediaType.IMAGE_PNG_VALUE);
            FileCopyUtils.copy(defaultIs, response.getOutputStream());
            log.info("✅ 成功返回默认头像");
        } catch (IOException e) {
            log.error("❌ 读取默认头像失败", e);
            throw new BusinessException("读取默认头像失败：" + e.getMessage());
        }
    }

    /**
     * 上传/更新用户头像（修正：补充文件写入逻辑 + 空间校验用枚举）
     */
    @Override
    public String uploadAvatar(AvatarUploadDTO dto) {
        // 1. 校验参数
        MultipartFile file = dto.getFile();
        String userId = dto.getUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的头像文件");
        }
        if (StringTools.isEmpty(userId)) {
            throw new BusinessException("用户ID不能为空");
        }

        // 2. 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!"png".equals(suffix) && !"jpg".equals(suffix) && !"jpeg".equals(suffix)) {
            throw new BusinessException("仅支持上传 png/jpg/jpeg 格式的头像");
        }

        // 3. 校验文件大小（2MB）
        long fileSize = file.getSize();
        if (fileSize > 2 * 1024 * 1024) {
            throw new BusinessException("头像文件大小不能超过 2MB");
        }

        // 4. 校验用户空间是否足够（修正：用枚举总空间）
        checkUserSpaceEnough(userId, fileSize);

        // 5. 构建文件存储路径
        String fileName = userId + "_" + System.currentTimeMillis() + "." + suffix;
        String relativePath = fileName;
        String fullPath = avatarBasePath + File.separator + relativePath;

        // 6. 创建目录
        File avatarDir = new File(avatarBasePath);
        if (!avatarDir.exists()) {
            boolean mkdirs = avatarDir.mkdirs();
            if (!mkdirs) {
                throw new BusinessException("创建头像存储目录失败");
            }
        }

        // 7. 写入文件（补充缺失的文件上传逻辑）
        try {
            FileUtil.writeBytes(file.getBytes(), fullPath);
            log.info("✅ 头像文件上传成功：{}", fullPath);
        } catch (IOException e) {
            log.error("❌ 头像文件写入失败", e);
            throw new BusinessException("头像上传失败：" + e.getMessage());
        }

        // 8. 更新用户表中的头像路径 + 累加已用空间
        UserInfo dbUser = this.getById(userId);
        this.lambdaUpdate()
                .eq(UserInfo::getUserId, userId)
                .set(UserInfo::getAvatarPath, relativePath)
                .set(UserInfo::getUseSpace, dbUser.getUseSpace() + fileSize)
                .update();

        // 9. 更新Redis中Token的头像路径
        String token = redisComponent.getTokenByUserId(userId);
        if (StringTools.isNotEmpty(token)) {
            TokenUserInfoDto tokenDto = redisComponent.getTokenUserInfoDto(token);
            if (tokenDto != null) {
                tokenDto.setAvatarPath(relativePath);
                redisComponent.saveTokenUserInfoDto(tokenDto);
            }
        }

        // 10. 返回头像相对路径
        return relativePath;
    }

    /**
     * 获取用户空间信息（修正：用用户实际总空间，而非硬编码5MB）
     */
    @Override
    public Map<String, Object> getUseSpace(String token) {
        // 1. 从Token解析用户信息
        TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("Token无效或已过期，请重新登录");
        }
        String userId = tokenUserInfoDto.getUserId();

        // 2. 查询用户完整信息
        UserInfo userInfo = this.getById(userId);
        if (userInfo == null) {
            throw new BusinessException("用户不存在");
        }

        // 3. 封装返回数据（修正：用用户实际总空间）
        Map<String, Object> result = new HashMap<>();
        // 基础信息
        result.put("userId", userInfo.getUserId());
        result.put("nickName", userInfo.getNickName());
        result.put("email", userInfo.getEmail());
        result.put("avatarPath", userInfo.getAvatarPath());

        // 空间信息（修正：用用户实际总空间，支持VIP）
        Long useSpace = userInfo.getUseSpace() == null ? 0L : userInfo.getUseSpace();
        Long totalSpace = userInfo.getTotalSpace(); // 从用户表获取（VIP/普通区分）
        result.put("useSpace", useSpace);
        result.put("totalSpace", totalSpace);
        result.put("useSpaceStr", formatFileSize(useSpace));
        result.put("totalSpaceStr", formatFileSize(totalSpace));
        result.put("usageRate", totalSpace == 0 ? "0.00%" : String.format("%.2f%%", (useSpace * 100.0) / totalSpace));
        result.put("isVip", userInfo.isEffectiveVip());
        result.put("vipLevelName", userInfo.getVipLevelName());

        return result;
    }

    /**
     * 修改密码
     */
    @Override
    public void changePwd(String token, ChangePwdDTO dto) {
        // 1. 从 Token 获取用户信息
        TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("登录态失效，请重新登录");
        }
        UserInfo user = this.getById(tokenUserInfoDto.getUserId());

        // 2. 校验旧密码
        if (!MD5Util.encrypt(dto.getOldPassword()).equals(user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 3. 更新新密码 + 清理 Token
        this.lambdaUpdate()
                .eq(UserInfo::getUserId, user.getUserId())
                .set(UserInfo::getPassword, MD5Util.encrypt(dto.getNewPassword()))
                .update();
        redisComponent.cleanUserTokenByUserId(user.getUserId());
    }

    /**
     * 根据QQOpenId查询用户
     */
    @Override
    public UserInfo getUserInfoByQQOpenId(String qqOpenId) {
        return this.lambdaQuery()
                .eq(UserInfo::getQqOpenId, qqOpenId)
                .one();
    }

    /**
     * 校验用户空间是否足够（修正：用用户实际总空间）
     */
    private void checkUserSpaceEnough(String userId, Long fileSize) {
        UserInfo userInfo = this.getById(userId);
        Long totalSpace = userInfo.getTotalSpace(); // 从用户表获取（支持VIP）
        Long currentUseSpace = userInfo.getUseSpace() == null ? 0L : userInfo.getUseSpace();

        if (currentUseSpace + fileSize > totalSpace) {
            throw new BusinessException("用户空间不足，当前已用：" + formatFileSize(currentUseSpace)
                    + "，总空间：" + formatFileSize(totalSpace));
        }
    }

    /**
     * 字节数转易读格式（无修改）
     */
    private String formatFileSize(Long size) {
        if (size == null) size = 0L;
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2fKB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 扩展：开通VIP（对接VipUtils）
     */
    @Override
    public void openVip(String userId, Integer months) {
        UserInfo user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 1. 计算VIP过期时间
        java.util.Date now = new java.util.Date();
        java.util.Date expireTime;
        if (user.getVipExpireTime() == null || user.getVipExpireTime().before(now)) {
            expireTime = new java.util.Date(now.getTime() + months * 30L * 24 * 60 * 60 * 1000);
        } else {
            expireTime = new java.util.Date(user.getVipExpireTime().getTime() + months * 30L * 24 * 60 * 60 * 1000);
        }

        // 2. 更新VIP状态
        user.setVipLevel(1);
        user.setVip(1);
        user.setVipExpireTime(expireTime);
        user.syncVipSpace(); // 同步VIP空间
        this.updateById(user);

        // 3. 更新Redis Token中的VIP信息
        String token = redisComponent.getTokenByUserId(userId);
        if (StringTools.isNotEmpty(token)) {
            TokenUserInfoDto tokenDto = redisComponent.getTokenUserInfoDto(token);
            if (tokenDto != null) {
                tokenDto.setVip(true);
                tokenDto.setVipLevel(1);
                tokenDto.setVipLevelName(UserVipLevelEnum.VIP.getName());
                tokenDto.setTotalSpace(UserVipLevelEnum.VIP.getTotalSpace());
                redisComponent.saveTokenUserInfoDto(tokenDto);
            }
        }
        log.info("✅ 用户{}开通VIP{}个月，过期时间：{}", userId, months, expireTime);
    }

    /**
     * 扩展：VIP到期降级
     */
    @Override
    public void expireVip(String userId) {
        UserInfo user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 1. 降级VIP状态
        user.setVipLevel(0);
        user.setVip(0);
        user.setVipExpireTime(null);
        user.syncVipSpace(); // 恢复普通空间
        this.updateById(user);

        // 2. 更新Redis Token中的VIP信息
        String token = redisComponent.getTokenByUserId(userId);
        if (StringTools.isNotEmpty(token)) {
            TokenUserInfoDto tokenDto = redisComponent.getTokenUserInfoDto(token);
            if (tokenDto != null) {
                tokenDto.setVip(false);
                tokenDto.setVipLevel(0);
                tokenDto.setVipLevelName(UserVipLevelEnum.NORMAL.getName());
                tokenDto.setTotalSpace(UserVipLevelEnum.NORMAL.getTotalSpace());
                redisComponent.saveTokenUserInfoDto(tokenDto);
            }
        }
        log.info("✅ 用户{}VIP已到期，已降级为普通用户", userId);
    }

    /**
     * 分页查询用户列表（管理员专用）
     * 支持根据昵称/邮箱模糊搜索，按创建时间倒序排序
     *
     * @param page 分页对象
     * @param queryDTO 查询条件（文件名字段用作搜索关键词）
     * @return 分页用户列表
     */
    @Override
    public IPage<UserInfo> getUserListPage(Page<UserInfo> page, FileListQueryDTO queryDTO) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringTools.isNotEmpty(queryDTO.getFileName())) {
            wrapper.like(UserInfo::getNickName, queryDTO.getFileName())
                    .or()
                    .like(UserInfo::getEmail, queryDTO.getFileName());
        }
        wrapper.orderByDesc(UserInfo::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 修改用户状态（管理员专用）
     * 可启用/禁用用户账号
     *
     * @param userId 用户ID
     * @param status 状态 0-禁用 1-启用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserStatus(String userId, Integer status) {
        UserInfo user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);
    }

    /**
     * 修改用户总存储空间（管理员专用）
     * 校验：总空间不能小于已使用空间
     *
     * @param userId 用户ID
     * @param totalSpace 新的总空间大小（字节）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserSpace(String userId, Long totalSpace) {
        UserInfo user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (totalSpace < user.getUseSpace()) {
            throw new BusinessException("总空间不能小于已使用空间");
        }
        user.setTotalSpace(totalSpace);
        this.updateById(user);
    }

    /**
     * 获取用户VIP状态VO
     */
    @Override
    public VipStatusVO getVipStatusVO(String userId) {
        UserInfo user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        VipStatusVO vipStatus = new VipStatusVO();
        vipStatus.setVipLevel(user.getVipLevel());
        vipStatus.setVipExpireTime(user.getVipExpireTime());
        vipStatus.setEffectiveVip(user.isEffectiveVip());
        vipStatus.setTotalSpace(user.getTotalSpace());
        vipStatus.setUseSpace(user.getUseSpace() == null ? 0L : user.getUseSpace());

        return vipStatus;
    }

    @Override
    public long countByVipLevel(Integer vipLevel) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getVipLevel, vipLevel);
        return this.count(wrapper);
    }
}