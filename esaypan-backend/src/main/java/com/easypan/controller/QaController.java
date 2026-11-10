package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.controller.ABaseController;
import com.easypan.entity.dto.TokenUserInfoDto;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.rag.service.DocIndexService;
import com.easypan.rag.service.QaService;
import com.easypan.redis.RedisComponent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;

/**
 * 文件问答（RAG）控制器
 */
@RestController
@RequestMapping("/qa")
public class QaController extends ABaseController {

    @Resource
    private QaService qaService;

    @Resource
    private DocIndexService docIndexService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 文件问答（SSE 流式返回）
     * 事件流：sources（溯源引用）→ message（回答增量）→ done
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @GlobalInterceptor
    public SseEmitter ask(@RequestParam String question,
                          @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        return qaService.askStream(userInfo.getUserId(), question);
    }

    /**
     * 为指定文件建立/重建问答索引
     */
    @PostMapping("/index")
    @GlobalInterceptor
    public ResponseVO indexFile(@RequestParam String fileId,
                                @RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        int chunks = docIndexService.indexFile(userInfo.getUserId(), fileId);
        return getSuccessResponseVO("索引完成，共生成 " + chunks + " 个切片");
    }

    /**
     * 为当前用户所有支持索引的文档批量建立索引
     */
    @PostMapping("/indexAll")
    @GlobalInterceptor
    public ResponseVO indexAll(@RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        return getSuccessResponseVO(docIndexService.indexAll(userInfo.getUserId()));
    }

    /**
     * 查询已建立索引的文件列表
     */
    @PostMapping("/indexedFiles")
    @GlobalInterceptor
    public ResponseVO indexedFiles(@RequestHeader("token") String token) {
        TokenUserInfoDto userInfo = redisComponent.getTokenUserInfoDto(token);
        return getSuccessResponseVO(docIndexService.listIndexedFiles(userInfo.getUserId()));
    }
}
