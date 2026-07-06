package com.smartcodenote.service;

import com.smartcodenote.dto.AnswerResultResponse;
import com.smartcodenote.dto.PracticeQuestionResponse;
import com.smartcodenote.dto.StartPracticeRequest;
import com.smartcodenote.dto.SubmitAnswerRequest;
import java.util.List;

public interface PracticeService {

    List<PracticeQuestionResponse> start(Long userId, StartPracticeRequest request);

    AnswerResultResponse submit(Long userId, SubmitAnswerRequest request);
}
