package com.lucky.fortune.domain;

import java.time.OffsetDateTime;

/**
 * 사주 컨텐츠(운세) 한 건. fortunes 테이블 매핑.
 *
 * <p>teaserPrompt / fullPrompt 는 <b>서버 전용</b> 필드다.
 * 절대 공개 API 응답에 직렬화하지 말 것 → 공개로 내보낼 땐 {@link com.lucky.fortune.dto.FortunePublic} 사용.
 */
public class Fortune {

    private Long id;
    private String slug;          // 폴더명/상품코드 (결제·생성 키)
    private String title;
    private String description;
    private String durationText;  // 예: "약 3분"
    private long price;           // 원
    private boolean active;       // 노출 on/off
    private String category;      // 메인 탭 분류 (연애/재회/결혼/가정/기타)
    private OffsetDateTime createdAt;  // 등록일 (NEW 뱃지: 7일 이내)

    private String teaserPrompt;  // 무료 맛보기용 (서버 전용)
    private String fullPrompt;    // 유료 풀 리포트용 (서버 전용)

    /**
     * 공용 입력폼/미리보기 구성 JSON (공개 가능).
     * {questions:[{key,label,type:single|multi|text,options?,placeholder?,required?}], concerns:[..4], chapters:[{no,title,sub}]}
     */
    private String uiConfig;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDurationText() {
        return durationText;
    }

    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTeaserPrompt() {
        return teaserPrompt;
    }

    public void setTeaserPrompt(String teaserPrompt) {
        this.teaserPrompt = teaserPrompt;
    }

    public String getFullPrompt() {
        return fullPrompt;
    }

    public void setFullPrompt(String fullPrompt) {
        this.fullPrompt = fullPrompt;
    }

    public String getUiConfig() {
        return uiConfig;
    }

    public void setUiConfig(String uiConfig) {
        this.uiConfig = uiConfig;
    }
}
