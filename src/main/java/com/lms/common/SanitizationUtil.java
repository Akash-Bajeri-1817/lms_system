package com.lms.common;

import org.springframework.stereotype.Component;

@Component
public class SanitizationUtil {

    // removes HTML tags to prevent XSS attacks
    // e.g. "<script>alert('xss')</script>Hello" → "Hello"
    public String sanitize(String input) {
        if (input == null) return null;
        return input
                .replaceAll("<[^>]*>", "")           // remove HTML tags
                .replaceAll("javascript:", "")        // remove JS injection
                .replaceAll("on\\w+=\"[^\"]*\"", "") // remove event handlers
                .trim();
    }
}