package com.campuscore.backend.lib;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PageParameters {
    private PageParameters() {}

    public static Pageable optional(Integer page, Integer size, String sortField) {
        if (page == null && size == null) return null;
        if (page == null || size == null || page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide page >= 0 and size between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, sortField));
    }

    public static Pageable optionalNewest(Integer page, Integer size, String sortField) {
        if (page == null && size == null) return null;
        if (page == null || size == null || page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide page >= 0 and size between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortField));
    }
}
