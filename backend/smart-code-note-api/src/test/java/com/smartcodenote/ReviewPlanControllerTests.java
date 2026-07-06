package com.smartcodenote;

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
class ReviewPlanControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listTodayReviewTasksAndSubmitReviewResult() throws Exception {
        String token = registerAndLogin("reviewuser");
        long knowledgeId = createKnowledgePoint(token);

        mockMvc.perform(get("/api/reviews/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].knowledgeId").value(knowledgeId))
                .andExpect(jsonPath("$.data[0].masteryLevel").value(0));

        mockMvc.perform(post("/api/reviews/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "knowledgeId", knowledgeId,
                                "reviewResult", "remembered",
                                "score", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.knowledgeId").value(knowledgeId))
                .andExpect(jsonPath("$.data.reviewResult").value("remembered"))
                .andExpect(jsonPath("$.data.masteryLevel").value(1))
                .andExpect(jsonPath("$.data.nextReviewTime").isNotEmpty());

        mockMvc.perform(get("/api/knowledge/{id}", knowledgeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.masteryLevel").value(1))
                .andExpect(jsonPath("$.data.nextReviewTime").isNotEmpty());
    }

    @Test
    void answerSubmissionUpdatesKnowledgeReviewPlan() throws Exception {
        String token = registerAndLogin("reviewpractice");
        long knowledgeId = createKnowledgePoint(token);
        long questionId = generateSingleChoiceQuestion(token, knowledgeId);

        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", "A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true));

        mockMvc.perform(get("/api/knowledge/{id}", knowledgeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.masteryLevel").value(1))
                .andExpect(jsonPath("$.data.nextReviewTime").isNotEmpty());
    }

    @Test
    void rejectSubmittingReviewForAnotherUsersKnowledgePoint() throws Exception {
        String ownerToken = registerAndLogin("reviewowner");
        long ownerKnowledgeId = createKnowledgePoint(ownerToken);
        String otherToken = registerAndLogin("reviewother");

        mockMvc.perform(post("/api/reviews/submit")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "knowledgeId", ownerKnowledgeId,
                                "reviewResult", "remembered"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("knowledge point not found"));
    }

    private long generateSingleChoiceQuestion(String token, long knowledgeId) throws Exception {
        String questionBody = mockMvc.perform(post("/api/questions/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "knowledgeId", knowledgeId,
                                "count", 3))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(questionBody).path("data").get(0).path("id").asLong();
    }

    private long createKnowledgePoint(String token) throws Exception {
        long noteId = uploadNote(token);

        String generateBody = mockMvc.perform(post("/api/knowledge/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("noteId", noteId))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(generateBody).path("data").get(0).path("id").asLong();
    }

    private long uploadNote(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "review-note.md",
                "text/markdown",
                "# Spring Boot\n\nController: maps HTTP requests to Java methods.\nService: handles business logic."
                        .getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "Spring Boot Review")
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
