package org.example.Core.Task.Model;

public enum TaskType {
    /** Multiple-choice questions; checked automatically. */
    QUIZ(true),
    /** A code the company hides in its content (video, blog...); checked automatically. */
    SECRET_CODE(true),
    /** Free-text proof (link, handle, screenshot URL); reviewed by the company. */
    MANUAL_PROOF(false);

    private final boolean autoVerified;

    TaskType(boolean autoVerified) {
        this.autoVerified = autoVerified;
    }

    public boolean isAutoVerified() {
        return autoVerified;
    }
}
