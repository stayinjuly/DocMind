package com.zm.docmind.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

/**
 * AI 服务接口，LangChain4j 会自动生成实现并处理 LLM 调用。
 */
@SystemMessage("你是知识库助手，只能根据知识库内容回答，不知道则直接说不知道。")
public interface QaAssistant {

    String answer(String question);

    TokenStream stream(String question);
}
