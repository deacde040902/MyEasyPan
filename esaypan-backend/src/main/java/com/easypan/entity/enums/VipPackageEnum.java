package com.easypan.entity.enums;

public enum VipPackageEnum {
    MONTH("月度VIP", 1, 20.0),
    QUARTER("季度VIP", 3, 50.0),
    YEAR("年度VIP", 12, 180.0);

    private final String name;
    private final int months;
    private final double price;

    VipPackageEnum(String name, int months, double price) {
        this.name = name;
        this.months = months;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getMonths() {
        return months;
    }

    public double getPrice() {
        return price;
    }
}