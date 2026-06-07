<?php
defined('MOODLE_INTERNAL') || die();
require_once($CFG->dirroot . '/question/type/description/questiontype.php');

class qtype_splitreading extends qtype_description {
    public function is_real_question_type() {
        return false;
    }

    public function is_manual_graded() {
        return false;
    }

    public function actual_number_of_questions($questiondata) {
        return 0;
    }
}
