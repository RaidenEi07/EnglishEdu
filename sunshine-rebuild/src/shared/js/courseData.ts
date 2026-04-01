/**
 * Course data - All 24 courses with metadata
 */
import type { CourseStaticData } from './types.ts';

export const courseData: CourseStaticData[] = [
  // IELTS Courses
  { id: 83, name: 'IELTS Test Preparation Platform',    category: 'IELTS', level: 'All Levels' },
  { id: 84, name: 'IELTS PREP',                         category: 'IELTS', level: 'Beginner' },
  { id: 40, name: 'PRE - INTERMEDIATE',                 category: 'IELTS', level: 'Pre-Intermediate' },
  { id: 68, name: 'PRE - UPPER IELTS',                  category: 'IELTS', level: 'Pre-Upper' },
  { id: 39, name: 'INTER - IELTS',                      category: 'IELTS', level: 'Intermediate' },
  { id: 27, name: 'UPPER - IELTS',                      category: 'IELTS', level: 'Upper-Intermediate' },
  { id: 30, name: 'ADVANCED - IELTS',                   category: 'IELTS', level: 'Advanced' },
  { id: 33, name: 'INTENSIVE - IELTS',                  category: 'IELTS', level: 'Intensive' },
  { id: 42, name: 'IELTS - INTENSIVE SPEAKING',         category: 'IELTS', level: 'Intensive' },
  { id: 41, name: 'IELTS Mocktest - 30 REAL Tests',     category: 'IELTS', level: 'All Levels' },
  { id: 76, name: 'TIC - MOCKTESTS',                    category: 'IELTS', level: 'All Levels' },
  { id: 81, name: 'Joint IELTS Assessment Portal',      category: 'IELTS', level: 'All Levels' },
  { id: 82, name: 'Schedule - Session Monitoring',      category: 'IELTS', level: 'Internal' },
  { id: 74, name: 'Internal Test',                      category: 'IELTS', level: 'Internal' },

  // CAMBRIDGE Courses
  { id: 25, name: 'PREPARE 2 - CAMBRIDGE A2',                       category: 'CAMBRIDGE', level: 'A2' },
  { id: 29, name: 'PREPARE 3 - CAMBRIDGE A2',                       category: 'CAMBRIDGE', level: 'A2' },
  { id: 38, name: 'PREPARE 4 - CAMBRIDGE B1',                       category: 'CAMBRIDGE', level: 'B1' },
  { id: 66, name: 'PREPARE 5 - CAMBRIDGE B1',                       category: 'CAMBRIDGE', level: 'B1' },
  { id: 70, name: 'Boost your Vocabulary and Reading - A2',         category: 'CAMBRIDGE', level: 'A2' },
  { id: 71, name: 'Boost your Listening - A2',                      category: 'CAMBRIDGE', level: 'A2' },
  { id: 67, name: 'Boost your Vocabulary and Reading - B1',         category: 'CAMBRIDGE', level: 'B1' },
  { id: 69, name: 'Boost your Listening - B1',                      category: 'CAMBRIDGE', level: 'B1' },
  { id: 72, name: 'Boost your Vocabulary and Reading - B2',         category: 'CAMBRIDGE', level: 'B2' },
  { id: 73, name: 'Boost your Listening - B2',                      category: 'CAMBRIDGE', level: 'B2' },
];

