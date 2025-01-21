package com.localmarket.main.util;

import org.apache.commons.text.StringEscapeUtils;
import java.util.regex.Pattern;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;

public class InputValidator {
    private static final Pattern SAFE_STRING = Pattern.compile("^[a-zA-Z0-9\\s\\-_,.&()]+$");
    
    public static String sanitizeCategory(String category) {
        if (category == null) return null;
        
        // HTML escape
        String escaped = StringEscapeUtils.escapeHtml4(category.trim());
        
        // Check if the category matches safe pattern
        if (!SAFE_STRING.matcher(escaped).matches()) {
            throw new ApiException(ErrorType.INVALID_REQUEST, "Category contains invalid characters");
        }
        
        return escaped;
    }
} 