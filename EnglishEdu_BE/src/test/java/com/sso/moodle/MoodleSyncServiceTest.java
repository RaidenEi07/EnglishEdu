package com.sso.moodle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sso.entity.Category;
import com.sso.entity.Course;
import com.sso.repository.CourseRepository;
import com.sso.repository.EnrollmentRepository;
import com.sso.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoodleSyncServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private MoodleClient moodleClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private MoodleProperties moodleProperties;
    private MoodleSyncService moodleSyncService;

    @BeforeEach
    void setUp() {
        moodleProperties = new MoodleProperties();
        moodleSyncService = new MoodleSyncService(
                moodleClient,
                moodleProperties,
                userRepository,
                courseRepository,
                enrollmentRepository
        );
    }

    @Test
    void ensureMoodleCourseUsesMatchingCategoryNameFromMoodle() {
        Course course = Course.builder()
                .id(38L)
                .name("IELTS - PREP")
                .description("Prep course")
                .build();
        course.setCategory("IELTS");

        JsonNode moodleCategories = OBJECT_MAPPER.createArrayNode()
                .add(OBJECT_MAPPER.createObjectNode()
                        .put("id", 3)
                        .put("name", "IELTS"));

        when(moodleClient.getCourseCategories()).thenReturn(moodleCategories);
        when(moodleClient.createCourse(eq("IELTS - PREP"), eq("SSO-38"), eq("3"), eq("Prep course")))
                .thenReturn(91L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long moodleCourseId = moodleSyncService.ensureMoodleCourse(course);

        assertEquals(91L, moodleCourseId);
        assertEquals(91L, course.getMoodleCourseId());
        verify(moodleClient).createCourse("IELTS - PREP", "SSO-38", "3", "Prep course");
    }

    @Test
    void ensureMoodleCourseFallsBackToConfiguredDefaultCategoryId() {
        Course course = Course.builder()
                .id(52L)
                .name("Brand New Course")
                .description("Desc")
                .build();
        Category category = new Category();
        category.setName("New Category");
        course.setCategoryEntity(category);
        moodleProperties.setDefaultCourseCategoryId("6");

        when(moodleClient.getCourseCategories()).thenReturn(OBJECT_MAPPER.createArrayNode());
        when(moodleClient.createCourse(eq("Brand New Course"), eq("SSO-52"), eq("6"), eq("Desc")))
                .thenReturn(123L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long moodleCourseId = moodleSyncService.ensureMoodleCourse(course);

        assertEquals(123L, moodleCourseId);
        verify(moodleClient).createCourse("Brand New Course", "SSO-52", "6", "Desc");
    }

    @Test
    void ensureMoodleCourseSkipsCreationWhenAlreadyLinked() {
        Course course = Course.builder()
                .id(99L)
                .name("Existing")
                .moodleCourseId(777L)
                .build();

        long moodleCourseId = moodleSyncService.ensureMoodleCourse(course);

        assertEquals(777L, moodleCourseId);
                verify(moodleClient, never()).getCourseCategories();
        verify(moodleClient, never()).createCourse(any(), any(), any(), any());
    }
}