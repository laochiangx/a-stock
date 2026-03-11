package com.example.stock.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 市场资讯DTO
 */
public class MarketNewsDTO {
    private Long id;
    private String time;
    private LocalDateTime dataTime;
    private String title;
    private String content;
    private List<String> subjects;
    private List<String> stocks;
    @com.fasterxml.jackson.annotation.JsonProperty("isRed")
    private boolean isRed;
    private String url;
    private String source;
    private List<TelegraphTagDTO> tags;
    private String sentimentResult;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public LocalDateTime getDataTime() {
        return dataTime;
    }

    public void setDataTime(LocalDateTime dataTime) {
        this.dataTime = dataTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<String> getStocks() {
        return stocks;
    }

    public void setStocks(List<String> stocks) {
        this.stocks = stocks;
    }

    public boolean isRed() {
        return isRed;
    }

    public void setRed(boolean red) {
        isRed = red;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<TelegraphTagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TelegraphTagDTO> tags) {
        this.tags = tags;
    }

    public String getSentimentResult() {
        return sentimentResult;
    }

    public void setSentimentResult(String sentimentResult) {
        this.sentimentResult = sentimentResult;
    }

    public static class TelegraphTagDTO {
        private Long tagId;
        private Long telegraphId;

        // Getters and Setters
        public Long getTagId() {
            return tagId;
        }

        public void setTagId(Long tagId) {
            this.tagId = tagId;
        }

        public Long getTelegraphId() {
            return telegraphId;
        }

        public void setTelegraphId(Long telegraphId) {
            this.telegraphId = telegraphId;
        }
    }
}