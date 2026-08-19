const fs = require("fs");

// mapper xml: input_json 매핑 + insert 포함 + 미수령 결제 조회
const xp = "src/main/resources/mapper/PaymentMapper.xml";
let x = fs.readFileSync(xp, "utf8");
x = x.replace(
  '        <result property="rawJson" column="raw_json"/>',
  '        <result property="rawJson" column="raw_json"/>\n        <result property="inputJson" column="input_json"/>',
);
x = x.replace(
  `            (payment_id, member_id, product_code, order_name, amount, currency, status)
        VALUES
            (#{paymentId}, #{memberId}, #{productCode}, #{orderName}, #{amount}, #{currency}, #{status})`,
  `            (payment_id, member_id, product_code, order_name, amount, currency, status, input_json)
        VALUES
            (#{paymentId}, #{memberId}, #{productCode}, #{orderName}, #{amount}, #{currency}, #{status}, #{inputJson})`,
);
if (!x.includes("findUnfulfilledByMember")) {
  x = x.replace(
    "</mapper>",
    `    <!--
      결제는 됐는데(PAID) 리포트 행이 없는 결제건.
      결제 직후 브라우저를 닫는 등으로 생성 요청이 유실된 경우를 보관함에서 복구하기 위한 조회.
    -->
    <select id="findUnfulfilledByMember" resultMap="paymentResultMap">
        SELECT p.* FROM payments p
        LEFT JOIN fortune_results r ON r.payment_id = p.payment_id
        WHERE p.member_id = #{memberId}
          AND p.status = 'PAID'
          AND r.id IS NULL
        ORDER BY p.paid_at DESC NULLS LAST, p.id DESC
    </select>

</mapper>`,
  );
}
fs.writeFileSync(xp, x);
console.log("xml:", x.includes("findUnfulfilledByMember"), x.includes("input_json"));

// mapper interface
const mp = "src/main/java/com/lucky/payment/mapper/PaymentMapper.java";
let m = fs.readFileSync(mp, "utf8");
if (!m.includes("findUnfulfilledByMember")) {
  m = m.replace(
    /\}\s*$/,
    `
    /** 결제 완료(PAID)됐지만 리포트가 만들어지지 않은 결제건(최신순). */
    java.util.List<com.lucky.payment.domain.Payment> findUnfulfilledByMember(@Param("memberId") Long memberId);
}
`,
  );
  fs.writeFileSync(mp, m);
}
console.log("mapper:", m.includes("findUnfulfilledByMember"));
