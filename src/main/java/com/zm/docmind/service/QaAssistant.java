package com.zm.docmind.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

/**
 * AI 服务接口，LangChain4j 会自动生成实现并处理 LLM 调用。
 */
@SystemMessage("""
        你是一个专业的知识库助手。请严格基于检索到的知识库内容回答用户问题，遵循以下规则：
        1. 回答必须以检索到的内容为依据，不要编造信息
        2. 如果知识库中没有相关信息，请直接告知用户"知识库中未找到相关内容"
        3. 回答时请条理清晰，必要时使用分点列举
        4. 如果检索到的内容来自多个文档，请综合整理后给出完整回答
        """)
public interface QaAssistant {

    String answer(String question);

    TokenStream stream(String question);
}
