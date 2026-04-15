package com.easypan.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("vip_order")
public class VipOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String orderId;

    private String userId;

    private String productName;

    private BigDecimal amount;

    private Byte payStatus; // 0未支付 1已支付

    private Date payTime;

    private Date createTime;

    private Date updateTime;

    private Integer months;
}