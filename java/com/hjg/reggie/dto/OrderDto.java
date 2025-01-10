package com.hjg.reggie.dto;

import com.hjg.reggie.entity.OrderDetail;
import com.hjg.reggie.entity.Orders;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto extends Orders {

    private List<OrderDetail> orderDetails;
}
