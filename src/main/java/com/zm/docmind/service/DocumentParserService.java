package com.zm.docmind.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档解析服务，使用 Apache Tika 统一解析各种文档格式
 */
@Slf4j
@Service
public class DocumentParserService {

    public String parseDocument(Path filePath) {
        DocumentParser tikaParser = new ApacheTikaDocumentParser();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            Document document = tikaParser.parse(inputStream);
            String text = document.text();
            log.info("文档解析成功: {}, 提取文本长度: {}", filePath.getFileName(), text.length());
            return text;
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + filePath.getFileName(), e);
        }
    }
}
