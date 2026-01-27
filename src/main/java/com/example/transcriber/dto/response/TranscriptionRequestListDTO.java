package com.example.transcriber.dto.response;

import java.util.List;

public class TranscriptionRequestListDTO {
    private List<TranscriptionRequestDTO> data;
    private OverviewDTO overview;
    private PaginationDTO pagination;
    private FilterOptionsDTO filters;
    private List<SortingOptionDTO> sortingOptions;

    // Getters and setters
    public List<TranscriptionRequestDTO> getData() {
        return data;
    }

    public void setData(List<TranscriptionRequestDTO> data) {
        this.data = data;
    }

    public OverviewDTO getOverview() {
        return overview;
    }

    public void setOverview(OverviewDTO overview) {
        this.overview = overview;
    }

    public PaginationDTO getPagination() {
        return pagination;
    }

    public void setPagination(PaginationDTO pagination) {
        this.pagination = pagination;
    }

    public FilterOptionsDTO getFilters() {
        return filters;
    }

    public void setFilters(FilterOptionsDTO filters) {
        this.filters = filters;
    }

    public List<SortingOptionDTO> getSortingOptions() {
        return sortingOptions;
    }

    public void setSortingOptions(List<SortingOptionDTO> sortingOptions) {
        this.sortingOptions = sortingOptions;
    }

    public static class OverviewDTO {
        private Long total;
        private Long completed;
        private Long processing;
        private Long failed;
        private Long usedQuota;

        // Getters and setters
        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getCompleted() {
            return completed;
        }

        public void setCompleted(Long completed) {
            this.completed = completed;
        }

        public Long getProcessing() {
            return processing;
        }

        public void setProcessing(Long processing) {
            this.processing = processing;
        }

        public Long getFailed() {
            return failed;
        }

        public void setFailed(Long failed) {
            this.failed = failed;
        }

        public Long getUsedQuota() {
            return usedQuota;
        }

        public void setUsedQuota(Long usedQuota) {
            this.usedQuota = usedQuota;
        }
    }

    public static class PaginationDTO {
        private Integer page;
        private Integer perPage;
        private Long total;
        private Integer totalPages;

        // Getters and setters
        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getPerPage() {
            return perPage;
        }

        public void setPerPage(Integer perPage) {
            this.perPage = perPage;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Integer getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }
    }

    public static class FilterOptionsDTO {
        private List<LanguageDTO> languages;
        private List<CategoryDTO> categories;
        private List<String> statuses;

        // Getters and setters
        public List<LanguageDTO> getLanguages() {
            return languages;
        }

        public void setLanguages(List<LanguageDTO> languages) {
            this.languages = languages;
        }

        public List<CategoryDTO> getCategories() {
            return categories;
        }

        public void setCategories(List<CategoryDTO> categories) {
            this.categories = categories;
        }

        public List<String> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<String> statuses) {
            this.statuses = statuses;
        }
    }

    public static class LanguageDTO {
        private Long id;
        private String name;
        private String code;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class CategoryDTO {
        private String key;
        private String name;

        // Getters and setters
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SortingOptionDTO {
        private String key;
        private String displayName;
        private List<String> sortBy;

        // Getters and setters
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<String> getSortBy() {
            return sortBy;
        }

        public void setSortBy(List<String> sortBy) {
            this.sortBy = sortBy;
        }
    }
}
