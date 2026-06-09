package com.itheima.smartcodenote;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
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
class ReportControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void showLearningOverviewWeakKnowledgeAndSuggestions() throws Exception {
        String token = registerAndLogin("reportuser");
        QuestionIds questionIds = createQuestions(token);

        submitAnswer(token, questionIds.singleChoiceId(), "A");
        submitAnswer(token, questionIds.judgementId(), "false");

        mockMvc.perform(get("/api/reports/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noteCount").value(1))
                .andExpect(jsonPath("$.data.knowledgeCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.questionCount").value(3))
                .andExpect(jsonPath("$.data.answerCount").value(2))
                .andExpect(jsonPath("$.data.correctAnswerCount").value(1))
                .andExpect(jsonPath("$.data.correctRate").value(0.5))
                .andExpect(jsonPath("$.data.wrongQuestionCount").value(1));

        mockMvc.perform(get("/api/reports/weak-knowledge")
                        .param("limit", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].knowledgeId").isNumber())
                .andExpect(jsonPath("$.data[0].weaknessScore").isNumber());

        mockMvc.perform(get("/api/reports/suggestions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").isNotEmpty())
                .andExpect(jsonPath("$.data.suggestions", not(empty())))
                .andExpect(jsonPath("$.data.weakKnowledgePoints", not(empty())));
    }

    private void submitAnswer(String token, long questionId, String answer) throws Exception {
        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", answer))))
                .andExpect(status().isOk());
    }

    private QuestionIds createQuestions(String token) throws Exception {
        long noteId = uploadNote(token);
        long knowledgeId = generateKnowledge(token, noteId);

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

        JsonNode questions = objectMapper.readTree(questionBody).path("data");
        return new QuestionIds(
                questions.get(0).path("id").asLong(),
                questions.get(2).path("id").asLong());
    }

    private long generateKnowledge(String token, long noteId) throws Exception {
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
                "report-note.md",
                "text/markdown",
                "# Spring Boot\n\nController: maps HTTP requests to Java methods.\nService: handles business logic."
                        .getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "Spring Boot Report")
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

        return objectMapper.readTree(loginBody).path("data").path("token").asText();
    }

    private record QuestionIds(long singleChoiceId, long judgementId) {
    }
}
