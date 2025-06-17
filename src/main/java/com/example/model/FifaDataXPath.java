package com.example.model;

public enum FifaDataXPath {
    YEAR("//*[@id=\"mw-content-text\"]/div[1]/table[4]/tbody/tr[%d]/th/a"),
    WINNER("//*[@id=\"mw-content-text\"]/div[1]/table[4]/tbody/tr[%d]/td[1]/a"),
    SCORE("//*[@id=\"mw-content-text\"]/div[1]/table[4]/tbody/tr[%d]/td[2]/a[1]"),
    RUNNER_UP("//*[@id=\"mw-content-text\"]/div[1]/table[4]/tbody/tr[%d]/td[3]/span/a");

    private final String pattern;

    FifaDataXPath(String pattern) {
        this.pattern = pattern;
    }
    public String getFormattedXPath(int index) {
        return String.format(pattern, index);
    }
}