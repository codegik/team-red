package com.electromart;

public final class Env {

    private Env() {
    }

    public static String get(String key, String def) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? def : value;
    }
}
