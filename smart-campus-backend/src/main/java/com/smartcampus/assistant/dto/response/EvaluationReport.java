package com.smartcampus.assistant.dto.response;

import java.util.List;

/**
 * AI 探索助手评测报告（M6，方案 §8）。
 *
 * <ul>
 *   <li>{@code avgRelevance}：回答切题度（LLM-as-judge，0~1）。</li>
 *   <li>{@code avgGroundedness}：回答中推荐地点是否都来自检索候选（防编造/越界，0~1）。</li>
 * </ul>
 */
public record EvaluationReport(
        int cases,
        double avgRelevance,
        double avgGroundedness,
        List<CaseResult> details
) {

    public record CaseResult(
            String query,
            int candidateCount,
            String answerPreview,
            double relevance,
            double groundedness,
            String note
    ) {
    }
}
