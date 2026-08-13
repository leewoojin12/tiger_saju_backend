package com.lucky.stats.dto;

import java.util.List;

/**
 * 어드민 통계 대시보드 응답. 매출/콘텐츠/사용자/사주보관함 4영역.
 * 모든 수치는 실제 DB 집계 (매출은 status='PAID', paid_at KST 기준).
 */
public record StatsResponse(
        Summary summary,
        // 매출·결제
        List<DailyPoint> dailyRevenue,        // 일별 매출(30일)
        List<LabelCount> paymentStatus,       // 결제 상태 분포
        List<LabelCount> payMethods,          // 결제수단 분포(PAID)
        List<ContentSale> contentSales,       // 콘텐츠별 매출/건수(PAID, 매출순)
        // 콘텐츠
        List<LabelCount> contentGeneration,   // 생성 수 TOP (fortune_results)
        List<CategoryStat> categoryStats,     // 카테고리별 매출/생성
        List<LabelCount> generationStatus,    // 생성 상태(DONE/GENERATING/FAILED)
        List<ContentConversion> contentConversion, // 콘텐츠별 결제→생성 전환
        // 사용자
        List<CountPoint> dailySignups,        // 일별 신규가입(30일)
        List<LabelCount> roleDistribution,    // 관리자/일반
        // 사주 보관함
        List<CountPoint> dailySaju,           // 일별 무료사주(30일)
        List<LabelCount> genderDistribution,
        List<LabelCount> calendarDistribution,
        List<LabelCount> timeDistribution
) {
    /** KPI 요약. */
    public record Summary(
            long totalRevenue,
            long paidCount,
            long avgPaid,            // = totalRevenue/paidCount
            double successRate,      // PAID / 전체 결제행
            long memberCount,
            long totalPayments,
            long payingMembers,      // PAID 1건 이상
            long repurchaseMembers,  // PAID 2건 이상
            long arpu,               // = totalRevenue/memberCount (회원당)
            long arppu,              // = totalRevenue/payingMembers (결제회원당)
            long freeSajuUsers,      // 무료 사주 이용자(distinct)
            long freeSajuPaid        // 무료→유료 전환자(distinct)
    ) {
    }
}
