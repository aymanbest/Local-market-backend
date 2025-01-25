package com.localmarket.main.dto.product;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProducerProductsResponse {
    private Long producerId;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private List<ProductResponse> products;
} 