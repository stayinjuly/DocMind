package com.zm.docmind.controller;

import com.zm.docmind.service.QaAssistant;
import com.zm.docmind.service.QaAssistantManager;
import org.springframework.http.MediaType;
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
     * @param userId 用户ID（可选，默认为 "default"）
     * @param question 用户问题
     */
    @GetMapping
    public String ask(@RequestParam(required = false, defaultValue = "default") String userId,
                      @RequestParam String question) {
        QaAssistant assistant = assistantManager.getAssistant(userId);
        return assistant.answer(question);
    }

    /**
     * 流式问答接口（SSE）
     * @param userId 用户ID（可选，默认为 "default"）
     * @param question 用户问题
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestParam(required = false, defaultValue = "default") String userId,
                                @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(60000L);
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
                .onError(emitter::completeWithError);

        return emitter;
    }

    /**
     * 清除用户对话历史
     */
    @DeleteMapping("/history/del/{userId}")
    public String clearHistory(@PathVariable String userId) {
        assistantManager.clearUserHistory(userId);
        return "用户 " + userId + " 的对话历史已清除";
    }
}
