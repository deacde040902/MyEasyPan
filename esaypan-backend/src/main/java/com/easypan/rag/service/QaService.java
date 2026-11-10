package com.easypan.rag.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface QaService {

    /**
     * 权限感知的文件问答（SSE 流式）
     * <p>
     * 事件流：sources（引用来源 JSON）→ message（回答增量）→ done
     */
    SseEmitter askStream(String userId, String question);
}
