package com.a9.etutoring.domain.enums;

public enum VirtualMeetingPlatform {
    ZOOM,
    MICROSOFT_TEAMS,
    GOOGLE_MEET,
    OTHER;

    public String displayName() {
        return switch (this) {
            case ZOOM -> "Zoom";
            case MICROSOFT_TEAMS -> "Microsoft Teams";
            case GOOGLE_MEET -> "Google Meet";
            case OTHER -> "Other";
        };
    }
}
