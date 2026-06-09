package com.itheima.smartcodenote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.smartcodenote.common.PageResponse;
import com.itheima.smartcodenote.dto.PracticeOptionResponse;
import com.itheima.smartcodenote.dto.PracticeQuestionResponse;
import com.itheima.smartcodenote.dto.WrongQuestionDetailResponse;
import com.itheima.smartcodenote.dto.WrongQuestionListItemResponse;
import com.itheima.smartcodenote.dto.WrongQuestionQueryRequest;
import com.itheima.smartcodenote.entity.Question;
import com.itheima.smartcodenote.entity.QuestionOption;
import com.itheima.smartcodenote.entity.WrongQuestion;
import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.mapper.QuestionMapper;
import com.itheima.smartcodenote.mapper.QuestionOptionMapper;
import com.itheima.smartcodenote.mapper.WrongQuestionMapper;
import com.itheima.smartcodenote.service.WrongQuestionService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;

    @Override
    public PageResponse<WrongQuestionListItemResponse> list(Long userId, WrongQuestionQueryRequest request) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .eq(request.getQuestionId() != null, WrongQuestion::getQuestionId, request.getQuestionId())
                .eq(request.getMastered() != null, WrongQuestion::getMastered, request.getMastered())
                .orderByDesc(WrongQuestion::getLastWrongTime);
        Page<WrongQuestion> page = wrongQuestionMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                wrapper);
        return PageResponse.of(
                page.getRecords().stream().map(this::toListItem).toList(),
                page.getTotal(),
                request.getPageNum(),
                request.getPageSize());
    }

    @Override
    public PracticeQuestionResponse retry(Long userId, Long wrongQuestionId) {
        WrongQuestion wrongQuestion = requireOwnedWrongQuestion(userId, wrongQuestionId);
        return toPracticeQuestion(requireQuestion(wrongQuestion.getQuestionId()));
    }

    @Override
    public WrongQuestionDetailResponse markMastered(Long userId, Long wrongQuestionId) {
        WrongQuestion wrongQuestion = requireOwnedWrongQuestion(userId, wrongQuestionId);
        wrongQuestion.setMastered(1);
        wrongQuestion.setUpdateTime(LocalDateTime.now());
        wrongQuestionMapper.updateById(wrongQuestion);
        return toDetail(wrongQuestion);
    }

    private WrongQuestionListItemResponse toListItem(WrongQuestion wrongQuestion) {
        Question question = requireQuestion(wrongQuestion.getQuestionId());
        return WrongQuestionListItemResponse.builder()
                .id(wrongQuestion.getId())
                .questionId(wrongQuestion.getQuestionId())
                .noteId(question.getNoteId())
                .knowledgeId(question.getKnowledgeId())
                .questionType(question.getQuestionType())
                .content(question.getContent())
                .difficulty(question.getDifficulty())
                .wrongCount(wrongQuestion.getWrongCount())
                .mastered(wrongQuestion.getMastered() != null && wrongQuestion.getMastered() == 1)
                .lastWrongTime(wrongQuestion.getLastWrongTime())
                .createTime(wrongQuestion.getCreateTime())
                .build();
    }

    private WrongQuestionDetailResponse toDetail(WrongQuestion wrongQuestion) {
        return WrongQuestionDetailResponse.builder()
                .id(wrongQuestion.getId())
                .questionId(wrongQuestion.getQuestionId())
                .wrongCount(wrongQuestion.getWrongCount())
                .mastered(wrongQuestion.getMastered() != null && wrongQuestion.getMastered() == 1)
                .lastWrongTime(wrongQuestion.getLastWrongTime())
                .question(toPracticeQuestion(requireQuestion(wrongQuestion.getQuestionId())))
                .build();
    }

    private WrongQuestion requireOwnedWrongQuestion(Long userId, Long wrongQuestionId) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectOne(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getId, wrongQuestionId)
                .eq(WrongQuestion::getUserId, userId)
                .last("LIMIT 1"));
        if (wrongQuestion == null) {
            throw new BusinessException(404, "wrong question not found");
        }
        return wrongQuestion;
    }

    private Question requireQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
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
}
