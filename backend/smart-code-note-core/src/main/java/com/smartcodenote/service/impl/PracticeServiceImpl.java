package com.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcodenote.ai.AiService;
import com.smartcodenote.dto.AnswerResultResponse;
import com.smartcodenote.dto.PracticeOptionResponse;
import com.smartcodenote.dto.PracticeQuestionResponse;
import com.smartcodenote.dto.StartPracticeRequest;
import com.smartcodenote.dto.SubmitAnswerRequest;
import com.smartcodenote.entity.AnswerRecord;
import com.smartcodenote.entity.Question;
import com.smartcodenote.entity.QuestionOption;
import com.smartcodenote.entity.WrongQuestion;
import com.smartcodenote.exception.BusinessException;
import com.smartcodenote.mapper.AnswerRecordMapper;
import com.smartcodenote.mapper.QuestionMapper;
import com.smartcodenote.mapper.QuestionOptionMapper;
import com.smartcodenote.mapper.WrongQuestionMapper;
import com.smartcodenote.service.PracticeService;
import com.smartcodenote.service.ReviewPlanService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private static final int PASS_SCORE = 60;

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewPlanService reviewPlanService;
    private final AiService aiService;

    @Override
    public List<PracticeQuestionResponse> start(Long userId, StartPracticeRequest request) {
        int count = request.getCount() == null ? 10 : request.getCount();
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getUserId, userId)
                .eq(request.getNoteId() != null, Question::getNoteId, request.getNoteId())
                .eq(request.getKnowledgeId() != null, Question::getKnowledgeId, request.getKnowledgeId())
                .eq(StringUtils.hasText(request.getQuestionType()), Question::getQuestionType, request.getQuestionType())
                .orderByDesc(Question::getCreateTime)
                .last("LIMIT " + count);
        return questionMapper.selectList(wrapper).stream()
                .map(this::toPracticeQuestion)
                .toList();
    }

    @Override
    public AnswerResultResponse submit(Long userId, SubmitAnswerRequest request) {
        Question question = requireOwnedQuestion(userId, request.getQuestionId());
        String userAnswer = request.getAnswer().trim();
        ScoreResult scoreResult = score(question, userAnswer);

        AnswerRecord record = new AnswerRecord();
        record.setUserId(userId);
        record.setQuestionId(question.getId());
        record.setUserAnswer(userAnswer);
        record.setScore(scoreResult.score());
        record.setCorrect(scoreResult.correct() ? 1 : 0);
        record.setAiComment(scoreResult.comment());
        record.setCreateTime(LocalDateTime.now());
        answerRecordMapper.insert(record);
        if (!scoreResult.correct()) {
            saveWrongQuestion(userId, question.getId(), record.getCreateTime());
        }
        reviewPlanService.recordAnswerResult(
                userId,
                question.getKnowledgeId(),
                question.getId(),
                record.getScore(),
                record.getCorrect() == 1);

        return AnswerResultResponse.builder()
                .recordId(record.getId())
                .questionId(question.getId())
                .questionType(question.getQuestionType())
                .userAnswer(userAnswer)
                .standardAnswer(question.getStandardAnswer())
                .score(record.getScore())
                .correct(record.getCorrect() == 1)
                .aiComment(record.getAiComment())
                .createTime(record.getCreateTime())
                .build();
    }

    private ScoreResult score(Question question, String userAnswer) {
        String questionType = normalize(question.getQuestionType());
        if ("single_choice".equals(questionType) || "multiple_choice".equals(questionType) || "judgement".equals(questionType)) {
            String correctAnswer = buildCorrectAnswer(question);
            boolean correct = normalizeAnswer(userAnswer).equals(normalizeAnswer(correctAnswer));
            return new ScoreResult(correct ? 100 : 0, correct, correct ? "Correct answer." : "Incorrect answer.");
        }

        AiService.AiScore aiScore = aiService.scoreSubjectiveAnswer(
                question.getContent(),
                question.getStandardAnswer(),
                userAnswer);
        boolean correct = aiScore.score() >= PASS_SCORE;
        return new ScoreResult(aiScore.score(), correct, aiScore.comment());
    }

    private void saveWrongQuestion(Long userId, Long questionId, LocalDateTime now) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(WrongQuestion::getQuestionId, questionId)
                .last("LIMIT 1"));
        if (wrongQuestion == null) {
            wrongQuestion = new WrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setWrongCount(1);
            wrongQuestion.setLastWrongTime(now);
            wrongQuestion.setMastered(0);
            wrongQuestion.setCreateTime(now);
            wrongQuestion.setUpdateTime(now);
            wrongQuestion.setDeleted(0);
            wrongQuestionMapper.insert(wrongQuestion);
            return;
        }
        wrongQuestion.setWrongCount((wrongQuestion.getWrongCount() == null ? 0 : wrongQuestion.getWrongCount()) + 1);
        wrongQuestion.setLastWrongTime(now);
        wrongQuestion.setMastered(0);
        wrongQuestion.setUpdateTime(now);
        wrongQuestionMapper.updateById(wrongQuestion);
    }

    private String buildCorrectAnswer(Question question) {
        List<QuestionOption> correctOptions = questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, question.getId()))
                .stream()
                .filter(option -> option.getCorrect() != null && option.getCorrect() == 1)
                .sorted(Comparator.comparing(QuestionOption::getOptionKey))
                .toList();
        if (correctOptions.isEmpty()) {
            return question.getStandardAnswer();
        }
        return String.join(",", correctOptions.stream().map(QuestionOption::getOptionKey).toList());
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

    private PracticeQuestionResponse toPracticeQuestion(Question question) {
        return PracticeQuestionResponse.builder()
                .id(question.getId())
                .noteId(question.getNoteId())
                .knowledgeId(question.getKnowledgeId())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .difficulty(question.getDifficulty())
                .options(listOptions(question.getId()))
                .build();
    }

    private List<PracticeOptionResponse> listOptions(Long questionId) {
        return questionOptionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, questionId)
                        .orderByAsc(QuestionOption::getOptionKey))
                .stream()
                .map(option -> PracticeOptionResponse.builder()
                        .id(option.getId())
                        .optionKey(option.getOptionKey())
                        .optionContent(option.getOptionContent())
                        .build())
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAnswer(String answer) {
        return normalize(answer).replaceAll("\\s+", "");
    }

    private record ScoreResult(int score, boolean correct, String comment) {
    }
}
