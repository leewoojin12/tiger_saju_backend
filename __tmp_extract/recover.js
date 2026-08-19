const fs = require("fs");

// 1) schema: payments.input_json (결제 준비 시 입력 스냅샷 → 이탈해도 서버가 복구 생성 가능)
const sp = "src/main/resources/schema.sql";
let s = fs.readFileSync(sp, "utf8");
if (!s.includes("payments ADD COLUMN IF NOT EXISTS input_json")) {
  s += `
-- 결제 준비 시 사용자의 입력(subjects/answers) 스냅샷.
-- 결제 후 브라우저를 닫아도 서버가 리포트를 만들어 줄 수 있게 한다(미수령 결제 복구).
ALTER TABLE payments ADD COLUMN IF NOT EXISTS input_json TEXT;
`;
  fs.writeFileSync(sp, s);
}
console.log("schema:", s.includes("input_json"));

// 2) Payment 도메인 필드
const dp = "src/main/java/com/lucky/payment/domain/Payment.java";
let d = fs.readFileSync(dp, "utf8");
if (!d.includes("inputJson")) {
  d = d.replace("    private String rawJson;", "    private String rawJson;\n\n    /** 결제 준비 시 저장한 사용자 입력(JSON). 미수령 결제 복구 생성에 사용. */\n    private String inputJson;");
  fs.writeFileSync(dp, d);
}
console.log("domain:", d.includes("inputJson"), "| lombok:", d.includes("@Data") || d.includes("@Getter"));
