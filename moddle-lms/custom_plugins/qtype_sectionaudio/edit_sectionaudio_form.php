<?php
defined('MOODLE_INTERNAL') || die();
require_once($CFG->dirroot . '/question/type/description/edit_description_form.php');

class qtype_sectionaudio_edit_form extends qtype_description_edit_form {
    protected function definition_inner($mform) {
        parent::definition_inner($mform);
        $mform->insertElementBefore($mform->createElement('static', 'audioguide', '', '<div class="alert alert-info">Vui lòng đính kèm file MP3 vào khung "Question text" bên dưới. Hệ thống sẽ tự động phát nó liên tục.</div>'), 'questiontext');
    }
}
