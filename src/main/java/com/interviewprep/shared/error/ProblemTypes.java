package com.interviewprep.shared.error;

import java.net.URI;

public final class ProblemTypes {

    public static final String BASE = "https://be-interview-prep/problems/";

    private ProblemTypes() {}

    public static URI of(String slug) {
        return URI.create(BASE + slug);
    }
}
