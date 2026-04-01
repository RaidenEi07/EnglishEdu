package com.sso.mapper;

import com.sso.dto.response.CourseResponse;
import com.sso.dto.response.CourseTeacherResponse;
import com.sso.dto.response.EnrolledCourseResponse;
import com.sso.entity.Course;
import com.sso.entity.CourseTeacher;
import com.sso.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.username", target = "teacherName")
    @Mapping(source = "categoryEntity.id", target = "categoryId")
    @Mapping(source = "levelEntity.id", target = "levelId")
    @Mapping(source = "courseTeachers", target = "teachers")
    CourseResponse toResponse(Course course);

    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.username", target = "teacherUsername")
    @Mapping(source = "teacher.fullName", target = "teacherFullName")
    CourseTeacherResponse toTeacherResponse(CourseTeacher ct);

    List<CourseTeacherResponse> toTeacherResponseList(List<CourseTeacher> teachers);

    @Mapping(source = "enrollment.id", target = "enrollmentId")
    @Mapping(source = "enrollment.course.id", target = "courseId")
    @Mapping(source = "enrollment.course.externalId", target = "externalId")
    @Mapping(source = "enrollment.course.name", target = "name")
    @Mapping(source = "enrollment.course.category", target = "category")
    @Mapping(source = "enrollment.course.level", target = "level")
    @Mapping(source = "enrollment.course.imageUrl", target = "imageUrl")
    @Mapping(source = "enrollment.progress", target = "progress")
    @Mapping(source = "enrollment.status", target = "status")
    @Mapping(source = "enrollment.enrolledAt", target = "enrolledAt")
    @Mapping(source = "enrollment.lastAccessed", target = "lastAccessed")
    @Mapping(source = "enrollment.starred", target = "starred")
    @Mapping(source = "enrollment.hidden", target = "hidden")
    @Mapping(source = "enrollment.teacherNote", target = "teacherNote")
    EnrolledCourseResponse toEnrolledResponse(Enrollment enrollment);
}
