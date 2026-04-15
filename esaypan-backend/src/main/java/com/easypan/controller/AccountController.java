package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.*;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.EmailService;
import com.easypan.service.CheckCodeService;
import com.easypan.service.UserInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 用户信息 Controller
 */
@RestController("userInfoController")
@RequestMapping("/userInfo")
public class AccountController extends ABaseController {

    @Resource
    private CheckCodeService checkCodeService;

    @Resource
    private EmailService emailService;

    @Resource
    private UserInfoService userInfoService;

    /**
     * 生成验证码（返回base64图片+验证码key）
     */
    @RequestMapping("/checkCode")
    public ResponseVO checkCode() {
        Map<String, String> result = checkCodeService.generateCheckCode();
        return getSuccessResponseVO(result);
    }

    /**
     * 发送qq邮箱验证码
     */
    @PostMapping("/sendEmailCode")
    public ResponseVO sendEmailCode(@Validated @RequestBody SendEmailCodeDTO dto) {
        userInfoService.sendEmailCode(dto);
        return getSuccessResponseVO("验证码发送成功，请注意查收");
    }
    /**
     * 用户注册接口
     */
    @PostMapping("/register") // 注意：这里是 POST，不是 GET
    public ResponseVO register(@Validated @RequestBody RegisterDTO dto) {
        userInfoService.register(dto);
        return getSuccessResponseVO("注册成功");
    }
    /**
     * 登录功能
     */
    @PostMapping("/login")
    public ResponseVO login(@Validated @RequestBody LoginDTO dto) {
        Map<String, String> result = userInfoService.login(dto);
        return getSuccessResponseVO(result);
    }

    /**
     * 重置密码接口 --忘记密码找回操作
     */
    @PostMapping("/resetPwd")
    public ResponseVO resetPwd(@Validated @RequestBody ResetPwdDTO dto) {
        userInfoService.resetPwd(dto);
        return getSuccessResponseVO("密码重置成功，请使用新密码登录");
    }

    /**
     * 获取用户头像
     * @param userId 用户ID
     * @return 头像文件流或默认头像
     */
    @GetMapping("/getAvatar")
    @GlobalInterceptor
    public void getAvatar(@RequestParam String userId,
                          HttpServletResponse response) {
        userInfoService.getAvatar(userId, response);
    }
    /**
     * 上传/更新用户头像
     * @param dto 包含用户ID和头像文件
     * @return 上传结果
     */
    @PostMapping("/uploadAvatar")
    @GlobalInterceptor
    public ResponseVO uploadAvatar(@Validated AvatarUploadDTO dto) {
        String avatarPath = userInfoService.uploadAvatar(dto);
        String resultMsg = String.format("头像上传成功，存储路径：%s", avatarPath);
        return getSuccessResponseVO(resultMsg);
    }

    /**
     * 根据用户ID获取用户基本信息
     * @param userId 用户ID
     * @return 用户基本信息（不含密码等敏感字段）
     */
    @GetMapping("/getUserInfo/{userId}")
    @GlobalInterceptor
    public ResponseVO getUserInfo(@PathVariable("userId") String userId) {
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        if (userInfo != null) {
            userInfo.setPassword(null);
        }
        return getSuccessResponseVO(userInfo);
    }

    /**
     * 获取用户空间与基础信息
     * @param token 登录Token（从请求头获取）
     * @return 用户基础信息 + 空间使用情况
     */
    @GetMapping("/getUseSpace")
    @GlobalInterceptor
    public ResponseVO getUseSpace(@RequestHeader(Constants.REQUEST_HEADER_TOKEN) String token) {
        return getSuccessResponseVO(userInfoService.getUseSpace(token));
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePwd")
    @GlobalInterceptor
    public ResponseVO changePwd(@RequestHeader(Constants.REQUEST_HEADER_TOKEN) String token,
                                @Validated @RequestBody ChangePwdDTO dto) {
        userInfoService.changePwd(token, dto);
        return getSuccessResponseVO("密码修改成功，请重新登录");
    }
}