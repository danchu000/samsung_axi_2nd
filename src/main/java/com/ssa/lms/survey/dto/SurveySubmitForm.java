package com.ssa.lms.survey.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 훈련생 설문 제출 폼.
 *
 * 화면은 answers[i].questionId / answers[i].choiceIds / answers[i].scaleValue /
 * answers[i].answerText 로 보낸다.
 *
 * MULTI(복수 선택)는 choiceIds 에 여러 개가 담기고, 저장 시 선택한 보기 수만큼
 * SurveyAnswer 행이 만들어진다 — 유니크 제약이 (response, question, choice) 인 이유다.
 */
@Getter
@Setter
public class SurveySubmitForm {

    private List<AnswerInput> answers = new ArrayList<>();

    @Getter
    @Setter
    public static class AnswerInput {
        private Long questionId;
        private List<Long> choiceIds = new ArrayList<>();
        private Integer scaleValue;
        private String answerText;

        public List<Long> cleanChoiceIds() {
            List<Long> result = new ArrayList<>();
            if (choiceIds == null) {
                return result;
            }
            for (Long id : choiceIds) {
                if (id != null && !result.contains(id)) {
                    result.add(id);
                }
            }
            return result;
        }

        public boolean hasText() {
            return answerText != null && !answerText.isBlank();
        }
    }
}
