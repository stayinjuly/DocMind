package com.zm.docmind.controller;

import com.zm.docmind.service.QaAssistant;
import com.zm.docmind.service.QaAssistantManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/qa")
public class QaController {

    private final QaAssistantManager assistantManager;

    public QaController(QaAssistantManager assistantManager) {
        this.assistantManager = assistantManager;
    }

    @GetMapping
    public String ask(@AuthenticationPrincipal String email,
                      @RequestParam String question) {
        QaAssistant assistant = assistantManager.getAssistant(email);
        return assistant.answer(question);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@AuthenticationPrincipal String email,
                                @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时, 用户: {}", email);
            emitter.complete();
        });
        emitter.onCompletion(() -> {
            log.debug("SSE 连接关闭, 用户: {}", email);
        });

        QaAssistant assistant = assistantManager.getAssistant(email);

        assistant.stream(question)
                .onPartialResponse(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .onCompleteResponse(response -> {
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .onError(e -> {
                    log.error("流式问答出错, 用户: {}", email, e);
                    try {
                        emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                        emitter.completeWithError(e);
                    } catch (IOException ignored) {
                        // 发送失败说明客户端已断开，无需处理
                    }
                })
                .start();

        return emitter;
    }

    /**
     * 清除当前用户对话历史
     */
    @DeleteMapping("/history")
    public String clearHistory(@AuthenticationPrincipal String email) {
        assistantManager.clearUserHistory(email);
        return "对话历史已清除";
    }
}
