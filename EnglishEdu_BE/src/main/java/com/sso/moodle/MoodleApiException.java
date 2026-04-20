package com.sso.moodle;

public class MoodleApiException extends RuntimeException {
    private final String errorCode;

    public MoodleApiException(String message) {
        super(message);
        this.errorCode = null;
    }

    public MoodleApiException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
