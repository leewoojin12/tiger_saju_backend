package com.lucky.stats.service;

import com.lucky.stats.dto.FunnelRow;
import com.lucky.stats.dto.PaymentSummaryRow;
import com.lucky.stats.dto.StatsResponse;
import com.lucky.stats.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 통계 조립. 집계는 매퍼에 위임하고 평균/성공률/ARPU 등 파생값만 계산. */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    @Transactional(readOnly = true)
    public StatsResponse dashboard() {
        PaymentSummaryRow p = statsMapper.paymentSummary();
        FunnelRow f = statsMapper.funnel();

        long avgPaid = p.getPaidCount() == 0 ? 0 : p.getTotalRevenue() / p.getPaidCount();
        double successRate = p.getTotalCount() == 0
                ? 0.0
                : (double) p.getPaidCount() / p.getTotalCount();
        long arpu = f.getTotalMembers() == 0 ? 0 : p.getTotalRevenue() / f.getTotalMembers();
        long arppu = f.getPayingMembers() == 0 ? 0 : p.getTotalRevenue() / f.getPayingMembers();

        StatsResponse.Summary summary = new StatsResponse.Summary(
                p.getTotalRevenue(),
                p.getPaidCount(),
                avgPaid,
                successRate,
                f.getTotalMembers(),
                p.getTotalCount(),
                f.getPayingMembers(),
                f.getRepurchaseMembers(),
                arpu,
                arppu,
                f.getFreeSajuUsers(),
                f.getFreeSajuPaid()
        );

        return new StatsResponse(
                summary,
                statsMapper.dailyRevenue(),
                statsMapper.paymentStatusDistribution(),
                statsMapper.payMethodDistribution(),
                statsMapper.contentSales(),
                statsMapper.contentGeneration(),
                statsMapper.categoryStats(),
                statsMapper.generationStatus(),
                statsMapper.contentConversion(),
                statsMapper.dailySignups(),
                statsMapper.roleDistribution(),
                statsMapper.dailySaju(),
                statsMapper.genderDistribution(),
                statsMapper.calendarDistribution(),
                statsMapper.timeDistribution()
        );
    }
}
