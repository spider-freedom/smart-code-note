package com.itheima.smartcodenote;

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
class WrongQuestionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void wrongAnswerCreatesWrongQuestionAndSupportsRetryAndMastered() throws Exception {
        String token = registerAndLogin("wronguser");
        long questionId = createSingleChoiceQuestion(token);

        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", "B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(0))
                .andExpect(jsonPath("$.data.correct").value(false));

        String listBody = mockMvc.perform(get("/api/wrong-questions/list")
                        .param("mastered", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].questionId").value(questionId))
                .andExpect(jsonPath("$.data.records[0].wrongCount").value(1))
                .andExpect(jsonPath("$.data.records[0].mastered").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long wrongQuestionId = objectMapper.readTree(listBody).path("data").path("records").get(0).path("id").asLong();

        mockMvc.perform(post("/api/wrong-questions/{id}/retry", wrongQuestionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(questionId))
                .andExpect(jsonPath("$.data.options[0].correct").doesNotExist());

        mockMvc.perform(put("/api/wrong-questions/{id}/mastered", wrongQuestionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(wrongQuestionId))
                .andExpect(jsonPath("$.data.mastered").value(true));

        mockMvc.perform(get("/api/wrong-questions/list")
                        .param("mastered", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].mastered").value(true));
    }

    @Test
    void repeatedWrongAnswerIncrementsWrongCount() throws Exception {
        String token = registerAndLogin("wrongrepeat");
        long questionId = createSingleChoiceQuestion(token);

        submitWrongAnswer(token, questionId);
        submitWrongAnswer(token, questionId);

        mockMvc.perform(get("/api/wrong-questions/list")
                        .param("questionId", String.valueOf(questionId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].wrongCount").value(2));
    }

    private void submitWrongAnswer(String token, long questionId) throws Exception {
        mockMvc.perform(post("/api/practice/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "answer", "B"))))
                .andExpect(status().isOk());
    }

    private long createSingleChoiceQuestion(String token) throws Exception {
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

        return objectMapper.readTree(questionBody).path("data").get(0).path("id").asLong();
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
                "wrong-note.md",
                "text/markdown",
                "# Spring Boot\n\nController: maps HTTP requests to Java methods.\nService: handles business logic."
                        .getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/api/note/upload")
                        .file(file)
                        .param("title", "Spring Boot Wrong Questions")
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
