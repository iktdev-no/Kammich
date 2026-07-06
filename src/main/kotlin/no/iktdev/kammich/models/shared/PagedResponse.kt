package no.iktdev.kammich.models.shared

data class PagedResponse<T>(
    val data: List<T>,
    val totalPages: Int,
    val currentPage: Int,
    val hasMore: Boolean
)