package com.example.util;

import java.util.Collections;
import java.util.List;

/**
 * Resultat pagine generique.
 *
 * <p>{@code totalPages} est calcule dans le constructeur a partir du nombre
 * total d'elements et de la taille de page : {@code ceil(totalItems / pageSize)}.</p>
 *
 * @param <T> type des elements de la page
 */
public class PaginationResult<T> {

    private final List<T> items;
    private final int currentPage;
    private final int pageSize;
    private final int totalPages;
    private final long totalItems;

    public PaginationResult(List<T> items, int currentPage, int pageSize, long totalItems) {
        this.items = items == null ? Collections.emptyList() : items;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
    }

    public List<T> getItems() { return items; }

    public int getCurrentPage() { return currentPage; }

    public int getPageSize() { return pageSize; }

    public int getTotalPages() { return totalPages; }

    public long getTotalItems() { return totalItems; }

    public boolean isFirstPage() { return currentPage <= 1; }

    public boolean isLastPage() { return currentPage >= totalPages; }

    public boolean isEmpty() { return items.isEmpty(); }

    @Override
    public String toString() {
        return "PaginationResult{page=" + currentPage + '/' + totalPages
                + ", pageSize=" + pageSize
                + ", totalItems=" + totalItems
                + ", items=" + items.size() + '}';
    }
}
