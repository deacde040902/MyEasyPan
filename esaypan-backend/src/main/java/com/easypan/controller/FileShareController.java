package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.vo.FileShareVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.redis.RedisComponent;
import com.easypan.service.FileShareService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 文件分享表 前端控制器（完整接口版）
 */
@RestController
@RequestMapping("/file/share") // 适配分享接口路径
public class FileShareController extends ABaseController {

    @Resource
    private FileShareService fileShareService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 创建分享链接
     */
    @PostMapping("/create")
    @GlobalInterceptor
    public ResponseVO createShare(@RequestParam String fileId,
                                  @RequestParam Integer expireType,
                                  @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        FileShareVO shareVO = fileShareService.createShare(fileId, expireType, userInfo.getUserId());
        return getSuccessResponseVO(shareVO);
    }

    /**
     * 取消分享
     */
    @PostMapping("/cancel")
    @GlobalInterceptor
    public ResponseVO cancelShare(@RequestParam String shareId,
                                  @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileShareService.cancelShare(shareId, userInfo.getUserId());
        return getSuccessResponseVO("取消分享成功");
    }

    /**
     * 校验分享码
     */
    @PostMapping("/verifyCode")
    public ResponseVO verifyShareCode(@RequestParam String shareId,
                                      @RequestParam String shareCode) {
        fileShareService.verifyShareCode(shareId, shareCode);
        return getSuccessResponseVO("校验成功");
    }

    /**
     * 获取分享文件信息
     */
    @GetMapping("/getFileInfo")
    public ResponseVO getShareFileInfo(@RequestParam String shareId) {
        return getSuccessResponseVO(fileShareService.getShareFileInfo(shareId));
    }
    /**
     * 获取用户分享列表
     */
    @PostMapping("/list")
    @GlobalInterceptor
    public ResponseVO<List> getShareList(@RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        return getSuccessResponseVO(fileShareService.getShareList(userInfo.getUserId()));
    }

    /**
     * 保存分享文件到我的网盘
     */
    @PostMapping("/save")
    @GlobalInterceptor
    public ResponseVO saveShareFile(@RequestParam String shareId,
                                    @RequestParam String filePid,
                                    @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileShareService.saveShareFile(shareId, filePid, userInfo.getUserId());
        return getSuccessResponseVO("保存成功");
    }
}