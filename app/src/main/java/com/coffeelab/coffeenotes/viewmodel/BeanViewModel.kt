package com.coffeelab.coffeenotes.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.CoffeeBean
import com.coffeelab.coffeenotes.data.entity.FlavorTag
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

    // 烘焙度/处理法列表（用于下拉选）
    val allRoastDegrees = repository.allRoastDegrees
    val allProcessMethods = repository.allProcessMethods

    private val _selectedBean = MutableStateFlow<CoffeeBean?>(null)
    val selectedBean: StateFlow<CoffeeBean?> = _selectedBean.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

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

    fun loadTags(beanId: Long) {
        viewModelScope.launch {
            repository.getTagsForBean(beanId).collect { tagEntities ->
                _tags.value = tagEntities.map { it.name }
            }
        }
    }

    suspend fun saveBeanSync(bean: CoffeeBean, tagList: List<String>): Long {
        val id = repository.insertBean(bean)
        if (id > 0) {
            repository.saveTagsForBean(id, tagList)
        }
        return id
    }

    suspend fun updateBeanSync(bean: CoffeeBean, tagList: List<String>) {
        repository.updateBean(bean)
        repository.saveTagsForBean(bean.id, tagList)
    }

    fun deleteBean(bean: CoffeeBean) {
        viewModelScope.launch {
            repository.deleteBean(bean)
        }
    }

    fun archiveBean(bean: CoffeeBean) {
        viewModelScope.launch {
            repository.archiveBean(bean)
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

    fun runKeywordRecognition(bitmap: Bitmap) {
        viewModelScope.launch {
            _recognitionProcessing.value = true
            try {
                val result = keywordEngine.recognize(bitmap)
                _recognitionResult.value = result
            } catch (e: Exception) {
                _recognitionResult.value = RecognitionResult(success = false, rawResponse = "识别出错: ${e.message}", engineName = "本地关键词")
            } finally {
                _recognitionProcessing.value = false
            }
        }
    }

    fun clearRecognitionResult() {
        _recognitionResult.value = null
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
