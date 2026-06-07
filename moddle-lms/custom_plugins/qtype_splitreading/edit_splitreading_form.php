<?php
defined('MOODLE_INTERNAL') || die();
require_once($CFG->dirroot . '/question/type/description/edit_description_form.php');

class qtype_splitreading_edit_form extends qtype_description_edit_form {
    protected function definition_inner($mform) {
        parent::definition_inner($mform);
        $mform->insertElementBefore($mform->createElement('static', 'readingguide', '', '<div class="alert alert-info">Giao diện thi sẽ cố định phần văn bản này ở nửa TRÁI màn hình, và đẩy các câu hỏi tiếp theo sang nửa PHẢI màn hình.</div>'), 'questiontext');
    }
}
