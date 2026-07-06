package com.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcodenote.ai.AiService;
import com.smartcodenote.common.PageResponse;
import com.smartcodenote.dto.GenerateQuestionRequest;
import com.smartcodenote.dto.QuestionDetailResponse;
import com.smartcodenote.dto.QuestionListItemResponse;
import com.smartcodenote.dto.QuestionOptionResponse;
import com.smartcodenote.dto.QuestionQueryRequest;
import com.smartcodenote.entity.KnowledgePoint;
import com.smartcodenote.entity.Question;
import com.smartcodenote.entity.QuestionOption;
import com.smartcodenote.exception.BusinessException;
import com.smartcodenote.mapper.KnowledgePointMapper;
import com.smartcodenote.mapper.QuestionMapper;
import com.smartcodenote.mapper.QuestionOptionMapper;
import com.smartcodenote.service.QuestionService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final int DEFAULT_QUESTION_COUNT = 3;

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final AiService aiService;

    @Override
    public List<QuestionDetailResponse> generate(Long userId, GenerateQuestionRequest request) {
        KnowledgePoint knowledgePoint = requireOwnedKnowledge(userId, request.getKnowledgeId());
        int count = request.getCount() == null ? DEFAULT_QUESTION_COUNT : request.getCount();
        List<AiService.GeneratedQuestion> generatedQuestions =
                aiService.generateQuestions(knowledgePoint.getTitle(), knowledgePoint.getSummary(), count);
        if (generatedQuestions.isEmpty()) {
            throw new BusinessException("no questions generated");
        }

        deleteQuestionsByKnowledge(userId, knowledgePoint.getId());

        LocalDateTime now = LocalDateTime.now();
        return generatedQuestions.stream()
                .map(generatedQuestion -> createQuestion(userId, knowledgePoint, generatedQuestion, now))
                .map(this::toDetail)
                .toList();
    }

    @Override
    public PageResponse<QuestionListItemResponse> list(Long userId, QuestionQueryRequest request) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getUserId, userId)
                .eq(request.getNoteId() != null, Question::getNoteId, request.getNoteId())
                .eq(request.getKnowledgeId() != null, Question::getKnowledgeId, request.getKnowledgeId())
                .eq(StringUtils.hasText(request.getQuestionType()), Question::getQuestionType, request.getQuestionType())
                .eq(StringUtils.hasText(request.getDifficulty()), Question::getDifficulty, request.getDifficulty())
                .and(StringUtils.hasText(request.getKeyword()), query -> query
                        .like(Question::getContent, request.getKeyword())
                        .or()
                        .like(Question::getStandardAnswer, request.getKeyword()))
                .orderByDesc(Question::getCreateTime);
        Page<Question> page = questionMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                wrapper);
        return PageResponse.of(
                page.getRecords().stream().map(this::toListItem).toList(),
                page.getTotal(),
                request.getPageNum(),
                request.getPageSize());
    }

    @Override
    public QuestionDetailResponse detail(Long userId, Long questionId) {
        return toDetail(requireOwnedQuestion(userId, questionId));
    }

    @Override
    public void generateStream(Long userId, GenerateQuestionRequest request, SseEmitter emitter) {
        KnowledgePoint knowledgePoint = requireOwnedKnowledge(userId, request.getKnowledgeId());
        int count = request.getCount() == null ? DEFAULT_QUESTION_COUNT : request.getCount();

        aiService.generateQuestionsStream(
                knowledgePoint.getTitle(),
                knowledgePoint.getSummary(),
                count,
                chunk -> sendSse(emitter, "chunk", chunk),
                generatedQuestions -> {
                    if (generatedQuestions.isEmpty()) {
                        sendSse(emitter, "error", "AI未生成题目");
                        emitter.complete();
                        return;
                    }

                    deleteQuestionsByKnowledge(userId, knowledgePoint.getId());

                    LocalDateTime now = LocalDateTime.now();
                    List<QuestionDetailResponse> result = generatedQuestions.stream()
                            .map(q -> createQuestion(userId, knowledgePoint, q, now))
                            .map(this::toDetail)
                            .toList();

                    sendSse(emitter, "result", result);
                    emitter.complete();
                },
                error -> {
                    sendSse(emitter, "error", error.getMessage());
                    emitter.completeWithError(error);
                });
    }

    @Override
    public void delete(Long userId, Long questionId) {
        Question question = requireOwnedQuestion(userId, questionId);
        questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, question.getId()));
        questionMapper.deleteById(question.getId());
    }

    private Question createQuestion(
            Long userId,
            KnowledgePoint knowledgePoint,
            AiService.GeneratedQuestion generatedQuestion,
            LocalDateTime now) {
        Question question = new Question();
        question.setUserId(userId);
        question.setNoteId(knowledgePoint.getNoteId());
        question.setKnowledgeId(knowledgePoint.getId());
        question.setQuestionType(trimToDefault(generatedQuestion.questionType(), "short_answer"));
        question.setContent(trimToDefault(generatedQuestion.content(), "Please explain " + knowledgePoint.getTitle()));
        question.setStandardAnswer(trimToNull(generatedQuestion.standardAnswer()));
        question.setAnalysis(trimToNull(generatedQuestion.analysis()));
        question.setDifficulty(trimToDefault(generatedQuestion.difficulty(), "medium"));
        question.setCreateTime(now);
        question.setUpdateTime(now);
        question.setDeleted(0);
        questionMapper.insert(question);

        List<AiService.GeneratedQuestionOption> options = generatedQuestion.options();
        if (options != null) {
            options.forEach(option -> createOption(question.getId(), option));
        }
        return question;
    }

    private void createOption(Long questionId, AiService.GeneratedQuestionOption generatedOption) {
        QuestionOption option = new QuestionOption();
        option.setQuestionId(questionId);
        option.setOptionKey(trimToDefault(generatedOption.optionKey(), "A"));
        option.setOptionContent(trimToDefault(generatedOption.optionContent(), ""));
        option.setCorrect(generatedOption.correct() ? 1 : 0);
        questionOptionMapper.insert(option);
    }

    private void deleteQuestionsByKnowledge(Long userId, Long knowledgeId) {
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getUserId, userId)
                .eq(Question::getKnowledgeId, knowledgeId));
        questions.forEach(question -> questionOptionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, question.getId())));
        questionMapper.delete(new LambdaQueryWrapper<Question>()
                .eq(Question::getUserId, userId)
                .eq(Question::getKnowledgeId, knowledgeId));
    }

    private KnowledgePoint requireOwnedKnowledge(Long userId, Long knowledgeId) {
        KnowledgePoint knowledgePoint = knowledgePointMapper.selectOne(new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getId, knowledgeId)
                .eq(KnowledgePoint::getUserId, userId)
                .last("LIMIT 1"));
        if (knowledgePoint == null) {
            throw new BusinessException(404, "knowledge point not found");
        }
        return knowledgePoint;
    }

    private Question requireOwnedQuestion(Long userId, Long questionId) {
        Question question = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getId, questionId)
                .eq(Question::getUserId, userId)
                .last("LIMIT 1"));
        if (question == null) {
            throw new BusinessException(404, "question not found");
        }
        return question;
    }

    private QuestionListItemResponse toListItem(Question question) {
        return QuestionListItemResponse.builder()
                .id(question.getId())
                .noteId(question.getNoteId())
                .knowledgeId(question.getKnowledgeId())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .difficulty(question.getDifficulty())
                .createTime(question.getCreateTime())
                .build();
    }

    private QuestionDetailResponse toDetail(Question question) {
        return QuestionDetailResponse.builder()
                .id(question.getId())
                .noteId(question.getNoteId())
                .knowledgeId(question.getKnowledgeId())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .standardAnswer(question.getStandardAnswer())
                .analysis(question.getAnalysis())
                .difficulty(question.getDifficulty())
                .options(listOptions(question.getId()))
                .createTime(question.getCreateTime())
                .updateTime(question.getUpdateTime())
                .build();
    }

    private List<QuestionOptionResponse> listOptions(Long questionId) {
        return questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, questionId)
                        .orderByAsc(QuestionOption::getOptionKey))
                .stream()
                .map(option -> QuestionOptionResponse.builder()
                        .id(option.getId())
                        .optionKey(option.getOptionKey())
                        .optionContent(option.getOptionContent())
                        .correct(option.getCorrect() != null && option.getCorrect() == 1)
                        .build())
                .toList();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private void sendSse(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
