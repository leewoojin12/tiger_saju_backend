package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사용자/사주 퍼널 스칼라 묶음 (한 행). */
@Getter
@Setter
@NoArgsConstructor
public class FunnelRow {
    private long totalMembers;       // 전체 회원
    private long payingMembers;      // PAID 1건 이상 (distinct)
    private long repurchaseMembers;  // PAID 2건 이상 (distinct)
    private long freeSajuUsers;      // 무료 사주 이용 (distinct, saju_history)
    private long freeSajuPaid;       // 무료 사주 + PAID 둘 다 (distinct)
}
