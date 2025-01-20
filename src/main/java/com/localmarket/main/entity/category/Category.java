package com.localmarket.main.entity.category;

import java.util.Set;
import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import jakarta.persistence.FetchType;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.localmarket.main.entity.product.Product;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Data
@Table(name = "Category")
@EqualsAndHashCode(exclude = {"products"})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    
    private String name;
    
    @JsonIgnore
    @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Product> products = new HashSet<>();
    
    @JsonProperty("productCount")
    public int getProductCount() {
        return products.size();
    }
} 