package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页的 ViewModel。
 *
 * 存在的意义：设置页原先直接 `AppDatabase.getInstance(...).xxxDao().deleteAll()`（审计 #5
 * 的 UI 直访 DAO），而且清空动作跑在 Compose 的 rememberCoroutineScope 里——那是随页面销毁
 * 一起取消的 scope，中途返回会留下「删了一半」的残库。现在收敛到 Repository（单事务）+
 * viewModelScope，页面销毁不会腰斩写入。
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))

    private val _clearing = MutableStateFlow(false)
    val clearing: StateFlow<Boolean> = _clearing.asStateFlow()

    /** 清空完成后置 true，由界面消费一次后调 [onClearHandled] 复位 */
    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    private val _clearFailed = MutableStateFlow(false)
    val clearFailed: StateFlow<Boolean> = _clearFailed.asStateFlow()

    fun clearAllData() {
        if (_clearing.value) return
        _clearing.value = true
        _clearFailed.value = false
        viewModelScope.launch {
            val ok = runCatching { repository.clearAllUserData() }.isSuccess
            _clearing.value = false
            if (ok) _cleared.value = true else _clearFailed.value = true
        }
    }

    /** 界面消费完「已清空」事件后复位，避免返回设置页时重复触发导航 */
    fun onClearHandled() {
        _cleared.value = false
    }

    fun onClearFailedHandled() {
        _clearFailed.value = false
    }
}
