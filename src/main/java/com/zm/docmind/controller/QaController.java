package com.zm.docmind.controller;

import com.zm.docmind.dto.ApiResponse;
import com.zm.docmind.dto.QaRequest;
import com.zm.docmind.service.QaAssistant;
import com.zm.docmind.service.QaAssistantManager;
import com.zm.docmind.service.QaRateLimiter;
import jakarta.validation.Valid;
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
    private final QaRateLimiter rateLimiter;

    public QaController(QaAssistantManager assistantManager, QaRateLimiter rateLimiter) {
        this.assistantManager = assistantManager;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ApiResponse<String> ask(@AuthenticationPrincipal String userId,
                                   @Valid @RequestBody QaRequest request) {
        rateLimiter.checkAndConsume(userId);
        QaAssistant assistant = assistantManager.getAssistant(userId);
        String answer = assistant.answer(request.getQuestion());
        return ApiResponse.ok(answer);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody QaRequest request) {
        rateLimiter.checkAndConsume(userId);
        String question = request.getQuestion();
        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时, 用户: {}", userId);
            emitter.complete();
        });
        emitter.onCompletion(() -> {
            log.debug("SSE 连接关闭, 用户: {}", userId);
        });

        QaAssistant assistant = assistantManager.getAssistant(userId);

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
                    log.error("流式问答出错, 用户: {}", userId, e);
                    try {
                        emitter.send(SseEmitter.event().data("[ERROR] 问答服务暂时不可用"));
                        emitter.completeWithError(e);
                    } catch (IOException ignored) {
                        // 发送失败说明客户端已断开，无需处理
                    }
                })
                .start();

        return emitter;
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal String userId) {
        assistantManager.clearUserHistory(userId);
        return ApiResponse.ok("对话历史已清除", null);
    }
}
