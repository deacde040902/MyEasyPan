package com.easypan.entity.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VipPackageVO {
    private String packageId;
    private String name;
    private Integer months;
    private Double price;
    private BigDecimal originalPrice;
    private String desc;
}