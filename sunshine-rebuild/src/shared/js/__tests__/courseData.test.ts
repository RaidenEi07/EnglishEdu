import { describe, it, expect } from 'vitest';
import { courseData } from '../courseData.ts';

describe('courseData', () => {
  it('contains exactly 24 courses', () => {
    expect(courseData).toHaveLength(24);
  });

  it('every course has id, name, category, and level', () => {
    for (const course of courseData) {
      expect(course.id).toBeTypeOf('number');
      expect(course.name).toBeTypeOf('string');
      expect(course.name.length).toBeGreaterThan(0);
      expect(course.category).toBeTypeOf('string');
      expect(course.level).toBeTypeOf('string');
    }
  });

  it('contains IELTS and CAMBRIDGE categories', () => {
    const categories = new Set(courseData.map((c) => c.category));
    expect(categories.has('IELTS')).toBe(true);
    expect(categories.has('CAMBRIDGE')).toBe(true);
  });

  it('all IDs are unique', () => {
    const ids = courseData.map((c) => c.id);
    const unique = new Set(ids);
    expect(unique.size).toBe(ids.length);
  });

  it('has exactly 2 distinct categories', () => {
    const categories = new Set(courseData.map((c) => c.category));
    expect(categories.size).toBe(2);
  });
});
