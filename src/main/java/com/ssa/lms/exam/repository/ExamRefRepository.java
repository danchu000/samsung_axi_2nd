package com.ssa.lms.exam.repository;

import com.ssa.lms.course.entity.Course;
import com.ssa.lms.course.entity.Session;
import com.ssa.lms.course.entity.Subject;
import com.ssa.lms.exam.entity.Exam;
import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 시험이 참조하는 A 소유 엔티티(Course/Subject/Session/User)의 <b>읽기 전용</b> 조회.
 *
 * A 의 리포지토리(CourseRepository 등)를 직접 의존하지 않으려고 B 패키지 안에 따로 뒀다.
 * (CLAUDE.md: 상대 도메인 코드는 수정 금지 / 필요한 조회는 계약으로 요청)
 * 수강생 명단처럼 이미 계약이 있는 조회는 {@code CourseQueryService} 를 쓴다.
 *
 * 여기에 쓰기 메서드를 추가하지 말 것 — A 도메인의 상태를 B 가 바꾸면 안 된다.
 */
public interface ExamRefRepository extends Repository<Exam, Long> {

    @Query("select c from Course c order by c.courseName asc")
    List<Course> findAllCourses();

    @Query("select c from Course c where c.id = :id")
    Optional<Course> findCourse(@Param("id") Long id);

    @Query("select s from Subject s where s.course.id = :courseId order by s.orderNo asc")
    List<Subject> findSubjectsByCourse(@Param("courseId") Long courseId);

    @Query("select s from Subject s where s.id = :id")
    Optional<Subject> findSubject(@Param("id") Long id);

    @Query("select s from Session s where s.subject.course.id = :courseId order by s.seq asc")
    List<Session> findSessionsByCourse(@Param("courseId") Long courseId);

    @Query("select s from Session s where s.id = :id")
    Optional<Session> findSession(@Param("id") Long id);

    @Query("select u from User u where u.role = :role order by u.name asc")
    List<User> findUsersByRole(@Param("role") Role role);

    @Query("select u from User u where u.id = :id")
    Optional<User> findUser(@Param("id") Long id);
}
