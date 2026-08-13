package com.lucky.fortune.saju;

/** 오행(五行). hanja는 결과 JSON의 orbs.el 값으로 그대로 사용(火/木/土/水/金). */
enum Ohaeng {
    MOK("木"), HWA("火"), TO("土"), GEUM("金"), SU("水");

    private final String hanja;

    Ohaeng(String hanja) {
        this.hanja = hanja;
    }

    String hanja() {
        return hanja;
    }
}
