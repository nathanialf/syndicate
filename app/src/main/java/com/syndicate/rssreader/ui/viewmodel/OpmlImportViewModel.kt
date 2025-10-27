package com.syndicate.rssreader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syndicate.rssreader.data.models.OpmlImportResult
import com.syndicate.rssreader.data.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class OpmlImportViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {
    
    private val _importState = MutableStateFlow<OpmlImportState>(OpmlImportState.Idle)
    val importState: StateFlow<OpmlImportState> = _importState.asStateFlow()
    
    private val _showFileDialog = MutableStateFlow(false)
    val showFileDialog: StateFlow<Boolean> = _showFileDialog.asStateFlow()
    
    private val _showPreviewDialog = MutableStateFlow(false)
    val showPreviewDialog: StateFlow<Boolean> = _showPreviewDialog.asStateFlow()
    
    private val _importResult = MutableStateFlow<OpmlImportResult?>(null)
    val importResult: StateFlow<OpmlImportResult?> = _importResult.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun showFileDialog() {
        _showFileDialog.value = true
    }
    
    fun hideFileDialog() {
        _showFileDialog.value = false
    }
    
    fun hidePreviewDialog() {
        _showPreviewDialog.value = false
        _importResult.value = null
    }
    
    fun parseOpmlFile(inputStream: InputStream) {
        viewModelScope.launch {
            _importState.value = OpmlImportState.Parsing
            _errorMessage.value = null
            
            try {
                val result = repository.importOpml(inputStream)
                _importResult.value = result
                _importState.value = OpmlImportState.Preview
                _showPreviewDialog.value = true
                _showFileDialog.value = false
            } catch (e: Exception) {
                _importState.value = OpmlImportState.Error
                _errorMessage.value = "Failed to parse OPML file: ${e.message}"
            }
        }
    }
    
    fun executeImport(skipDuplicates: Boolean = true) {
        val result = _importResult.value ?: return
        
        viewModelScope.launch {
            _importState.value = OpmlImportState.Importing
            _showPreviewDialog.value = false  // Close preview dialog when importing starts
            _errorMessage.value = null
            
            try {
                val importResult = repository.executeOpmlImport(result, skipDuplicates)
                
                if (importResult.isSuccess) {
                    _importState.value = OpmlImportState.Success(importResult.getOrNull() ?: "Import completed")
                } else {
                    _importState.value = OpmlImportState.Error
                    _errorMessage.value = importResult.exceptionOrNull()?.message ?: "Import failed"
                }
            } catch (e: Exception) {
                _importState.value = OpmlImportState.Error
                _errorMessage.value = "Import failed: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
        if (_importState.value is OpmlImportState.Error) {
            _importState.value = OpmlImportState.Idle
        }
    }
    
    fun resetState() {
        _importState.value = OpmlImportState.Idle
        _importResult.value = null
        _errorMessage.value = null
        _showPreviewDialog.value = false
        _showFileDialog.value = false
    }
}

sealed class OpmlImportState {
    object Idle : OpmlImportState()
    object Parsing : OpmlImportState()
    object Preview : OpmlImportState()
    object Importing : OpmlImportState()
    data class Success(val message: String) : OpmlImportState()
    object Error : OpmlImportState()
}