package com.itheima.smartcodenote;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadListDetailReparseAndDeleteNote() throws Exception {
        String token = registerAndLogin("noteuser");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "spring-note.md",
                "text/markdown",
                "# Spring Boot\n\n\n  Controller maps HTTP requests.\n".getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "Spring Boot Note")
                        .param("category", "Spring")
                        .param("tags", "java,spring")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Spring Boot Note"))
                .andExpect(jsonPath("$.data.fileType").value("md"))
                .andExpect(jsonPath("$.data.parseStatus").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long noteId = objectMapper.readTree(uploadBody).path("data").path("id").asLong();

        mockMvc.perform(get("/api/note/list")
                        .param("keyword", "Spring")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(noteId));

        mockMvc.perform(get("/api/note/{id}", noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noteId))
                .andExpect(jsonPath("$.data.cleanContent").value("# Spring Boot\n\nController maps HTTP requests."));

        mockMvc.perform(post("/api/note/{id}/parse", noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noteId))
                .andExpect(jsonPath("$.data.parseStatus").value(1));

        mockMvc.perform(delete("/api/note/{id}", noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/note/{id}", noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("note not found"));
    }

    @Test
    void rejectUnsupportedFileType() throws Exception {
        String token = registerAndLogin("noteuser2");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.pdf",
                "application/pdf",
                "fake pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "PDF Note")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("only .txt and .md files are supported"));
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "123456",
                                "confirmPassword", "123456"))))
                .andExpect(status().isOk());

        String loginBody = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "account", username,
                                "password", "123456"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(loginBody);
        return root.path("data").path("token").asText();
    }
}
