package com.ecom.app.e2e.support;

public final class TestConfig {

    public static final String BASE_URL = resolveBaseUrl();

    private TestConfig() {
    }

    private static String resolveBaseUrl() {
        String fromProperty = System.getProperty("APP_BASE_URL");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return stripTrailingSlash(fromProperty);
        }
        String fromEnv = System.getenv("APP_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return stripTrailingSlash(fromEnv);
        }
        return "http://localhost:5173";
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
