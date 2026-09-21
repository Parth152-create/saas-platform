package com.yourco.saas.search;

import com.yourco.saas.search.dto.SearchResultDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SearchResultDto>> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "25") Integer limit) {
        return ResponseEntity.ok(searchService.search(q, type, limit));
    }
}
