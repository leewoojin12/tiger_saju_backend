package com.lucky.payment.mapper;

import com.lucky.payment.domain.Payment;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {

    void insert(Payment payment);

    Optional<Payment> findByPaymentId(@Param("paymentId") String paymentId);

    /** 이 회원이 이 컨텐츠(product_code)를 PAID 한 적 있는지. */
    boolean existsPaidByMemberAndProduct(@Param("memberId") Long memberId,
                                         @Param("productCode") String productCode);

    /** 검증 후 상태/결과 반영. */
    void updateResult(Payment payment);
}
