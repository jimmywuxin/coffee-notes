package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.entity.ImpressionTag
import com.coffeelab.coffeenotes.data.entity.FlavorTag
import com.coffeelab.coffeenotes.util.BitmapLoader
import com.coffeelab.coffeenotes.data.entity.PeakFlavorConfig
import com.coffeelab.coffeenotes.data.entity.RestPeriodConfig
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import com.coffeelab.coffeenotes.util.ImageUtils
import com.coffeelab.coffeenotes.util.OCRProcessor
import com.coffeelab.coffeenotes.util.OCRResult
import com.coffeelab.coffeenotes.util.engine.KeywordRecognitionEngine
import com.coffeelab.coffeenotes.util.engine.RecognitionResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BeanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CoffeeRepository(AppDatabase.getInstance(application))
    val allBeans = repository.allBeans
    val activeBeans = repository.activeBeans
    val archivedBeans = repository.archivedBeans

    // 豆子列表筛选状态
    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    fun setShowFavoritesOnly(value: Boolean) {
        _showFavoritesOnly.value = value
    }

    private val _showArchivedOnly = MutableStateFlow(false)
    val showArchivedOnly: StateFlow<Boolean> = _showArchivedOnly.asStateFlow()

    fun setShowArchivedOnly(value: Boolean) {
        _showArchivedOnly.value = value
    }

    // ===== Search =====
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isSearching: StateFlow<Boolean> = _searchQuery.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val searchResults: StateFlow<List<CoffeeBean>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.activeBeans
            } else {
                repository.searchBeansFull(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // 烘焙度/处理法列表（用于下拉选）
    val allRoastDegrees = repository.allRoastDegrees
    val allProcessMethods = repository.allProcessMethods

    private val _selectedBean = MutableStateFlow<CoffeeBean?>(null)
    val selectedBean: StateFlow<CoffeeBean?> = _selectedBean.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _impressionTags = MutableStateFlow<List<ImpressionTag>>(emptyList())
    val impressionTags: StateFlow<List<ImpressionTag>> = _impressionTags.asStateFlow()

    val allImpressionTags: StateFlow<List<ImpressionTag>> = repository.allImpressionTags
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 赏味期即将结束的豆子（距离赏味期结束 <= 15天，即提前15天开始倒计时），按倒计时升序排列
    // 赏味期结束日 = 烘焙日 + 赏味期天数
    val beansNearingPeakFlavorEnd: StateFlow<List<Pair<CoffeeBean, Int>>> = combine(
        repository.activeBeans,
        repository.allRoastDegrees,
        repository.allPeakFlavorConfigs,
        repository.allRestPeriodConfigs
    ) { beans, roastDegrees, peakConfigs, _ ->
        val now = System.currentTimeMillis()
        val fifteenDaysFromNow = now + 15L * 24 * 60 * 60 * 1000
        beans.filter { bean ->
            bean.roastDate != null
        }.mapNotNull { bean ->
            val peakDays = bean.peakFlavorDays
                ?: roastDegrees.find { it.name == bean.roastLevel }?.let { rd ->
                    peakConfigs.find { it.roastDegreeId == rd.id }?.peakFlavorDays
                }
                ?: 14  // 赏味期默认 14 天
            val peakEndDate = bean.roastDate!! + (peakDays * 86400000L)
            if (peakEndDate in now..fifteenDaysFromNow) {
                val daysLeft = ((peakEndDate - now) / (24 * 60 * 60 * 1000)).toInt()
                bean to daysLeft
            } else null
        }.sortedBy { it.second }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadBean(beanId: Long) {
        viewModelScope.launch {
            val bean = repository.getBean(beanId)
            _selectedBean.value = bean
            if (bean != null) {
                val tagEntities = repository.getTagsForBeanOnce(beanId)
                _tags.value = tagEntities.map { it.name }
            }
        }
    }

    fun loadImpressionTags(beanId: Long) {
        viewModelScope.launch {
            repository.getImpressionTagsForBean(beanId).collect { tags ->
                _impressionTags.value = tags
            }
        }
    }

    fun loadTags(beanId: Long) {
        viewModelScope.launch {
            repository.getTagsForBean(beanId).collect { tagEntities ->
                _tags.value = tagEntities.map { it.name }
            }
        }
    }

    suspend fun saveBeanSync(bean: CoffeeBean, tagList: List<String>, impressionTagIds: List<Long>): Long {
        val id = repository.insertBean(bean)
        if (id > 0) {
            repository.saveTagsForBean(id, tagList)
            repository.saveImpressionTagsForBean(id, impressionTagIds)
        }
        return id
    }

    suspend fun updateBeanSync(bean: CoffeeBean, tagList: List<String>, impressionTagIds: List<Long>) {
        repository.updateBean(bean)
        repository.saveTagsForBean(bean.id, tagList)
        repository.saveImpressionTagsForBean(bean.id, impressionTagIds)
    }

    fun deleteBean(bean: CoffeeBean) {
        viewModelScope.launch {
            repository.deleteBean(bean)
        }
    }

    fun archiveBean(bean: CoffeeBean, clearStock: Boolean = false) {
        viewModelScope.launch {
            repository.archiveBean(bean, clearStock)
        }
    }

    fun unarchiveBean(bean: CoffeeBean) {
        viewModelScope.launch {
            repository.unarchiveBean(bean)
        }
    }

    fun toggleFavorite(bean: CoffeeBean) {
        viewModelScope.launch {
            repository.updateBean(bean.copy(isFavorite = !bean.isFavorite))
        }
    }

    fun saveBeanOrder(items: List<CoffeeBean>) {
        viewModelScope.launch {
            repository.saveBeanOrder(items)
        }
    }


    suspend fun addImpressionTag(name: String): Long {
        val maxOrder = repository.getMaxImpressionTagSortOrder() ?: 0
        return repository.insertImpressionTag(ImpressionTag(name = name, sortOrder = maxOrder + 1))
    }

    fun searchBeans(query: String): Flow<List<CoffeeBean>> = repository.searchBeans(query)

    // 根据烘焙度ID查养豆期/赏味期配置
    suspend fun getRestPeriodConfigByRoastDegreeId(roastDegreeId: Long): RestPeriodConfig? =
        repository.getRestPeriodConfigByRoastDegreeId(roastDegreeId)
    suspend fun getPeakFlavorConfigByRoastDegreeId(roastDegreeId: Long): PeakFlavorConfig? =
        repository.getPeakFlavorConfigByRoastDegreeId(roastDegreeId)

    // ===== Recognition (pluggable engine) =====
    private val keywordEngine = KeywordRecognitionEngine()

    private val _recognitionResult = MutableStateFlow<RecognitionResult?>(null)
    val recognitionResult: StateFlow<RecognitionResult?> = _recognitionResult.asStateFlow()

    private val _recognitionProcessing = MutableStateFlow(false)
    val recognitionProcessing: StateFlow<Boolean> = _recognitionProcessing.asStateFlow()

    private val _blurWarning = MutableStateFlow(false)
    val blurWarning: StateFlow<Boolean> = _blurWarning.asStateFlow()
    fun clearBlurWarning() { _blurWarning.value = false }


    fun runKeywordRecognition(bitmap: Bitmap) {
        viewModelScope.launch {
            _recognitionProcessing.value = true
            _blurWarning.value = false
            try {
                val blurScore = BitmapLoader.detectBlur(bitmap)
                if (blurScore < 60f) {
                    _blurWarning.value = true
                }
                // 拉取历史 OCR 纠错，传给引擎做后处理替换
                val corrections = AppDatabase.getInstance(getApplication())
                    .ocrCorrectionDao().getAllOnce()
                val result = keywordEngine.recognize(bitmap, corrections)
                _recognitionResult.value = result
            } catch (e: Exception) {
                _recognitionResult.value = RecognitionResult(success = false, rawResponse = "识别出错: ${e.message}", engineName = "本地关键词")
            } finally {
                _recognitionProcessing.value = false
            }
        }
    }

    /**
     * 保存豆子时记录 OCR 纠错回流。
     * @param beanId 豆子 id（新建取 saveBeanSync 返回值，编辑用 bean.id）
     * @param ocrSnapshot OCR 填入表单时的原始值快照（field -> ocrRaw）
     * @param finalValues 保存时表单的最终值（field -> userValue）
     */
    suspend fun recordOcrCorrections(
        beanId: Long,
        ocrSnapshot: Map<String, String>,
        finalValues: Map<String, String>
    ) {
        if (ocrSnapshot.isEmpty()) return
        val dao = AppDatabase.getInstance(getApplication()).ocrCorrectionDao()
        val now = System.currentTimeMillis()
        ocrSnapshot.forEach { (field, ocrRaw) ->
            if (ocrRaw.isBlank()) return@forEach
            val userValue = finalValues[field] ?: return@forEach
            if (userValue.isBlank()) return@forEach
            // 没改不记；归一化后相等也不记
            if (normalizeForCompare(ocrRaw) == normalizeForCompare(userValue)) return@forEach
            runCatching { dao.upsert(field, ocrRaw, userValue, beanId, now) }
        }
    }

    private fun normalizeForCompare(s: String): String =
        s.trim().lowercase().replace(Regex("\\s+"), " ")

    fun clearRecognitionResult() {
        _recognitionResult.value = null
        _blurWarning.value = false
    }

    // OCR Processing
    private val _ocrResult = MutableStateFlow<OCRResult?>(null)
    val ocrResult: StateFlow<OCRResult?> = _ocrResult.asStateFlow()

    private val _ocrProcessing = MutableStateFlow(false)
    val ocrProcessing: StateFlow<Boolean> = _ocrProcessing.asStateFlow()

    fun processImageForOCR(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrProcessing.value = true
            try {
                val result = OCRProcessor.processBitmap(bitmap)
                _ocrResult.value = result
            } catch (e: Exception) {
                _ocrResult.value = OCRResult(fullText = "识别出错: ${e.message}")
            } finally {
                _ocrProcessing.value = false
            }
        }
    }

    fun clearOCRResult() {
        _ocrResult.value = null
    }

    // Image saving
    fun saveImage(bitmap: Bitmap): String {
        return ImageUtils.saveBitmapToFile(getApplication(), bitmap)
    }
}
