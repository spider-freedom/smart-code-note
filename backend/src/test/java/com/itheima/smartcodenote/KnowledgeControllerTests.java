package com.itheima.smartcodenote;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class KnowledgeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateListDetailUpdateAndDeleteKnowledgePoint() throws Exception {
        String token = registerAndLogin("knowledgeuser");
        long noteId = uploadNote(token);

        String generateBody = mockMvc.perform(post("/api/knowledge/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("noteId", noteId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].noteId").value(noteId))
                .andExpect(jsonPath("$.data[0].masteryLevel").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long knowledgeId = objectMapper.readTree(generateBody).path("data").get(0).path("id").asLong();

        mockMvc.perform(get("/api/knowledge/list")
                        .param("noteId", String.valueOf(noteId))
                        .param("type", "concept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(knowledgeId));

        mockMvc.perform(get("/api/knowledge/{id}", knowledgeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(knowledgeId))
                .andExpect(jsonPath("$.data.summary").isNotEmpty());

        mockMvc.perform(put("/api/knowledge/{id}", knowledgeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Spring MVC Controller Mapping",
                                "type", "code",
                                "summary", "Controller methods map HTTP requests.",
                                "difficulty", "easy",
                                "masteryLevel", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring MVC Controller Mapping"))
                .andExpect(jsonPath("$.data.masteryLevel").value(2));

        mockMvc.perform(delete("/api/knowledge/{id}", knowledgeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/knowledge/{id}", knowledgeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("knowledge point not found"));
    }

    @Test
    void rejectGeneratingKnowledgeFromAnotherUsersNote() throws Exception {
        String ownerToken = registerAndLogin("knowledgeowner");
        long ownerNoteId = uploadNote(ownerToken);
        String otherToken = registerAndLogin("knowledgeother");

        mockMvc.perform(post("/api/knowledge/generate")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("noteId", ownerNoteId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("note not found"));
    }

    private long uploadNote(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "knowledge-note.md",
                "text/markdown",
                "# Spring Boot\n\nController: maps HTTP requests to Java methods.\nService: handles business logic."
                        .getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "Spring Boot Knowledge")
                        .param("category", "Spring")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(uploadBody).path("data").path("id").asLong();
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
