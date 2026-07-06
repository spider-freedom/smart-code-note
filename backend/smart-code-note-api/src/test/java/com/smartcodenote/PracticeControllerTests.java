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
class PracticeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startPracticeAndSubmitObjectiveAnswer() throws Exception {
        String token = registerAndLogin("practiceuser");
        QuestionIds questionIds = createQuestions(token);

        mockMvc.perform(get("/api/practice/start")
                        .param("knowledgeId", String.valueOf(questionIds.knowledgeId()))
                        .param("count", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].standardAnswer").doesNotExist())
                .andExpect(jsonPath("$.data[0].options[0].correct").doesNotExist());

        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionIds.singleChoiceId(),
                                "answer", "A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionId").value(questionIds.singleChoiceId()))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.recordId").isNumber());
    }

    @Test
    void submitSubjectiveAnswerWithMockAiScore() throws Exception {
        String token = registerAndLogin("practicesubjective");
        QuestionIds questionIds = createQuestions(token);

        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionIds.shortAnswerId(),
                                "answer", "Spring Boot Controller maps HTTP requests and Service handles business logic."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionType").value("short_answer"))
                .andExpect(jsonPath("$.data.score").value(100))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.aiComment").isNotEmpty());
    }

    @Test
    void rejectSubmittingAnotherUsersQuestion() throws Exception {
        String ownerToken = registerAndLogin("practiceowner");
        QuestionIds ownerQuestionIds = createQuestions(ownerToken);
        String otherToken = registerAndLogin("practiceother");

        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", ownerQuestionIds.singleChoiceId(),
                                "answer", "A"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("question not found"));
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
                knowledgeId,
                questions.get(0).path("id").asLong(),
                questions.get(1).path("id").asLong());
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
                "practice-note.md",
                "text/markdown",
                "# Spring Boot\n\nController: maps HTTP requests to Java methods.\nService: handles business logic."
                        .getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "Spring Boot Practice")
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

    private record QuestionIds(long knowledgeId, long singleChoiceId, long shortAnswerId) {
    }
}
