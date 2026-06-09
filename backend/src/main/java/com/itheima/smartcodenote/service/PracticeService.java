package com.itheima.smartcodenote.service;

import com.itheima.smartcodenote.dto.AnswerResultResponse;
import com.itheima.smartcodenote.dto.PracticeQuestionResponse;
import com.itheima.smartcodenote.dto.StartPracticeRequest;
import com.itheima.smartcodenote.dto.SubmitAnswerRequest;
import java.util.List;

public interface PracticeService {

    List<PracticeQuestionResponse> start(Long userId, StartPracticeRequest request);

    AnswerResultResponse submit(Long userId, SubmitAnswerRequest request);
}
