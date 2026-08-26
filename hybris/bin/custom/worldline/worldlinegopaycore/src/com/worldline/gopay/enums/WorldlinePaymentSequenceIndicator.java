package com.worldline.gopay.enums;

public enum WorldlinePaymentSequenceIndicator {
    FIRST("first"), SEQUENCE("sequence");
    String name;

    WorldlinePaymentSequenceIndicator(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
