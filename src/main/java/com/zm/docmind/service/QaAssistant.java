package com.zm.docmind.service;

import dev.langchain4j.service.SystemMessage;

/**
 * AI 服务接口，LangChain4j 会自动生成实现并处理 LLM 调用。
 */
public interface QaAssistant {

    @SystemMessage("你是企业知识库助手，只能根据知识库内容回答，不知道则直接说不知道。")
    String answer(String question);
}
