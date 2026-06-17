package com.zm.docmind.service;

import com.zm.docmind.entity.Document;
import com.zm.docmind.entity.DocumentStatus;
import com.zm.docmind.exception.InvalidInputException;
import com.zm.docmind.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.security.access.AccessDeniedException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentProcessingService documentProcessingService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, documentProcessingService);
        ReflectionTestUtils.setField(documentService, "storagePath", tempDir.toString());
        ReflectionTestUtils.setField(documentService, "maxFileSize", 10);
    }

    @Nested
    @DisplayName("uploadDocument")
    class UploadDocument {

        @Test
        @DisplayName("正常上传应返回 documentId 并保存文档")
        void success() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "content".getBytes());

            String documentId = documentService.uploadDocument(file, "user@test.com", false);

            assertThat(documentId).isNotBlank();
            verify(documentRepository).save(any(Document.class));
            verify(documentProcessingService).processDocumentAsync(anyString(), any(Path.class),
                    eq("test.pdf"), eq("user@test.com"), eq(false));
        }

        @Test
        @DisplayName("文件名为 null 应抛 InvalidInputException")
        void nullFilename_throwsException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "application/pdf", "content".getBytes());

            assertThatThrownBy(() -> documentService.uploadDocument(file, "user@test.com", false))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("超大文件应抛 InvalidInputException")
        void oversizedFile_throwsException() {
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB, 超过 10MB 限制
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", largeContent);

            assertThatThrownBy(() -> documentService.uploadDocument(file, "user@test.com", false))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("文件过大");
        }

        @Test
        @DisplayName("不支持的文件类型应抛 InvalidInputException")
        void unsupportedType_throwsException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.exe", "application/octet-stream", "content".getBytes());

            assertThatThrownBy(() -> documentService.uploadDocument(file, "user@test.com", false))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("不支持");
        }

        @Test
        @DisplayName("上传的文档初始状态应为 PENDING")
        void initialStatusIsPending() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "hello".getBytes());

            documentService.uploadDocument(file, "user@test.com", true);

            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(DocumentStatus.PENDING);
            assertThat(captor.getValue().isPublic()).isTrue();
            assertThat(captor.getValue().getUserId()).isEqualTo("user@test.com");
        }
    }

    @Nested
    @DisplayName("getDocumentForUser")
    class GetDocumentForUser {

        @Test
        @DisplayName("公开文档任何人可访问")
        void publicDocument_success() {
            Document doc = Document.builder().id("1").userId("owner@test.com").isPublic(true).build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            Document result = documentService.getDocumentForUser("1", "other@test.com");

            assertThat(result.getId()).isEqualTo("1");
        }

        @Test
        @DisplayName("所有者可访问自己的私有文档")
        void ownerAccessPrivate_success() {
            Document doc = Document.builder().id("1").userId("owner@test.com").isPublic(false).build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            Document result = documentService.getDocumentForUser("1", "owner@test.com");

            assertThat(result.getId()).isEqualTo("1");
        }

        @Test
        @DisplayName("非所有者访问私有文档应抛 AccessDeniedException")
        void otherUserAccessPrivate_throwsAccessDenied() {
            Document doc = Document.builder().id("1").userId("owner@test.com").isPublic(false).build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> documentService.getDocumentForUser("1", "other@test.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("文档不存在应抛 NoSuchElementException")
        void nonexistentDoc_throwsNoSuchElement() {
            when(documentRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getDocumentForUser("999", "user@test.com"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("文档不存在");
        }
    }

    @Nested
    @DisplayName("getOwnedDocument")
    class GetOwnedDocument {

        @Test
        @DisplayName("所有者可操作")
        void owner_success() {
            Document doc = Document.builder().id("1").userId("owner@test.com").build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            Document result = documentService.getOwnedDocument("1", "owner@test.com");

            assertThat(result.getId()).isEqualTo("1");
        }

        @Test
        @DisplayName("非所有者应抛 AccessDeniedException")
        void otherUser_throwsAccessDenied() {
            Document doc = Document.builder().id("1").userId("owner@test.com").build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> documentService.getOwnedDocument("1", "other@test.com"))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("文档不存在应抛 NoSuchElementException")
        void nonexistentDoc_throwsNoSuchElement() {
            when(documentRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.getOwnedDocument("999", "user@test.com"))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocument {

        @Test
        @DisplayName("所有者删除存在的文档应成功")
        void existingDoc_success() {
            Document doc = Document.builder().id("1").userId("owner@test.com")
                    .filePath(tempDir.resolve("test.txt").toString()).build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            documentService.deleteDocument("1", "owner@test.com");

            verify(documentRepository).deleteById("1");
        }

        @Test
        @DisplayName("不存在的文档应抛 NoSuchElementException")
        void nonexistentDoc_throwsNoSuchElement() {
            when(documentRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.deleteDocument("999", "user@test.com"))
                    .isInstanceOf(NoSuchElementException.class);
            verify(documentRepository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("非所有者删除应抛 AccessDeniedException")
        void nonOwner_throwsAccessDenied() {
            Document doc = Document.builder().id("1").userId("owner@test.com").build();
            when(documentRepository.findById("1")).thenReturn(Optional.of(doc));

            assertThatThrownBy(() -> documentService.deleteDocument("1", "other@test.com"))
                    .isInstanceOf(AccessDeniedException.class);
            verify(documentRepository, never()).deleteById(anyString());
        }
    }
}
