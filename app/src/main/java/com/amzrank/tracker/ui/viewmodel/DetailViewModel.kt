package com.amzrank.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amzrank.tracker.data.local.entity.AsinItem
import com.amzrank.tracker.data.local.entity.RankRecord
import com.amzrank.tracker.data.repository.RankRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val asin: String,
    private val repository: RankRepository
) : ViewModel() {

    val asinItem: StateFlow<AsinItem?> = repository.getAsinFlow(asin)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allRecords: StateFlow<List<RankRecord>> = repository.getRecordsForAsinFlow(asin)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDays = MutableStateFlow(30) // 默认查看 30 天
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()

    private val _showSubRank = MutableStateFlow(false)
    val showSubRank: StateFlow<Boolean> = _showSubRank.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun setSelectedDays(days: Int) {
        _selectedDays.value = days
    }

    fun setShowSubRank(show: Boolean) {
        _showSubRank.value = show
    }

    fun refreshNow() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncSingleAsin(asin)
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val asin: String,
        private val repository: RankRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(asin, repository) as T
        }
    }
}
