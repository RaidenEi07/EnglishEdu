-- ============================================================
-- V19: Enrich priority courses with detailed content & modules
--   1) IELTS Test Preparation Platform (external_id = 83)
--   2) UPPER - IELTS (external_id = 27)
--   3) Boost your Listening - A2 (external_id = 71)
-- ============================================================

-- ─── 1. Update course descriptions & images ──────────────────────────────────

UPDATE courses SET
    description = E'Nền tảng luyện thi IELTS toàn diện dành cho mọi trình độ. Khóa học bao gồm đầy đủ 4 kỹ năng Listening, Reading, Writing và Speaking với hệ thống bài thi thử mô phỏng thực tế. Học viên được truy cập ngân hàng đề thi liên tục cập nhật, hệ thống chấm điểm tự động và phân tích chi tiết điểm yếu cần cải thiện.\n\n✅ Hệ thống Mock Test mô phỏng thi thật\n✅ Phân tích chi tiết band điểm từng kỹ năng\n✅ Ngân hàng đề thi cập nhật liên tục\n✅ Hỗ trợ luyện tập không giới hạn',
    image_url = '/images/courses/ielts-test-prep.jpg',
    level = 'All Levels'
WHERE external_id = 83;

UPDATE courses SET
    description = E'Khóa học IELTS dành cho học viên trình độ Upper-Intermediate (B2). Tập trung nâng cao kỹ năng academic với chiến lược làm bài hiệu quả cho từng dạng câu hỏi. Lộ trình học được cá nhân hóa giúp học viên đạt band 6.5–7.5.\n\n📚 Listening: Luyện nghe các bài giảng học thuật, hội thoại phức tạp\n📖 Reading: Chiến lược đọc nhanh, phân tích passage academic\n✍️ Writing: Task 1 (biểu đồ, bảng, quy trình) & Task 2 (essay academic)\n🎤 Speaking: Part 2 & 3 nâng cao với chủ đề đa dạng',
    image_url = '/images/courses/upper-ielts.png',
    level = 'Upper-Intermediate'
WHERE external_id = 27;

UPDATE courses SET
    description = E'Khóa học chuyên sâu kỹ năng Listening dành cho trình độ A2 (Pre-Intermediate). Giúp học viên xây dựng nền tảng nghe tiếng Anh vững chắc thông qua các bài tập đa dạng từ cuộc sống hàng ngày.\n\n🎧 Nghe hiểu hội thoại đời thường\n🎧 Nghe và điền thông tin quan trọng\n🎧 Nghe hiểu chỉ dẫn, hướng dẫn đơn giản\n🎧 Luyện phát âm và nhận diện âm thanh\n🎧 Bài tập tương tác với audio thực tế',
    image_url = '/images/courses/boost-listening-a2.jpg',
    level = 'A2'
WHERE external_id = 71;

-- Also update Reading A2 course
UPDATE courses SET
    description = E'Khóa học nâng cao kỹ năng Vocabulary và Reading cho trình độ A2. Mở rộng vốn từ vựng theo chủ đề và phát triển khả năng đọc hiểu thông qua các bài đọc phong phú.\n\n📖 Mở rộng từ vựng theo 12 chủ đề thông dụng\n📖 Đọc hiểu đoạn văn ngắn và trung bình\n📖 Kỹ thuật đoán nghĩa từ ngữ cảnh\n📖 Bài tập matching, true/false, fill-in-the-blank',
    image_url = '/images/courses/boost-reading-a2.jpg',
    level = 'A2'
WHERE external_id = 70;


-- ─── 2. Create course modules for IELTS Test Preparation Platform ────────────

INSERT INTO course_modules (course_id, title, description, sort_order) VALUES
((SELECT id FROM courses WHERE external_id = 83), 'Module 1: Listening Practice Tests',
 'Bộ đề luyện thi Listening với các dạng câu hỏi thường gặp trong IELTS: Multiple Choice, Map Labelling, Note Completion, Summary Completion.', 1),

((SELECT id FROM courses WHERE external_id = 83), 'Module 2: Reading Practice Tests',
 'Bộ đề luyện thi Reading với các passage academic và general. Rèn luyện kỹ năng skimming, scanning và phân tích logic.', 2),

((SELECT id FROM courses WHERE external_id = 83), 'Module 3: Writing Task 1 & Task 2',
 'Hướng dẫn chi tiết cách viết Task 1 (biểu đồ, bảng, quy trình) và Task 2 (argumentative essay, discussion essay).', 3),

((SELECT id FROM courses WHERE external_id = 83), 'Module 4: Speaking Mock Tests',
 'Luyện thi Speaking với bộ câu hỏi Part 1, 2, 3 cập nhật. Kỹ thuật trả lời và mở rộng ý.', 4),

((SELECT id FROM courses WHERE external_id = 83), 'Module 5: Full Mock Tests',
 'Bài thi thử IELTS hoàn chỉnh 4 kỹ năng. Tính giờ thực tế, chấm điểm band tự động.', 5);


-- ─── 3. Create modules for UPPER - IELTS ────────────────────────────────────

INSERT INTO course_modules (course_id, title, description, sort_order) VALUES
((SELECT id FROM courses WHERE external_id = 27), 'Unit 1: Advanced Listening Skills',
 'Nghe hiểu bài giảng học thuật, monologue dài, đề tài khoa học và xã hội. Dạng bài: sentence completion, multiple choice, matching.', 1),

((SELECT id FROM courses WHERE external_id = 27), 'Unit 2: Academic Reading Strategies',
 'Chiến lược đọc passage dài 800+ từ. Kỹ thuật: skimming, scanning, paraphrasing. Dạng bài: Yes/No/Not Given, Matching Headings, Summary.', 2),

((SELECT id FROM courses WHERE external_id = 27), 'Unit 3: Writing Task 1 - Data Description',
 'Mô tả biểu đồ đường, cột, tròn, bảng và quy trình. Cấu trúc bài viết, từ vựng so sánh, xu hướng.', 3),

((SELECT id FROM courses WHERE external_id = 27), 'Unit 4: Writing Task 2 - Essay Types',
 'Các dạng essay: Opinion, Discussion, Problem-Solution, Two-part question. Cấu trúc 4 đoạn, lập luận logic.', 4),

((SELECT id FROM courses WHERE external_id = 27), 'Unit 5: Speaking Part 2 & 3',
 'Long turn speaking (2 phút) và thảo luận nâng cao. Chiến lược brainstorm nhanh, phát triển ý, dùng linking words.', 5),

((SELECT id FROM courses WHERE external_id = 27), 'Unit 6: Vocabulary & Grammar for Band 7+',
 'Từ vựng academic theo chủ đề. Cấu trúc ngữ pháp nâng cao: inversion, cleft sentences, participle clauses.', 6),

((SELECT id FROM courses WHERE external_id = 27), 'Unit 7: Full Practice Tests',
 'Bộ đề thi thử đầy đủ cấp độ Upper-Intermediate. Phân tích kết quả chi tiết cho từng kỹ năng.', 7);


-- ─── 4. Create modules for Boost your Listening - A2 ────────────────────────

INSERT INTO course_modules (course_id, title, description, sort_order) VALUES
((SELECT id FROM courses WHERE external_id = 71), 'Topic 1: Daily Conversations',
 'Nghe hiểu các cuộc hội thoại đời thường: chào hỏi, giới thiệu bản thân, hỏi đường, mua sắm.', 1),

((SELECT id FROM courses WHERE external_id = 71), 'Topic 2: At School & Work',
 'Nghe hiểu trong môi trường học đường và công việc: lịch học, thông báo, hướng dẫn đơn giản.', 2),

((SELECT id FROM courses WHERE external_id = 71), 'Topic 3: Travel & Transportation',
 'Nghe hiểu thông báo tại sân bay, ga tàu, đặt vé, hỏi lịch trình di chuyển.', 3),

((SELECT id FROM courses WHERE external_id = 71), 'Topic 4: Food & Health',
 'Nghe hiểu cuộc trò chuyện về ẩm thực, đặt món, tư vấn sức khỏe đơn giản.', 4),

((SELECT id FROM courses WHERE external_id = 71), 'Topic 5: Entertainment & Hobbies',
 'Nghe hiểu về phim ảnh, âm nhạc, thể thao, sở thích – đề tài quen thuộc trong đời sống.', 5),

((SELECT id FROM courses WHERE external_id = 71), 'Topic 6: Listening Comprehension Tests',
 'Bài kiểm tra tổng hợp cuối khóa. Đánh giá khả năng nghe hiểu toàn diện cấp A2.', 6);


-- ─── 5. Create lessons for IELTS Test Preparation (Module 1 sample) ─────────

INSERT INTO lessons (module_id, title, content, type, duration, sort_order, is_free) VALUES
((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 83) AND sort_order = 1),
 'Listening Test 1: Section 1 - Conversation', 'Bài nghe Section 1: Cuộc hội thoại giữa 2 người trong tình huống đời thường. Dạng bài: Form Completion.', 'DOCUMENT', 30, 1, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 83) AND sort_order = 1),
 'Listening Test 1: Section 2 - Monologue', 'Bài nghe Section 2: Một người nói về chủ đề xã hội. Dạng bài: Multiple Choice, Map Labelling.', 'DOCUMENT', 30, 2, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 83) AND sort_order = 1),
 'Listening Test 1: Section 3 - Academic Discussion', 'Bài nghe Section 3: Thảo luận học thuật giữa 2-3 người. Dạng bài: Matching, Note Completion.', 'DOCUMENT', 30, 3, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 83) AND sort_order = 1),
 'Listening Test 1: Section 4 - Lecture', 'Bài nghe Section 4: Bài giảng học thuật. Dạng bài: Summary Completion, Sentence Completion.', 'DOCUMENT', 30, 4, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 83) AND sort_order = 5),
 'Full Mock Test 1', 'Bài thi thử IELTS đầy đủ 4 kỹ năng. Listening 40 phút, Reading 60 phút, Writing 60 phút.', 'DOCUMENT', 180, 1, FALSE);


-- ─── 6. Create lessons for UPPER - IELTS (Unit 1 & 2 samples) ──────────────

INSERT INTO lessons (module_id, title, content, type, duration, sort_order, is_free) VALUES
((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 27) AND sort_order = 1),
 'Lecture Listening: Science & Technology', 'Nghe bài giảng về công nghệ mới. Luyện note-taking và sentence completion.', 'DOCUMENT', 25, 1, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 27) AND sort_order = 1),
 'Academic Discussion: Environment', 'Nghe thảo luận về biến đổi khí hậu. Dạng bài matching speakers to opinions.', 'DOCUMENT', 25, 2, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 27) AND sort_order = 2),
 'Reading: Coral Reef Ecosystems', 'Passage academic 900 từ. Dạng bài: Yes/No/Not Given, Summary Completion.', 'DOCUMENT', 40, 1, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 27) AND sort_order = 2),
 'Reading: History of Urban Planning', 'Passage 1000 từ. Dạng bài: Matching Headings, Multiple Choice, Short Answer.', 'DOCUMENT', 40, 2, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 27) AND sort_order = 6),
 'Academic Vocabulary: Topic - Education', 'Từ vựng chủ đề giáo dục: curriculum, assessment, pedagogy, discipline...', 'TEXT', 20, 1, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 27) AND sort_order = 6),
 'Grammar Focus: Inversion & Cleft Sentences', 'Đảo ngữ nâng cao: Not only...but also, Never before, Hardly...when. Câu chẻ: It is...that.', 'TEXT', 25, 2, FALSE);


-- ─── 7. Create lessons for Boost Listening A2 (Topic 1 & 2 samples) ─────────

INSERT INTO lessons (module_id, title, content, type, duration, sort_order, is_free) VALUES
((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 71) AND sort_order = 1),
 'Greeting & Introduction', 'Nghe và thực hành các cách chào hỏi, giới thiệu bản thân bằng tiếng Anh. Audio + bài tập điền từ.', 'DOCUMENT', 15, 1, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 71) AND sort_order = 1),
 'Asking for Directions', 'Nghe người bản ngữ hỏi và chỉ đường. Từ vựng: turn left/right, go straight, next to, opposite.', 'DOCUMENT', 15, 2, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 71) AND sort_order = 1),
 'Shopping Conversations', 'Nghe hội thoại mua sắm: hỏi giá, size, màu sắc, thanh toán. Bài tập: chọn đáp án đúng.', 'DOCUMENT', 15, 3, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 71) AND sort_order = 2),
 'School Announcements', 'Nghe thông báo trường học: lịch thi, sự kiện, thay đổi lịch học. Bài tập note-taking.', 'DOCUMENT', 15, 1, TRUE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 71) AND sort_order = 2),
 'Simple Instructions at Work', 'Nghe hướng dẫn công việc đơn giản: cách sử dụng máy, quy trình, nội quy.', 'DOCUMENT', 15, 2, FALSE),

((SELECT id FROM course_modules WHERE course_id = (SELECT id FROM courses WHERE external_id = 71) AND sort_order = 6),
 'Final Listening Test - A2', 'Bài kiểm tra tổng hợp: 30 câu, 25 phút. Đánh giá năng lực nghe hiểu cấp A2.', 'DOCUMENT', 25, 1, FALSE);
