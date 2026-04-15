package com.easypan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.dto.FileListQueryDTO;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.redis.RedisComponent;
import com.easypan.service.FileInfoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件信息表 前端控制器（完整接口版）
 */
@RestController
@RequestMapping("/file")
public class FileInfoController extends ABaseController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 加载文件列表
     */
    @PostMapping("/loadDataList")
    @GlobalInterceptor
    public ResponseVO loadDataList(@RequestBody FileListQueryDTO queryDTO,
                                   @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        IPage<FileInfoVO> filePage = fileInfoService.loadFileList(queryDTO, userInfo.getUserId());
        return getSuccessResponseVO(filePage);
    }

    /**
     * 新建文件夹
     */
    @PostMapping("/createFolder")
    @GlobalInterceptor
    public ResponseVO createFolder(@RequestParam String filePid,
                                   @RequestParam String folderName,
                                   @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.createFolder(filePid, folderName, userInfo.getUserId());
        return getSuccessResponseVO("文件夹创建成功");
    }

    /**
     * 文件重命名
     */
    @PostMapping("/rename")
    @GlobalInterceptor
    public ResponseVO renameFile(@RequestParam String fileId,
                                 @RequestParam String newFileName,
                                 @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.renameFile(fileId, newFileName, userInfo.getUserId());
        return getSuccessResponseVO("重命名成功");
    }

    /**
     * 移动文件
     */
    @PostMapping("/move")
    @GlobalInterceptor
    public ResponseVO moveFile(@RequestParam String fileId,
                               @RequestParam String targetPid,
                               @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.moveFile(fileId, targetPid, userInfo.getUserId());
        return getSuccessResponseVO("移动成功");
    }

    /**
     * 删除文件（移入回收站）
     */
    @PostMapping("/delete")
    @GlobalInterceptor
    public ResponseVO deleteFile(@RequestParam String fileId,
                                 @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.deleteFile(fileId, userInfo.getUserId());
        return getSuccessResponseVO("删除成功，文件已移入回收站");
    }

    /**
     * 获取文件路径（面包屑）
     */
    @PostMapping("/getFilePath")
    @GlobalInterceptor
    public ResponseVO getFilePath(@RequestParam String fileId,
                                  @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        List<FileInfoVO> pathList = fileInfoService.getFilePath(fileId, userInfo.getUserId());
        return getSuccessResponseVO(pathList);
    }

    /**
     * 获取目录树
     */
    @PostMapping("/getFolderTree")
    @GlobalInterceptor
    public ResponseVO getFolderTree(@RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        List<FileInfoVO> folderTree = fileInfoService.getFolderTree(userInfo.getUserId());
        return getSuccessResponseVO(folderTree);
    }

    /**
     * 预览文件（调用 Service 层逻辑）
     */
    @GetMapping("/preview/{fileId}")
    @GlobalInterceptor
    public void previewFile(@PathVariable String fileId,
                            @RequestHeader("token") String token,
                            HttpServletResponse response) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.previewFile(fileId, userInfo.getUserId(), response);
    }

    /**
     * 显示文件封面（图片/视频封面）
     */
    @GetMapping("/cover/{fileId}")
    @GlobalInterceptor
    public void getFileCover(@PathVariable String fileId,
                             @RequestHeader("token") String token,
                             HttpServletResponse response) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.getFileCover(fileId, userInfo.getUserId(), response);
    }

    /**
     * 创建下载链接（生成临时验证码）
     */
    @PostMapping("/createDownloadUrl")
    @GlobalInterceptor
    public ResponseVO<String> createDownloadUrl(@RequestParam String fileId,
                                                @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        String downloadCode = fileInfoService.createDownloadUrl(fileId, userInfo.getUserId());
        return getSuccessResponseVO(downloadCode);
    }

    /**
     * 获取回收站文件列表
     */
    @PostMapping("/recycle/list")
    @GlobalInterceptor
    public ResponseVO<IPage<FileInfoVO>> getRecycleList(@RequestBody FileListQueryDTO queryDTO,
                                                        @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        IPage<FileInfoVO> recyclePage = fileInfoService.getRecycleFileList(queryDTO, userInfo.getUserId());
        return getSuccessResponseVO(recyclePage);
    }

    /**
     * 恢复回收站文件
     */
    @PostMapping("/recycle/recover")
    @GlobalInterceptor
    public ResponseVO recoverFile(@RequestParam String fileId,
                                  @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.recoverFile(fileId, userInfo.getUserId());
        return getSuccessResponseVO("恢复成功");
    }

    /**
     * 永久删除文件
     */
    @PostMapping("/recycle/delete")
    @GlobalInterceptor
    public ResponseVO permanentDeleteFile(@RequestParam String fileId,
                                          @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileInfoService.permanentDeleteFile(fileId, userInfo.getUserId());
        return getSuccessResponseVO("永久删除成功");
    }
}