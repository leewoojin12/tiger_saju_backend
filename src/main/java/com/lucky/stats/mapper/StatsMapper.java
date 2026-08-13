package com.lucky.stats.mapper;

import com.lucky.stats.dto.CategoryStat;
import com.lucky.stats.dto.ContentConversion;
import com.lucky.stats.dto.ContentSale;
import com.lucky.stats.dto.CountPoint;
import com.lucky.stats.dto.DailyPoint;
import com.lucky.stats.dto.FunnelRow;
import com.lucky.stats.dto.LabelCount;
import com.lucky.stats.dto.PaymentSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 어드민 통계 집계. 매출은 status='PAID' + paid_at(KST) 기준. */
@Mapper
public interface StatsMapper {

    // 매출·결제
    PaymentSummaryRow paymentSummary();

    List<DailyPoint> dailyRevenue();

    List<LabelCount> paymentStatusDistribution();

    List<LabelCount> payMethodDistribution();

    List<ContentSale> contentSales();

    // 콘텐츠
    List<LabelCount> contentGeneration();

    List<CategoryStat> categoryStats();

    List<LabelCount> generationStatus();

    List<ContentConversion> contentConversion();

    // 사용자 / 퍼널
    long memberCount();

    FunnelRow funnel();

    List<CountPoint> dailySignups();

    List<LabelCount> roleDistribution();

    // 사주 보관함
    List<CountPoint> dailySaju();

    List<LabelCount> genderDistribution();

    List<LabelCount> calendarDistribution();

    List<LabelCount> timeDistribution();
}
