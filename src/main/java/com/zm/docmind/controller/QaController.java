package com.zm.docmind.controller;

import com.zm.docmind.service.QaAssistant;
import com.zm.docmind.service.QaAssistantManager;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/qa")
public class QaController {

    private final QaAssistantManager assistantManager;

    public QaController(QaAssistantManager assistantManager) {
        this.assistantManager = assistantManager;
    }

    /**
     * 问答接口（支持用户隔离）
     * @param email 当前登录用户邮箱（从 JWT 令牌中提取）
     * @param question 用户问题
     */
    @GetMapping
    public String ask(@AuthenticationPrincipal String email,
                      @RequestParam String question) {
        QaAssistant assistant = assistantManager.getAssistant(email);
        return assistant.answer(question);
    }

    /**
     * 流式问答接口（SSE）
     * @param email 当前登录用户邮箱（从 JWT 令牌中提取）
     * @param question 用户问题
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@AuthenticationPrincipal String email,
                                @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(60000L);
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
                    try {
                        emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                        emitter.complete();
                    } catch (IOException ignored) {
                        emitter.completeWithError(e);
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
