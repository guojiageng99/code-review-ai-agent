package com.codeguardian.constants;

public final class ReviewConstants {
    private ReviewConstants() {}
    public enum ReviewType { PROJECT, DIRECTORY, FILE, SNIPPET, GIT }
    public enum TaskStatus { PENDING, RUNNING, COMPLETED, FAILED }
    public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }
    public enum Category { SECURITY, PERFORMANCE, BUG, CODE_STYLE, MAINTAINABILITY }
}
