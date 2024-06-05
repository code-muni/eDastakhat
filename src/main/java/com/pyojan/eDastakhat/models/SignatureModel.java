package com.pyojan.eDastakhat.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.sf.oval.constraint.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignatureModel {

    private static final String PAGE_REGEX_PATTERN = "(?!0)(?i)(f|l|a|\\d+)";

    @NotNull(message = "CertInfo cannot be null")
    @AssertValid
    private CertInfo certInfo;

    @NotNull(message = "Options cannot be null")
    @AssertValid
    private Options options;

    @NotNull(message = "Pdf cannot be null")
    @AssertValid
    private Pdf pdf;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertInfo {
        @NotEmpty(message = "pfxPath cannot be empty")
        @NotNull(message = "pfxPath cannot be null")
        private String pfxPath;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Options {
        @NotEmpty(message = "page cannot be empty")
        @MatchPattern(pattern = PAGE_REGEX_PATTERN, message = "Invalid page value. Valid values are: F, L, A, <number>")
        private String page = "L";

        @NotEmpty(message = "coord cannot be empty")
        @Size(min = 4, max = 4, message = "coord must have exactly 4 elements of <number>")
        private int[] coord = {0, 0, 0, 0};

        @Length(max = 25, message = "reason cannot be longer than 25 characters")
        private String reason = "";
        @Length(max = 40, message = "location cannot be longer than 40 characters")
        private String location = "";
        @Length(max = 60, message = "customText cannot be longer than 60 characters")
        private String customText = "";

        private boolean greenTick;
        private boolean changesAllowed;
        @NotNull(message = "timestamp cannot be null")
        private Timestamp timestamp;
        private boolean enableLtv;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timestamp {
        private boolean enabled;
        @NotNull(message = "url cannot be null")
        @AssertURL(message = "url must be a valid URL")
        private String url;
        private String username;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pdf {
        @NotEmpty(message = "base64Content cannot be empty")
        private String base64Content;
        @NotNull(message = "password cannot be null")
        private String password;
    }
}