package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FifaFinalResult {
    private Integer year;
    private String winner;
    private String score;
    private String runnerUp;

    @Override
    public String toString() {
        return "Year=" + year + ", Winner='" + winner + '\'' + ", Score='" + score + '\'' + ", RunnerUp='" + runnerUp + '\'';
    }
}
