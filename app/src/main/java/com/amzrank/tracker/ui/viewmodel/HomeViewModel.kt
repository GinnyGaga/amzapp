package com.amzrank.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.amzrank.tracker.data.local.entity.AsinItem
import com.amzrank.tracker.data.repository.RankRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: RankRepository) : ViewModel() {

    val asins: StateFlow<List<AsinItem>> = repository.allAsinsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgressText = MutableStateFlow<String?>(null)
    val syncProgressText: StateFlow<String?> = _syncProgressText.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addAsin(rawInput: String, alias: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgressText.value = "正在添加并初始化抓取..."
            val result = repository.addAsin(rawInput, alias)
            _isSyncing.value = false
            _syncProgressText.value = null

            result.onSuccess { asin ->
                _userMessage.value = "成功添加监控 ASIN: $asin"
            }.onFailure { e ->
                _userMessage.value = "添加失败: ${e.message}"
            }
        }
    }

    fun addAsinsBatch(rawInput: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgressText.value = "正在批量添加商品..."
            val added = repository.addAsinsBatch(rawInput)
            _isSyncing.value = false
            _syncProgressText.value = null

            if (added.isNotEmpty()) {
                _userMessage.value = "成功批量导入 ${added.size} 个 ASIN，开始更新排名..."
                syncAll()
            } else {
                _userMessage.value = "未从输入内容中识别出有效的 10 位 ASIN"
            }
        }
    }

    fun syncAll() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val summary = repository.syncAllActiveAsins { curr, total, asin ->
                _syncProgressText.value = "正在更新 ($curr/$total): $asin"
            }
            _isSyncing.value = false
            _syncProgressText.value = null

            if (summary.captchaCount > 0) {
                _userMessage.value = "完成更新：${summary.successCount} 件成功，有 ${summary.captchaCount} 件遇到人机验证"
            } else {
                _userMessage.value = "更新完毕：共 ${summary.total} 件，成功 ${summary.successCount} 件"
            }
        }
    }

    fun syncSingle(asin: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgressText.value = "正在刷新 $asin..."
            repository.syncSingleAsin(asin)
            _isSyncing.value = false
            _syncProgressText.value = null
        }
    }

    fun deleteAsin(asin: String) {
        viewModelScope.launch {
            repository.deleteAsin(asin)
            _userMessage.value = "已移除监控 $asin"
        }
    }

    fun toggleActive(asin: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleAsinActive(asin, isActive)
        }
    }

    class Factory(private val repository: RankRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
