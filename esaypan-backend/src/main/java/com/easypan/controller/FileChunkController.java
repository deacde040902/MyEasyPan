package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.redis.RedisComponent;
import com.easypan.service.FileChunkService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 文件分片上传记录表 前端控制器（完整接口版）
 */
@RestController
@RequestMapping("/file/chunk") // 适配分片上传接口路径
public class FileChunkController extends ABaseController {

    @Resource
    private FileChunkService fileChunkService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 初始化分片上传
     */
    @PostMapping("/init")
    @GlobalInterceptor
    public ResponseVO initChunkUpload(@RequestParam String fileMd5,
                                      @RequestParam String fileName,
                                      @RequestParam Long fileSize,
                                      @RequestParam Integer chunkSize,
                                      @RequestParam String filePid,
                                      @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        String uploadId = fileChunkService.initUpload(fileMd5, fileName, fileSize, chunkSize, filePid, userInfo.getUserId());
        return getSuccessResponseVO(uploadId);
    }

    /**
     * 上传分片
     */
    @PostMapping("/upload")
    @GlobalInterceptor
    public ResponseVO uploadChunk(@RequestParam String uploadId,
                                  @RequestParam Integer chunkIndex,
                                  @RequestParam MultipartFile file,
                                  @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileChunkService.uploadChunk(uploadId, chunkIndex, file, userInfo.getUserId());
        return getSuccessResponseVO("分片上传成功");
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    @GlobalInterceptor
    public ResponseVO mergeChunk(@RequestParam String uploadId,
                                 @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        fileChunkService.mergeChunk(uploadId, userInfo.getUserId());
        return getSuccessResponseVO("文件合并成功");
    }

    /**
     * 查询已上传分片
     */
    @GetMapping("/getUploadedChunks")
    @GlobalInterceptor
    public ResponseVO getUploadedChunks(@RequestParam String uploadId,
                                        @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        return getSuccessResponseVO(fileChunkService.getUploadedChunks(uploadId, userInfo.getUserId()));
    }
}