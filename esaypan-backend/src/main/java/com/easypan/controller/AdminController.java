
package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.FileListQueryDTO;
import com.easypan.entity.dto.SysSettingDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.FileInfoService;
import com.easypan.service.UserInfoService;
import com.easypan.service.SysConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员控制器
 */
@RestController("adminController")
@RequestMapping("/admin")
public class AdminController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private SysConfigService sysConfigService;

    /**
     * 获取用户列表（分页）
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（昵称/邮箱）
     * @return 用户列表
     */
    @GetMapping("/user/list")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<UserInfo> pageObj = new Page<>(page, pageSize);
        FileListQueryDTO queryDTO = new FileListQueryDTO();
        queryDTO.setFileName(keyword);
        IPage<UserInfo> userList = userInfoService.getUserListPage(pageObj, queryDTO);
        return getSuccessResponseVO(userList);
    }

    /**
     * 修改用户状态
     * @param userId 用户ID
     * @param status 状态（0-禁用，1-启用）
     * @return 操作结果
     */
    @PostMapping("/user/status")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO changeUserStatus(
            @RequestParam String userId,
            @RequestParam Integer status) {
        userInfoService.changeUserStatus(userId, status);
        return getSuccessResponseVO("用户状态修改成功");
    }

    /**
     * 修改用户存储空间
     * @param userId 用户ID
     * @param totalSpace 总空间大小（字节）
     * @return 操作结果
     */
    @PostMapping("/user/space")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO changeUserSpace(
            @RequestParam String userId,
            @RequestParam Long totalSpace) {
        userInfoService.changeUserSpace(userId, totalSpace);
        return getSuccessResponseVO("用户存储空间修改成功");
    }

    /**
     * 获取文件列表（分页）
     * @param page 页码
     * @param pageSize 每页大小
     * @param keyword 搜索关键词（文件名）
     * @return 文件列表
     */
    @GetMapping("/file/list")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO getFileList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<FileInfo> pageObj = new Page<>(page, pageSize);
        IPage<FileInfo> fileList = fileInfoService.page(pageObj);
        return getSuccessResponseVO(fileList);
    }

    /**
     * 删除文件
     * @param fileId 文件ID
     * @return 操作结果
     */
    @PostMapping("/file/delete")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO deleteFile(
            @RequestParam String fileId) {
        fileInfoService.removeById(fileId);
        return getSuccessResponseVO("文件删除成功");
    }

    /**
     * 下载文件
     * @param fileId 文件ID
     * @param response 响应对象
     */
    @GetMapping("/file/download")
    @GlobalInterceptor(checkAdmin = true)
    public void downloadFile(
            @RequestParam String fileId,
            HttpServletResponse response) {
        try {
            fileInfoService.downloadFile(fileId, "", response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取仪表盘统计数据
     * @return 统计数据
     */
    @GetMapping("/dashboard/stats")
    @GlobalInterceptor(checkAdmin = true)

    public ResponseVO getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userInfoService.count());
        stats.put("totalFiles", fileInfoService.count());
        stats.put("totalStorage", "100GB");
        stats.put("vipUsers", userInfoService.countByVipLevel(1));
        return getSuccessResponseVO(stats);
    }

    /**
     * 获取系统设置
     * @return 系统设置
     */
    @GetMapping("/settings")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO getSystemSettings() {
        Map<String, String> settings = sysConfigService.getSystemSettings();
        return getSuccessResponseVO(settings);
    }

    /**
     * 保存系统设置
     * @param settings 系统设置
     * @return 操作结果
     */
    @PostMapping("/settings")
    @GlobalInterceptor(checkAdmin = true)
    public ResponseVO saveSystemSettings(
            @RequestBody SysSettingDto settings) {
        sysConfigService.saveSystemSettings(settings);
        return getSuccessResponseVO("保存成功");
    }
}
