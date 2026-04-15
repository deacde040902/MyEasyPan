package com.easypan.entity.vo;

import lombok.Data;
import java.util.Date;

@Data
public class VipStatusVO {
    private Integer vipLevel;
    private Date vipExpireTime;
    private boolean effectiveVip;
    private Long totalSpace;
    private Long useSpace;
}