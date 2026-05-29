package com.lms.search.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SearchRequest {

    private String query;           // search term

    // filters
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private Boolean freeOnly;

    // pagination
    private int page = 0;
    private int size = 10;

    // sorting
    private String sortBy = "relevance";  // relevance, price, rating, newest
}