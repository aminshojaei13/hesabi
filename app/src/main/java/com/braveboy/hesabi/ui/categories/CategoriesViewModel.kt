package com.braveboy.hesabi.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.domain.repository.CategoryRepository
import com.braveboy.hesabi.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoriesUiState(
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val categories: List<Category> = emptyList(),
    val message: String? = null,
    val error: String? = null,
    val loading: Boolean = true,
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val kind = MutableStateFlow(CategoryKind.EXPENSE)
    private val notice = MutableStateFlow<Pair<String?, String?>>(null to null)

    val state: StateFlow<CategoriesUiState> = combine(
        categoryRepository.observeAll(),
        kind,
        notice,
    ) { categories, currentKind, message ->
        CategoriesUiState(
            kind = currentKind,
            categories = categories.filter { it.kind == currentKind }.sortedBy { it.sortOrder },
            message = message.first,
            error = message.second,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun setKind(kind: CategoryKind) {
        this.kind.value = kind
    }

    fun save(
        id: String?,
        name: String,
        kind: CategoryKind,
        iconKey: String,
        colorArgb: Long,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            notice.value = null to "نام دسته را بنویس."
            return
        }
        viewModelScope.launch {
            val all = categoryRepository.getAll()
            val existing = id?.let { categoryRepository.getById(it) }
            val category = Category(
                id = existing?.id ?: "cat_" + UUID.randomUUID().toString().take(8),
                name = cleanName,
                kind = kind,
                iconKey = iconKey,
                colorArgb = colorArgb,
                archived = existing?.archived ?: false,
                sortOrder = existing?.sortOrder ?: (all.count { it.kind == kind }),
            )
            categoryRepository.save(category)
            notice.value = if (existing == null) "دسته ساخته شد." to null else "دسته به‌روز شد." to null
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            val usage = transactionRepository.countForCategory(category.id)
            if (usage > 0) {
                notice.value = null to "این دسته در ${usage} تراکنش استفاده شده و قابل حذف نیست."
            } else {
                categoryRepository.delete(category.id)
                notice.value = "دسته حذف شد." to null
            }
        }
    }

    fun dismissNotice() {
        notice.value = null to null
    }
}
