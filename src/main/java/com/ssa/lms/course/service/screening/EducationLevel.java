package com.ssa.lms.course.service.screening;

/**
 * 학력 배점 — 편람 기준 "석사이상(10) / 초대졸이상(9) / 고졸이하(8)".
 *
 * <p>{@code code} 는 회원가입 폼(signup-trainee.html)의 최종학력 select value 와 같다.
 * 4년제 재학은 아직 초대졸 이상이 아니므로 고졸이하(8)로 본다.</p>
 */
public enum EducationLevel {

    GRADUATE("graduate_graduate", "대학원 졸업", "석사이상", 10),
    UNIVERSITY("university_graduate", "4년제 대학 졸업", "초대졸이상", 9),
    JUNIOR_COLLEGE("junior_college_graduate", "전문대 졸업", "초대졸이상", 9),
    UNIVERSITY_ENROLLED("university_enrolled", "4년제 대학 재학", "고졸이하", 8),
    HIGH_SCHOOL("highschool", "고등학교 졸업", "고졸이하", 8);

    private final String code;
    private final String label;
    /** 편람의 평가 구분 (석사이상/초대졸이상/고졸이하) */
    private final String tier;
    private final int point;

    EducationLevel(String code, String label, String tier, int point) {
        this.code = code;
        this.label = label;
        this.tier = tier;
        this.point = point;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getTier() {
        return tier;
    }

    public int getPoint() {
        return point;
    }

    /** 가입 폼 코드값 → 학력. 미입력·미상은 고졸이하로 본다. */
    public static EducationLevel of(String code) {
        for (EducationLevel e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return HIGH_SCHOOL;
    }
}
