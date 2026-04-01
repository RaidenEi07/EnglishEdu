-- ============================================================
-- V20: Enrich lesson content with detailed learning material
-- Covers all 17 lessons across 3 seeded courses.
-- ============================================================

-- ── Course 1: IELTS Test Preparation Platform ─────────────────────

-- L1: Listening Section 1 – Conversation / Form Completion
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Listening Section 1: Hội thoại đời thường – Form Completion</h5>

<div class="alert alert-info mb-4">
  <b>Dạng bài:</b> Form Completion &nbsp;|&nbsp; <b>Nhân vật:</b> 2 người (thường 1 nam 1 nữ) &nbsp;|&nbsp; <b>Bối cảnh:</b> Tình huống hàng ngày (đặt phòng, đăng ký, mua vé…)
</div>

<h6 class="fw-semibold">📋 Cấu trúc bài</h6>
<p>Section 1 là phần nghe dễ nhất trong đề IELTS Listening. Bài gồm một đoạn hội thoại giữa 2 người — thường là giao dịch thực tế như đặt phòng khách sạn, đăng ký câu lạc bộ, hoặc hỏi thông tin dịch vụ.</p>
<p>Bạn cần <strong>lắng nghe và điền thông tin</strong> vào các ô trống trong form. Thông tin cần điền thường là: <em>tên, địa chỉ, số điện thoại, ngày tháng, giá tiền, mã số</em>...</p>

<h6 class="fw-semibold mt-4">🎯 Chiến lược làm bài</h6>
<ul>
  <li><b>Đọc trước form</b> trong 30 giây được cấp: xác định loại thông tin cần điền (số hay chữ?)</li>
  <li><b>Chú ý từ khóa:</b> người nói thường dùng từ đồng nghĩa hoặc paraphrase — đừng chờ nghe chính xác từ trong form</li>
  <li><b>Viết sạch và đúng chính tả</b> — lỗi chính tả sẽ bị trừ điểm ngay cả khi nghe đúng</li>
  <li><b>Giới hạn số từ:</b> thường là "NO MORE THAN TWO WORDS AND/OR A NUMBER" — đừng điền dư</li>
  <li>Nếu bỏ lỡ một câu, <b>bỏ qua ngay</b> và chuyển sang câu tiếp theo</li>
</ul>

<h6 class="fw-semibold mt-4">📝 Bài tập thực hành</h6>
<div class="card border-primary mb-3">
  <div class="card-body">
    <p class="mb-2"><b>Tình huống:</b> Một khách hàng gọi điện đặt chỗ tại nhà hàng. Nghe đoạn hội thoại và điền thông tin:</p>
    <table class="table table-sm table-bordered mb-0">
      <tr><td style="width:50%">Tên khách hàng</td><td>__________________</td></tr>
      <tr><td>Số lượng khách</td><td>__________________</td></tr>
      <tr><td>Ngày đặt bàn</td><td>__________________</td></tr>
      <tr><td>Giờ đến</td><td>__________________</td></tr>
      <tr><td>Yêu cầu đặc biệt</td><td>__________________</td></tr>
      <tr><td>Số điện thoại liên hệ</td><td>__________________</td></tr>
    </table>
  </div>
</div>

<h6 class="fw-semibold">💡 Lưu ý thường gặp</h6>
<ul>
  <li>Thông tin số: ngày tháng – viết <em>12th March</em> hay <em>12 March</em> đều được</li>
  <li>Tên riêng: người nói thường đánh vần — chuẩn bị viết nhanh từng chữ cái</li>
  <li>Số điện thoại: có thể đọc từng số hoặc nhóm — hãy ghi ngay từng chữ số</li>
</ul>
$$ WHERE id = 1;

-- L2: Listening Section 2 – Monologue / Multiple Choice & Map
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Listening Section 2: Monologue – Multiple Choice & Map Labelling</h5>

<div class="alert alert-info mb-4">
  <b>Dạng bài:</b> Multiple Choice, Map Labelling &nbsp;|&nbsp; <b>Nhân vật:</b> 1 người nói &nbsp;|&nbsp; <b>Bối cảnh:</b> Thông tin cộng đồng (hướng dẫn tham quan, thuyết minh địa điểm…)
</div>

<h6 class="fw-semibold">📋 Cấu trúc bài</h6>
<p>Section 2 là bài monologue – <strong>một người nói</strong>, không có đối thoại. Chủ đề thường liên quan đến thông tin cộng đồng hoặc dịch vụ công cộng: hướng dẫn tham quan bảo tàng, giới thiệu trung tâm thể thao, thông báo về sự kiện địa phương.</p>

<h6 class="fw-semibold mt-4">🗺️ Dạng Map Labelling</h6>
<p>Người nghe phải xác định vị trí các địa điểm trên sơ đồ dựa vào hướng dẫn nghe. Các từ chỉ phương hướng quan trọng:</p>
<ul>
  <li><b>opposite / across from</b>: đối diện</li>
  <li><b>next to / adjacent to</b>: ngay bên cạnh</li>
  <li><b>between A and B</b>: giữa A và B</li>
  <li><b>on the left/right of</b>: ở phía trái/phải</li>
  <li><b>at the end of</b>: ở cuối đường</li>
  <li><b>turn left/right at</b>: rẽ trái/phải tại</li>
</ul>

<h6 class="fw-semibold mt-4">🎯 Chiến lược Multiple Choice</h6>
<ul>
  <li>Đọc câu hỏi và 3 lựa chọn trước khi nghe — tìm điểm khác biệt giữa A, B, C</li>
  <li>Người nói thường <b>nhắc đến tất cả</b> các lựa chọn — chỉ một cái đúng</li>
  <li>Nếu người nói đính chính hoặc phủ nhận thông tin, chú ý từ như <em>"but", "actually", "however"</em></li>
</ul>

<h6 class="fw-semibold mt-4">📝 Bài tập: Nghe và điền vị trí</h6>
<div class="card border-primary">
  <div class="card-body">
    <p><b>Hướng dẫn:</b> Nghe bài giới thiệu bảo tàng và viết tên địa điểm vào ô tương ứng trên sơ đồ:</p>
    <p class="text-muted fst-italic">A – Information Desk &nbsp;|&nbsp; B – Gift Shop &nbsp;|&nbsp; C – Café &nbsp;|&nbsp; D – Exhibition Hall &nbsp;|&nbsp; E – Restrooms</p>
    <p class="mb-0 small text-muted">(Điền chữ cái A–E vào đúng vị trí theo hướng dẫn trong băng nghe)</p>
  </div>
</div>
$$ WHERE id = 2;

-- L3: Listening Section 3 – Academic Discussion / Matching
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Listening Section 3: Thảo luận học thuật – Matching & Note Completion</h5>

<div class="alert alert-info mb-4">
  <b>Dạng bài:</b> Matching, Note Completion &nbsp;|&nbsp; <b>Nhân vật:</b> 2–3 sinh viên hoặc giảng viên &nbsp;|&nbsp; <b>Bối cảnh:</b> Thảo luận bài tập nhóm, seminar, consultation
</div>

<h6 class="fw-semibold">📋 Cấu trúc bài</h6>
<p>Section 3 là phần <strong>khó nhất trong 4 sections</strong> của Listening. Cuộc thảo luận học thuật giữa nhiều người với lượng từ chuyên ngành cao và tốc độ nói nhanh hơn Section 1 và 2.</p>

<h6 class="fw-semibold mt-4">🔗 Dạng Matching</h6>
<p>Pessoa nói thường chia sẻ nhiều ý kiến, quan điểm. Nhiệm vụ của bạn là <em>nối từng người với quan điểm tương ứng</em>. Ví dụ:</p>
<div class="card border-secondary mb-3">
  <div class="card-body">
    <p class="mb-2">Nối mỗi sinh viên (1–3) với nhận xét về đề tài nghiên cứu họ đề xuất (A–F):</p>
    <div class="row">
      <div class="col-6">
        <p><b>Sinh viên:</b></p>
        <p>1. Anna<br>2. Ben<br>3. Carlos</p>
      </div>
      <div class="col-6">
        <p><b>Nhận xét:</b></p>
        <p>A. Quá rộng, cần thu hẹp<br>B. Khó tìm nguồn tham khảo<br>C. Phù hợp, có thể triển khai<br>D. Chủ đề đã được nghiên cứu nhiều<br>E. Thiếu dữ liệu thực nghiệm<br>F. Có tiềm năng học thuật cao</p>
      </div>
    </div>
  </div>
</div>

<h6 class="fw-semibold">🎯 Chiến lược Matching</h6>
<ul>
  <li>Đọc tất cả các lựa chọn trước — <b>nhiều câu có thể không được dùng</b></li>
  <li>Chú ý từ chỉ ý kiến: <em>"I think", "In my opinion", "What about...", "I'm not sure about..."</em></li>
  <li>Phân biệt <b>ý kiến cá nhân</b> với <b>thông tin khách quan</b></li>
</ul>

<h6 class="fw-semibold mt-4">📌 Từ vựng học thuật hay gặp</h6>
<ul>
  <li><b>hypothesis</b> – giả thuyết | <b>methodology</b> – phương pháp nghiên cứu</li>
  <li><b>primary/secondary sources</b> – nguồn sơ cấp/thứ cấp</li>
  <li><b>findings</b> – kết quả nghiên cứu | <b>conclude</b> – kết luận</li>
  <li><b>evaluate</b> – đánh giá | <b>justify</b> – biện minh, giải thích</li>
</ul>
$$ WHERE id = 3;

-- L4: Listening Section 4 – Lecture / Summary & Sentence Completion
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Listening Section 4: Bài giảng học thuật – Summary & Sentence Completion</h5>

<div class="alert alert-warning mb-4">
  <i class="fa fa-exclamation-triangle me-2"></i><b>Phần khó nhất trong Listening IELTS!</b> Không có khoảng dừng giữa câu hỏi, chủ đề chuyên ngành, tốc độ nói cao nhất.
</div>

<h6 class="fw-semibold">📋 Cấu trúc bài</h6>
<p>Section 4 là bài giảng của <strong>một giảng viên</strong> về chủ đề học thuật: khoa học, lịch sử, kinh tế, sinh học, công nghệ... Không có khoảng dừng – bạn phải theo dõi liên tục trong ~10 phút.</p>

<h6 class="fw-semibold mt-4">📝 Dạng Note/Summary Completion</h6>
<p>Điền thông tin vào tóm tắt (summary) hoặc ghi chú (notes) theo cấu trúc outline của bài giảng. Lưu ý:</p>
<ul>
  <li>Thông tin xuất hiện <b>theo thứ tự</b> trong bài nghe – theo dõi cấu trúc outline để không bị lạc</li>
  <li>Từ cần điền thường là: <em>danh từ, số liệu, tên riêng hoặc tính từ mô tả</em></li>
  <li>Đọc kỹ context xung quanh ô trống để dự đoán loại từ cần điền</li>
</ul>

<h6 class="fw-semibold mt-4">📖 Ví dụ: Bài giảng về Biến đổi khí hậu</h6>
<div class="card border-primary mb-3">
  <div class="card-body small">
    <p><b>Điền thông tin còn thiếu (không quá 2 từ và/hoặc 1 số):</b></p>
    <p><b>Climate Change: Key Concepts</b><br>
    • Global temperature has risen by approximately <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> °C since the 1800s.<br>
    • Main cause: increased concentration of <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> in the atmosphere.<br>
    • Most significant effect in the Arctic: <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> is melting at record speed.<br>
    • Countries agreed to limit warming to <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> °C in the Paris Agreement.</p>
  </div>
</div>

<h6 class="fw-semibold">🎯 Tips vàng cho Section 4</h6>
<ul>
  <li>Tận dụng <b>30 giây đọc đề</b> để hiểu chủ đề và dự đoán nội dung</li>
  <li>Khi nghe thấy <em>"however", "but", "in contrast"</em> — thông tin quan trọng sắp xuất hiện</li>
  <li>Số liệu, năm tháng: viết ngay vào đề — đừng để trong trí nhớ</li>
  <li>Khi bị bỏ lỡ: <b>đừng dừng lại tiếc nuối</b> — bỏ câu đó, tiếp tục theo dõi</li>
</ul>
$$ WHERE id = 4;

-- L5: Full Mock Test 1
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-file-alt text-primary me-2"></i>Full Mock Test 1 – Mô phỏng thi IELTS hoàn chỉnh</h5>

<div class="alert alert-success mb-4">
  <i class="fa fa-clock me-2"></i><b>Tổng thời gian:</b> ~2 giờ 45 phút &nbsp;|&nbsp; <b>4 kỹ năng:</b> Listening · Reading · Writing · Speaking
</div>

<h6 class="fw-semibold">📋 Cấu trúc bài thi</h6>
<table class="table table-bordered table-sm mb-3">
  <thead class="table-light"><tr><th>Kỹ năng</th><th>Thời gian</th><th>Số câu</th><th>Điểm quy đổi</th></tr></thead>
  <tbody>
    <tr><td>🎧 Listening</td><td>30 phút + 10 phút chép đáp án</td><td>40 câu</td><td>Raw score / 40 → Band</td></tr>
    <tr><td>📖 Reading</td><td>60 phút</td><td>40 câu</td><td>Raw score / 40 → Band</td></tr>
    <tr><td>✍️ Writing</td><td>60 phút</td><td>2 tasks</td><td>Task 1 (1/3) + Task 2 (2/3)</td></tr>
    <tr><td>🎤 Speaking</td><td>11–14 phút</td><td>3 parts</td><td>Fluency, Vocabulary, Grammar, Pronunciation</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">📌 Hướng dẫn làm bài mock test</h6>
<ol>
  <li><b>Chuẩn bị môi trường:</b> Tìm nơi yên tĩnh, tắt điện thoại, chuẩn bị đồng hồ</li>
  <li><b>Làm theo thứ tự:</b> Listening → Reading → Writing (không cần làm Speaking một mình)</li>
  <li><b>Không tra cứu:</b> Làm đúng như thi thật — không dùng từ điển hay Google</li>
  <li><b>Sau khi làm xong:</b> Chấm điểm Listening và Reading với đáp án. Tự nhận xét Writing.</li>
  <li><b>Phân tích lỗi:</b> Ghi lại <em>loại lỗi</em> bạn mắc phải, không chỉ số điểm</li>
</ol>

<h6 class="fw-semibold mt-4">📊 Bảng quy đổi điểm IELTS</h6>
<table class="table table-bordered table-sm">
  <thead class="table-light"><tr><th>Số câu đúng (Listening/Reading)</th><th>Band Score</th></tr></thead>
  <tbody>
    <tr><td>39–40</td><td>Band 9.0</td></tr>
    <tr><td>37–38</td><td>Band 8.5</td></tr>
    <tr><td>35–36</td><td>Band 8.0</td></tr>
    <tr><td>32–34</td><td>Band 7.5</td></tr>
    <tr><td>30–31</td><td>Band 7.0</td></tr>
    <tr><td>26–29</td><td>Band 6.5</td></tr>
    <tr><td>23–25</td><td>Band 6.0</td></tr>
  </tbody>
</table>
$$ WHERE id = 5;

-- ── Course 6: UPPER IELTS ──────────────────────────────────────────

-- L6: Lecture Listening Science & Technology
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Lecture Listening: Science & Technology</h5>

<div class="alert alert-info mb-4">
  <b>Cấp độ:</b> Upper-Intermediate (B2–C1) &nbsp;|&nbsp; <b>Dạng bài:</b> Sentence Completion, Note-taking
</div>

<h6 class="fw-semibold">🎯 Mục tiêu bài học</h6>
<ul>
  <li>Hiểu được bài giảng học thuật về chủ đề công nghệ (AI, robotics, biotechnology...)</li>
  <li>Thực hành kỹ năng <strong>note-taking</strong> trong khi nghe</li>
  <li>Nắm vững từ vựng học thuật liên quan đến khoa học và công nghệ</li>
</ul>

<h6 class="fw-semibold mt-4">📚 Từ vựng cần nhớ</h6>
<table class="table table-sm table-bordered">
  <thead class="table-light"><tr><th>Từ / Cụm từ</th><th>Nghĩa</th><th>Ví dụ</th></tr></thead>
  <tbody>
    <tr><td><b>disruptive technology</b></td><td>công nghệ đột phá</td><td>Smartphones were a disruptive technology.</td></tr>
    <tr><td><b>automation</b></td><td>tự động hóa</td><td>Automation threatens many manufacturing jobs.</td></tr>
    <tr><td><b>algorithm</b></td><td>thuật toán</td><td>Machine learning uses complex algorithms.</td></tr>
    <tr><td><b>innovation</b></td><td>sự đổi mới, cải tiến</td><td>R&D drives innovation in the tech sector.</td></tr>
    <tr><td><b>implications</b></td><td>hàm ý, tác động</td><td>What are the ethical implications of AI?</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold mt-4">✍️ Kỹ thuật Note-taking hiệu quả</h6>
<ul>
  <li>Dùng <b>ký hiệu tắt:</b> tech = technology, ↑ = increase, → = leads to, ∴ = therefore</li>
  <li>Chỉ ghi <b>key words</b>, không ghi nguyên câu</li>
  <li>Dùng <b>xuống dòng và thụt lề</b> để phân cấp ý chính – ý phụ</li>
  <li>Ghi <b>con số và ngày tháng</b> ngay lập tức — dễ quên nhất</li>
</ul>

<h6 class="fw-semibold mt-4">📝 Sample: Sentence Completion Exercise</h6>
<div class="card border-primary">
  <div class="card-body small">
    Complete the sentences (NO MORE THAN TWO WORDS):<br><br>
    1. The lecturer believes that AI will primarily affect jobs requiring <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> tasks.<br>
    2. <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> was cited as the main barrier to widespread AI adoption.<br>
    3. The professor recommends that students focus on developing <u>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</u> skills to remain competitive.
  </div>
</div>
$$ WHERE id = 6;

-- L7: Academic Discussion Environment
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Academic Discussion: Environmental Issues</h5>

<div class="alert alert-info mb-4">
  <b>Dạng bài:</b> Matching Speakers to Opinions &nbsp;|&nbsp; <b>Chủ đề:</b> Biến đổi khí hậu, Môi trường
</div>

<h6 class="fw-semibold">📚 Từ vựng môi trường – Environment Vocabulary</h6>
<table class="table table-sm table-bordered">
  <thead class="table-light"><tr><th>Từ vựng</th><th>Nghĩa</th></tr></thead>
  <tbody>
    <tr><td>carbon footprint</td><td>lượng khí thải carbon của một người/tổ chức</td></tr>
    <tr><td>renewable energy</td><td>năng lượng tái tạo</td></tr>
    <tr><td>deforestation</td><td>phá rừng</td></tr>
    <tr><td>biodiversity</td><td>đa dạng sinh học</td></tr>
    <tr><td>greenhouse gas</td><td>khí nhà kính</td></tr>
    <tr><td>sustainable development</td><td>phát triển bền vững</td></tr>
    <tr><td>ecosystem</td><td>hệ sinh thái</td></tr>
    <tr><td>conservation</td><td>bảo tồn</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold mt-4">🗣️ Cách nhận biết ý kiến từng người trong thảo luận</h6>
<p>Khi nghe thảo luận nhóm, chú ý các câu thể hiện ý kiến cá nhân:</p>
<ul>
  <li><em>"I strongly believe that..."</em> / <em>"In my view..."</em> → ý kiến cá nhân rõ ràng</li>
  <li><em>"I'm not convinced that..."</em> / <em>"I have doubts about..."</em> → không đồng ý</li>
  <li><em>"That's a fair point, but..."</em> → đồng ý một phần rồi phản biện</li>
  <li><em>"Research shows..."</em> / <em>"Studies suggest..."</em> → dẫn chứng khách quan</li>
</ul>

<h6 class="fw-semibold mt-4">📝 Bài tập Matching Opinions</h6>
<div class="card border-primary">
  <div class="card-body small">
    <p><b>Match each speaker (1–3) with the correct opinion (A–E). There are TWO opinions you will NOT need.</b></p>
    <p><b>Speakers:</b> 1. Professor Lim &nbsp; 2. Student Maya &nbsp; 3. Student Jake</p>
    <p><b>Opinions:</b><br>
    A. Individual lifestyle changes are more effective than government policy.<br>
    B. The economic cost of climate action is too high for developing countries.<br>
    C. International cooperation is essential to tackle climate change.<br>
    D. Technological innovation will solve environmental problems without sacrifice.<br>
    E. Public awareness campaigns have not been effective enough.</p>
  </div>
</div>
$$ WHERE id = 7;

-- L8: Reading Coral Reef Ecosystems
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-book-open text-success me-2"></i>Reading: Coral Reef Ecosystems</h5>

<div class="alert alert-info mb-4">
  <b>Passage length:</b> ~900 từ &nbsp;|&nbsp; <b>Dạng bài:</b> Yes/No/Not Given, Summary Completion &nbsp;|&nbsp; <b>Trình độ:</b> Academic
</div>

<h6 class="fw-semibold">📖 Key Vocabulary: Ocean & Biology</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Từ vựng</th><th>Nghĩa</th><th>Trong văn cảnh</th></tr></thead>
  <tbody>
    <tr><td>coral bleaching</td><td>hiện tượng tẩy trắng san hô</td><td>Rising temperatures cause coral bleaching.</td></tr>
    <tr><td>symbiotic</td><td>cộng sinh</td><td>Corals have a symbiotic relationship with algae.</td></tr>
    <tr><td>marine biodiversity</td><td>đa dạng sinh học biển</td><td>Reefs support 25% of marine biodiversity.</td></tr>
    <tr><td>calcification</td><td>sự canxi hóa</td><td>Ocean acidification impairs calcification.</td></tr>
    <tr><td>zooxanthellae</td><td>tảo vàng cộng sinh trong san hô</td><td>Zooxanthellae provide 90% of coral's energy.</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">❓ Dạng bài Yes / No / Not Given</h6>
<p>Phân biệt rõ 3 đáp án:</p>
<ul>
  <li><b>YES:</b> Thông tin trong passage <em>xác nhận</em> nhận định (agrees with)</li>
  <li><b>NO:</b> Thông tin trong passage <em>phủ nhận</em> nhận định (contradicts)</li>
  <li><b>NOT GIVEN:</b> Passage <em>không đề cập</em> đến thông tin này (no information)</li>
</ul>
<div class="alert alert-warning">
  <b>⚠️ Lỗi thường gặp:</b> Dùng kiến thức bên ngoài để trả lời. Chỉ dùng thông tin trong bài đọc!
</div>

<h6 class="fw-semibold mt-4">📝 Practice Questions</h6>
<div class="card border-primary">
  <div class="card-body small">
    <p><b>Do the following statements agree with the information given in the Reading Passage?</b><br>
    Write YES, NO or NOT GIVEN for each statement:</p>
    <ol>
      <li>Coral reefs cover less than 1% of the ocean floor but support a quarter of all marine species.</li>
      <li>Coral bleaching only occurs in tropical regions near the equator.</li>
      <li>The relationship between corals and zooxanthellae was first described in the 19th century.</li>
      <li>Ocean acidification makes it harder for corals to build their calcium carbonate skeletons.</li>
      <li>Scientists unanimously agree that coral reefs will disappear entirely by 2050.</li>
    </ol>
  </div>
</div>
$$ WHERE id = 8;

-- L9: Reading History of Urban Planning
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-book-open text-success me-2"></i>Reading: History of Urban Planning</h5>

<div class="alert alert-info mb-4">
  <b>Passage length:</b> ~1000 từ &nbsp;|&nbsp; <b>Dạng bài:</b> Matching Headings, Multiple Choice, Short Answer
</div>

<h6 class="fw-semibold">🏙️ Vocabulary: Urban & Architecture</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Từ vựng</th><th>Nghĩa</th></tr></thead>
  <tbody>
    <tr><td>urban sprawl</td><td>sự phát triển đô thị dàn trải không kiểm soát</td></tr>
    <tr><td>infrastructure</td><td>cơ sở hạ tầng</td></tr>
    <tr><td>zoning</td><td>phân khu chức năng (khu dân cư, công nghiệp...)</td></tr>
    <tr><td>gentrification</td><td>quá trình cải tạo đô thị làm tăng giá trị khu vực</td></tr>
    <tr><td>density</td><td>mật độ dân số hoặc xây dựng</td></tr>
    <tr><td>municipality</td><td>đơn vị hành chính đô thị, chính quyền thành phố</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">📑 Chiến lược: Matching Headings</h6>
<ol>
  <li>Đọc tất cả headings trước — ghi nhớ ý chính của từng heading</li>
  <li>Đọc đoạn văn — tìm <b>main idea</b> (thường ở câu đầu hoặc cuối đoạn)</li>
  <li>Loại bỏ headings đã dùng khỏi danh sách</li>
  <li>Chú ý: heading diễn đạt ý <em>khái quát</em> của đoạn, không chỉ một chi tiết nhỏ</li>
</ol>

<h6 class="fw-semibold mt-4">✏️ Short Answer Questions – Lưu ý</h6>
<ul>
  <li>Giới hạn từ: <b>NO MORE THAN THREE WORDS</b> — chỉ điền từ có trong passage</li>
  <li>Câu hỏi theo thứ tự nội dung bài — dùng để định vị vị trí cần đọc</li>
  <li>Tránh paraphrase — dùng chính xác từ trong bài đọc</li>
</ul>

<h6 class="fw-semibold mt-4">📝 Sample Questions</h6>
<div class="card border-primary">
  <div class="card-body small">
    <p><b>Answer the questions below. Choose NO MORE THAN THREE WORDS from the passage.</b></p>
    <ol>
      <li>What urban problem did Haussmann's renovation of Paris aim to solve in the 1850s?</li>
      <li>Which city is often cited as the first to implement comprehensive zoning laws?</li>
      <li>What term is used to describe the movement of wealthier residents into previously low-income urban areas?</li>
    </ol>
  </div>
</div>
$$ WHERE id = 9;

-- L10: Academic Vocabulary Education (TEXT type)
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-spell-check text-info me-2"></i>Academic Vocabulary: Topic – Education</h5>

<p class="lead">Nắm vững từ vựng học thuật chủ đề Giáo dục là chìa khóa cho cả 4 kỹ năng IELTS — đặc biệt Writing Task 2 và Speaking Part 3.</p>

<h6 class="fw-semibold mt-4">📚 Nhóm 1: Hệ thống giáo dục</h6>
<table class="table table-sm table-bordered">
  <thead class="table-light"><tr><th>Từ vựng</th><th>Nghĩa & Cách dùng</th></tr></thead>
  <tbody>
    <tr><td><b>curriculum</b> (n)</td><td>chương trình giảng dạy — <em>a well-designed curriculum</em></td></tr>
    <tr><td><b>pedagogy</b> (n)</td><td>phương pháp sư phạm — <em>student-centred pedagogy</em></td></tr>
    <tr><td><b>assessment</b> (n)</td><td>đánh giá — <em>formative/summative assessment</em></td></tr>
    <tr><td><b>literacy</b> (n)</td><td>khả năng đọc viết; tỷ lệ biết chữ — <em>digital literacy</em></td></tr>
    <tr><td><b>vocational training</b></td><td>đào tạo nghề — <em>vs. academic education</em></td></tr>
  </tbody>
</table>

<h6 class="fw-semibold mt-4">📚 Nhóm 2: Vấn đề và xu hướng giáo dục</h6>
<table class="table table-sm table-bordered">
  <thead class="table-light"><tr><th>Từ vựng</th><th>Nghĩa & Cách dùng</th></tr></thead>
  <tbody>
    <tr><td><b>rote learning</b></td><td>học vẹt, học thuộc lòng — <em>rote learning discourages critical thinking</em></td></tr>
    <tr><td><b>critical thinking</b></td><td>tư duy phản biện — <em>essential skill in the 21st century</em></td></tr>
    <tr><td><b>academic pressure</b></td><td>áp lực học tập — <em>excessive academic pressure</em></td></tr>
    <tr><td><b>extracurricular</b></td><td>ngoại khóa — <em>extracurricular activities broaden students' experience</em></td></tr>
    <tr><td><b>tuition fee</b></td><td>học phí — <em>rising tuition fees deter students from university</em></td></tr>
    <tr><td><b>scholarship</b></td><td>học bổng — <em>merit-based scholarship</em></td></tr>
  </tbody>
</table>

<h6 class="fw-semibold mt-4">✍️ Câu mẫu viết luận (Writing Task 2)</h6>
<div class="card border-success mb-3">
  <div class="card-body">
    <p><em>"While <b>rote learning</b> remains prevalent in many education systems, there is a growing consensus that fostering <b>critical thinking</b> and <b>creativity</b> better prepares students for the demands of modern society."</em></p>
  </div>
</div>

<h6 class="fw-semibold">🎯 Collocation hay gặp – Ghi nhớ theo cụm</h6>
<ul>
  <li>provide / receive <b>quality education</b></li>
  <li>bridge the <b>educational gap</b></li>
  <li><b>compulsory education</b> up to the age of 16</li>
  <li>invest in <b>early childhood education</b></li>
  <li>expand <b>access to education</b> in rural areas</li>
</ul>
$$ WHERE id = 10;

-- L11: Grammar Focus Inversion & Cleft Sentences (TEXT type)
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-pen-nib text-info me-2"></i>Grammar Focus: Đảo ngữ (Inversion) & Câu chẻ (Cleft Sentences)</h5>

<p class="lead">Hai cấu trúc ngữ pháp nâng cao giúp câu văn <strong>academic</strong> của bạn đạt Band 7+ trong IELTS Writing và speaking.</p>

<h6 class="fw-semibold mt-4">🔄 1. Đảo ngữ (Inversion)</h6>
<p>Đảo ngữ là cấu trúc đặt trợ động từ hoặc động từ be lên trước chủ ngữ để <strong>nhấn mạnh</strong>. Thường dùng trong văn phong học thuật và trang trọng.</p>

<div class="card border-info mb-3">
  <div class="card-header bg-info text-white small"><b>Not only... but also</b></div>
  <div class="card-body small">
    <p class="mb-1">Thường: <em>She is not only intelligent but also hardworking.</em></p>
    <p class="mb-0">Đảo ngữ: <em><b>Not only is she</b> intelligent, <b>but she also</b> works extremely hard.</em></p>
  </div>
</div>
<div class="card border-info mb-3">
  <div class="card-header bg-info text-white small"><b>Never / Rarely / Seldom / Hardly ever</b></div>
  <div class="card-body small">
    <p class="mb-1">Thường: <em>We rarely see such spectacular results.</em></p>
    <p class="mb-0">Đảo ngữ: <em><b>Rarely do we see</b> such spectacular results.</em></p>
  </div>
</div>
<div class="card border-info mb-3">
  <div class="card-header bg-info text-white small"><b>Hardly... when / No sooner... than</b></div>
  <div class="card-body small">
    <p class="mb-1">Đảo ngữ: <em><b>Hardly had</b> the government announced the policy <b>when</b> protests began.</em></p>
    <p class="mb-0">Đảo ngữ: <em><b>No sooner had</b> he arrived <b>than</b> the meeting started.</em></p>
  </div>
</div>

<h6 class="fw-semibold mt-4">✂️ 2. Câu chẻ (Cleft Sentences)</h6>
<p>Dùng để nhấn mạnh một thành phần cụ thể trong câu. Có hai loại:</p>

<div class="card border-success mb-3">
  <div class="card-header bg-success text-white small"><b>It-cleft: It is/was... that/who...</b></div>
  <div class="card-body small">
    <p class="mb-1">Thường: <em>The government introduced this policy in 2010.</em></p>
    <p class="mb-1">Nhấn chủ ngữ: <em><b>It was the government</b> that introduced this policy in 2010.</em></p>
    <p class="mb-0">Nhấn thời gian: <em><b>It was in 2010</b> that the government introduced this policy.</em></p>
  </div>
</div>
<div class="card border-success mb-3">
  <div class="card-header bg-success text-white small"><b>What-cleft: What... is/was...</b></div>
  <div class="card-body small">
    <p class="mb-1">Thường: <em>We need more investment in education.</em></p>
    <p class="mb-0">What-cleft: <em><b>What we need</b> is more investment in education.</em></p>
  </div>
</div>

<h6 class="fw-semibold mt-4">✏️ Luyện tập: Viết lại câu dùng đảo ngữ hoặc câu chẻ</h6>
<ol>
  <li><em>The results were so impressive that the researchers published them immediately.</em></li>
  <li><em>She seldom makes grammatical errors in her writing.</em></li>
  <li><em>The scientist discovered the cure, not the politician.</em></li>
  <li><em>I have never seen such a complex dataset.</em></li>
</ol>
$$ WHERE id = 11;

-- ── Course 20: Boost Your Listening - A2 ──────────────────────────

-- L12: Greeting & Introduction
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-headphones text-danger me-2"></i>Greeting & Introduction – Chào hỏi và Giới thiệu bản thân</h5>

<div class="alert alert-info mb-4">
  <b>Trình độ:</b> A2 &nbsp;|&nbsp; <b>Kỹ năng:</b> Listening Comprehension + Vocabulary
</div>

<h6 class="fw-semibold">💬 Phrases – Chào hỏi thông dụng</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Tiếng Anh</th><th>Tiếng Việt</th><th>Trả lời</th></tr></thead>
  <tbody>
    <tr><td>Hi / Hello!</td><td>Xin chào!</td><td>Hi! / Hey!</td></tr>
    <tr><td>How are you?</td><td>Bạn có khỏe không?</td><td>Fine, thanks. / Pretty good!</td></tr>
    <tr><td>How's it going?</td><td>Dạo này thế nào?</td><td>Not bad. / Great!</td></tr>
    <tr><td>Nice to meet you.</td><td>Rất vui được gặp bạn.</td><td>Nice to meet you too!</td></tr>
    <tr><td>What's your name?</td><td>Bạn tên gì?</td><td>My name is... / I'm...</td></tr>
    <tr><td>Where are you from?</td><td>Bạn đến từ đâu?</td><td>I'm from Vietnam. / I'm Vietnamese.</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">🎧 Luyện nghe: Hội thoại mẫu</h6>
<div class="card border-primary mb-3">
  <div class="card-body">
    <p><b>Anna:</b> Hi! I'm Anna. Nice to meet you!</p>
    <p><b>Tom:</b> Hi Anna! I'm Tom. Nice to meet you too. Are you new here?</p>
    <p><b>Anna:</b> Yes, I just started this week. Where are you from, Tom?</p>
    <p><b>Tom:</b> I'm from Australia. And you?</p>
    <p class="mb-0"><b>Anna:</b> I'm from Vietnam. I moved here last month.</p>
  </div>
</div>

<h6 class="fw-semibold">📝 Bài tập nghe: Điền thông tin</h6>
<p>Nghe đoạn hội thoại và điền thông tin:</p>
<table class="table table-sm table-bordered">
  <tr><td>Speaker 1's name</td><td>______________</td></tr>
  <tr><td>Speaker 2's job</td><td>______________</td></tr>
  <tr><td>Where Speaker 1 is from</td><td>______________</td></tr>
  <tr><td>How long Speaker 2 has lived here</td><td>______________</td></tr>
</table>
$$ WHERE id = 12;

-- L13: Asking for Directions
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-map-marked-alt text-warning me-2"></i>Asking for Directions – Hỏi đường</h5>

<div class="alert alert-info mb-4">
  <b>Trình độ:</b> A2 &nbsp;|&nbsp; <b>Kỹ năng:</b> Listening + Speaking Practice
</div>

<h6 class="fw-semibold">🗺️ Vocabulary – Từ vựng chỉ đường</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Tiếng Anh</th><th>Tiếng Việt</th></tr></thead>
  <tbody>
    <tr><td>turn left / right</td><td>rẽ trái / phải</td></tr>
    <tr><td>go straight ahead</td><td>đi thẳng</td></tr>
    <tr><td>at the traffic lights</td><td>ở đèn giao thông</td></tr>
    <tr><td>on the corner</td><td>ở góc đường</td></tr>
    <tr><td>next to / opposite</td><td>ngay cạnh / đối diện</td></tr>
    <tr><td>take the first / second street</td><td>rẽ vào đường đầu tiên / thứ hai</td></tr>
    <tr><td>It's about 5 minutes' walk.</td><td>Đi bộ khoảng 5 phút.</td></tr>
    <tr><td>You can't miss it!</td><td>Bạn sẽ không bỏ sót đâu!</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">💬 Useful Phrases – Hỏi và trả lời đường đi</h6>
<div class="card border-primary mb-3">
  <div class="card-body">
    <p><b>Hỏi đường:</b></p>
    <ul class="mb-2">
      <li><em>Excuse me, could you tell me how to get to the train station?</em></li>
      <li><em>I'm looking for the post office. Is it far from here?</em></li>
    </ul>
    <p><b>Trả lời:</b></p>
    <ul class="mb-0">
      <li><em>Go straight ahead, then turn left at the traffic lights.</em></li>
      <li><em>It's on the right, next to the supermarket.</em></li>
    </ul>
  </div>
</div>

<h6 class="fw-semibold">📝 Bài tập: Điền phương hướng còn thiếu</h6>
<p>Đọc hướng dẫn và vẽ/đánh dấu vị trí trên sơ đồ:</p>
<div class="card border-secondary">
  <div class="card-body small">
    <em>"Start at the school. <b>Go straight ahead</b> for two blocks. Then <b>turn right</b> at the traffic lights. The pharmacy is <b>on the left</b>, <b>opposite</b> the park."</em>
    <p class="mt-2 mb-0 text-muted">→ Đánh dấu vị trí nhà thuốc (pharmacy) trên sơ đồ.</p>
  </div>
</div>
$$ WHERE id = 13;

-- L14: Shopping Conversations
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-shopping-bag text-success me-2"></i>Shopping Conversations – Hội thoại mua sắm</h5>

<div class="alert alert-info mb-4">
  <b>Trình độ:</b> A2 &nbsp;|&nbsp; <b>Kỹ năng:</b> Listening for specific information
</div>

<h6 class="fw-semibold">🛍️ Vocabulary – Mua sắm</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Tiếng Anh</th><th>Tiếng Việt</th></tr></thead>
  <tbody>
    <tr><td>How much does it cost?</td><td>Cái này giá bao nhiêu?</td></tr>
    <tr><td>It's on sale. / There's a discount.</td><td>Đang giảm giá.</td></tr>
    <tr><td>Do you have this in a different size/colour?</td><td>Bạn có size/màu khác không?</td></tr>
    <tr><td>Can I try this on?</td><td>Tôi có thể thử không?</td></tr>
    <tr><td>I'll take it!</td><td>Tôi lấy cái này!</td></tr>
    <tr><td>Do you accept card / cash?</td><td>Bạn nhận thẻ / tiền mặt không?</td></tr>
    <tr><td>Can I have a receipt?</td><td>Cho tôi xin hóa đơn được không?</td></tr>
    <tr><td>I'd like to return / exchange this.</td><td>Tôi muốn trả lại / đổi cái này.</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">🎧 Hội thoại mẫu</h6>
<div class="card border-primary mb-3">
  <div class="card-body">
    <p><b>Customer:</b> Excuse me, how much is this jacket?</p>
    <p><b>Shop assistant:</b> It's £45. But it's on sale today — 20% off, so it's £36.</p>
    <p><b>Customer:</b> Great! Do you have it in blue?</p>
    <p><b>Shop assistant:</b> I'm sorry, we only have it in black and grey.</p>
    <p><b>Customer:</b> I'll take the grey one. Can I pay by card?</p>
    <p class="mb-0"><b>Shop assistant:</b> Of course! Here's the machine.</p>
  </div>
</div>

<h6 class="fw-semibold">📝 Bài tập: Nghe và trả lời True/False</h6>
<ol class="small">
  <li>The jacket's original price is £45. (True / False)</li>
  <li>The customer buys the blue jacket. (True / False)</li>
  <li>The customer pays with cash. (True / False)</li>
  <li>The discount is 20%. (True / False)</li>
</ol>
$$ WHERE id = 14;

-- L15: School Announcements
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-bullhorn text-primary me-2"></i>School Announcements – Thông báo nhà trường</h5>

<div class="alert alert-info mb-4">
  <b>Trình độ:</b> A2 &nbsp;|&nbsp; <b>Dạng bài:</b> Listening for specific information (dates, times, places)
</div>

<h6 class="fw-semibold">📅 Vocabulary – Thông báo thường gặp</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Tiếng Anh</th><th>Tiếng Việt</th></tr></thead>
  <tbody>
    <tr><td>The exam is scheduled for...</td><td>Kỳ thi được lên lịch vào...</td></tr>
    <tr><td>Please note that class is cancelled.</td><td>Xin lưu ý lớp học bị hủy.</td></tr>
    <tr><td>The deadline has been extended to...</td><td>Hạn chót đã được gia hạn đến...</td></tr>
    <tr><td>Students are required to...</td><td>Học sinh được yêu cầu...</td></tr>
    <tr><td>The event will take place in Room 204.</td><td>Sự kiện sẽ diễn ra ở phòng 204.</td></tr>
    <tr><td>Please bring your student ID.</td><td>Vui lòng mang theo thẻ học sinh.</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">🎧 Thông báo mẫu</h6>
<div class="card border-primary mb-3">
  <div class="card-body">
    <p><em>"Good morning, students. I have a few announcements. First, the Math exam scheduled for Thursday has been moved to <b>Friday, 14th March</b> at <b>9:00 AM</b> in the <b>Main Hall</b>. Please bring your student ID and a pencil. Second, the school library will be <b>closed on Monday</b> for maintenance. Finally, the sports day event will take place on <b>Saturday morning</b>, starting at 8:30."</em></p>
  </div>
</div>

<h6 class="fw-semibold">📝 Bài tập: Điền thông tin</h6>
<table class="table table-sm table-bordered">
  <tr><td>New date for Math exam</td><td>______________</td></tr>
  <tr><td>Time of Math exam</td><td>______________</td></tr>
  <tr><td>Location of Math exam</td><td>______________</td></tr>
  <tr><td>Day library is closed</td><td>______________</td></tr>
  <tr><td>Start time of sports day</td><td>______________</td></tr>
</table>
$$ WHERE id = 15;

-- L16: Simple Instructions at Work
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-cogs text-secondary me-2"></i>Simple Instructions at Work – Hướng dẫn công việc đơn giản</h5>

<div class="alert alert-info mb-4">
  <b>Trình độ:</b> A2 &nbsp;|&nbsp; <b>Kỹ năng:</b> Listening for instructions & procedures
</div>

<h6 class="fw-semibold">🛠️ Vocabulary – Hướng dẫn và quy trình</h6>
<table class="table table-sm table-bordered mb-3">
  <thead class="table-light"><tr><th>Tiếng Anh</th><th>Tiếng Việt</th></tr></thead>
  <tbody>
    <tr><td>First / Then / After that / Finally</td><td>Đầu tiên / Tiếp theo / Sau đó / Cuối cùng</td></tr>
    <tr><td>Make sure (that)...</td><td>Đảm bảo rằng...</td></tr>
    <tr><td>You need to / You have to</td><td>Bạn cần / Bạn phải</td></tr>
    <tr><td>Don't forget to...</td><td>Đừng quên...</td></tr>
    <tr><td>Press the button / Switch it on</td><td>Nhấn nút / Bật lên</td></tr>
    <tr><td>Fill in the form</td><td>Điền vào mẫu đơn</td></tr>
    <tr><td>Check / Confirm</td><td>Kiểm tra / Xác nhận</td></tr>
    <tr><td>Report to your manager</td><td>Báo cáo với quản lý</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">🎧 Ví dụ: Hướng dẫn sử dụng máy photocopy</h6>
<div class="card border-primary mb-3">
  <div class="card-body small">
    <p><em>"OK, so to use the photocopier, <b>first</b> press the green button to turn it on. <b>Then</b> place your document face down on the glass. <b>After that</b>, select the number of copies using the keypad. <b>Make sure</b> you choose the right paper size — A4 or A3. <b>Finally</b>, press the large COPY button. <b>Don't forget</b> to collect your original document when you're done!"</em></p>
  </div>
</div>

<h6 class="fw-semibold">📝 Bài tập: Sắp xếp các bước đúng thứ tự</h6>
<p>Sắp xếp các bước sau theo đúng thứ tự (1–5):</p>
<ul class="small">
  <li>___ Press the large COPY button.</li>
  <li>___ Select the number of copies.</li>
  <li>___ Turn on the machine by pressing the green button.</li>
  <li>___ Collect your original document.</li>
  <li>___ Place the document face down on the glass.</li>
</ul>
$$ WHERE id = 16;

-- L17: Final Listening Test A2
UPDATE lessons SET content = $$
<h5 class="fw-bold mb-3"><i class="fa fa-graduation-cap text-success me-2"></i>Final Listening Test – A2 Level</h5>

<div class="alert alert-warning mb-4">
  <i class="fa fa-clock me-2"></i><b>Thời gian:</b> 25 phút &nbsp;|&nbsp; <b>Số câu:</b> 30 câu &nbsp;|&nbsp; <b>Mục tiêu:</b> Đánh giá toàn diện kỹ năng nghe A2
</div>

<h6 class="fw-semibold">📋 Cấu trúc bài kiểm tra</h6>
<table class="table table-bordered table-sm mb-3">
  <thead class="table-light"><tr><th>Phần</th><th>Nội dung</th><th>Số câu</th><th>Thời gian</th></tr></thead>
  <tbody>
    <tr><td>Part 1</td><td>Hội thoại đời thường – điền form</td><td>8 câu</td><td>~7 phút</td></tr>
    <tr><td>Part 2</td><td>Thông báo ngắn – True/False</td><td>10 câu</td><td>~8 phút</td></tr>
    <tr><td>Part 3</td><td>Hội thoại mua sắm / chỉ đường – chọn đáp án</td><td>12 câu</td><td>~10 phút</td></tr>
  </tbody>
</table>

<h6 class="fw-semibold">📌 Checklist trước khi làm bài</h6>
<ul>
  <li>✅ Đọc hết câu hỏi trước khi nghe</li>
  <li>✅ Xác định LOẠI thông tin cần nghe (số? tên? địa điểm?)</li>
  <li>✅ Không lo lắng nếu lỡ 1 câu — tiếp tục theo dõi câu tiếp theo</li>
  <li>✅ Kiểm tra chính tả sau khi nghe xong</li>
  <li>✅ Không để trống câu nào — đoán nếu không chắc</li>
</ul>

<h6 class="fw-semibold mt-4">🎯 Đánh giá kết quả</h6>
<table class="table table-sm table-bordered">
  <thead class="table-light"><tr><th>Số câu đúng</th><th>Nhận xét</th></tr></thead>
  <tbody>
    <tr class="table-success"><td>27–30</td><td>Xuất sắc – Sẵn sàng lên trình độ B1</td></tr>
    <tr class="table-info"><td>22–26</td><td>Tốt – Cần luyện thêm từ vựng</td></tr>
    <tr class="table-warning"><td>16–21</td><td>Khá – Cần luyện nghe thêm mỗi ngày</td></tr>
    <tr class="table-danger"><td>0–15</td><td>Cần ôn lại tất cả các topic trong khóa học</td></tr>
  </tbody>
</table>
$$ WHERE id = 17;
