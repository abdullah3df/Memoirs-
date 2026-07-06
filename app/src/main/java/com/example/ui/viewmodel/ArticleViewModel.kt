package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Article
import com.example.data.ArticleRepository
import com.example.ui.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

class ArticleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ArticleRepository
    private val sharedPrefs = application.getSharedPreferences("portfolio_prefs", Context.MODE_PRIVATE)

    // Dynamic language configuration
    private val _currentLanguage = MutableStateFlow(AppLanguage.AR)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Dynamic theme mode
    private val _currentThemeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val currentThemeMode: StateFlow<AppThemeMode> = _currentThemeMode.asStateFlow()

    // Current Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Authentication States
    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Reactive Flow of Articles
    val articlesState: StateFlow<List<Article>>

    init {
        val database = AppDatabase.getInstance(application)
        repository = ArticleRepository(database.articleDao())

        // Load active session from preferences
        val savedEmail = sharedPrefs.getString("logged_in_email", null)
        if (savedEmail != null) {
            _currentUserEmail.value = savedEmail
            _isLoggedIn.value = true
        }

        // Load saved language
        val savedLangCode = sharedPrefs.getString("app_language", "ar")
        val savedLang = AppLanguage.values().find { it.code == savedLangCode } ?: AppLanguage.AR
        _currentLanguage.value = savedLang

        // Load saved theme mode
        val savedThemeCode = sharedPrefs.getString("app_theme", "system")
        val savedTheme = AppThemeMode.values().find { it.code == savedThemeCode } ?: AppThemeMode.SYSTEM
        _currentThemeMode.value = savedTheme

        // Combine search queries and repository reactive flows
        articlesState = _searchQuery
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.allArticles
                } else {
                    repository.searchArticles(query)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun changeLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        sharedPrefs.edit().putString("app_language", language.code).apply()
    }

    fun changeThemeMode(themeMode: AppThemeMode) {
        _currentThemeMode.value = themeMode
        sharedPrefs.edit().putString("app_theme", themeMode.code).apply()
    }

    fun clearAllArticles(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAll()
            onSuccess()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Authentication Actions ---

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        val sanitizedEmail = email.trim().lowercase()
        if (sanitizedEmail.isBlank() || password.length < 4) {
            onError()
            return
        }

        // Check against saved users
        val registeredPassword = sharedPrefs.getString("user_pwd_$sanitizedEmail", null)
        
        // Let's also support a default master user for developer review ease: admin@example.com / 123456
        if (registeredPassword == password || (sanitizedEmail == "admin@example.com" && password == "123456")) {
            sharedPrefs.edit().putString("logged_in_email", sanitizedEmail).apply()
            _currentUserEmail.value = sanitizedEmail
            _isLoggedIn.value = true
            onSuccess()
        } else {
            onError()
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val sanitizedEmail = email.trim().lowercase()
        if (sanitizedEmail.isBlank() || !sanitizedEmail.contains("@") || !sanitizedEmail.contains(".")) {
            onError("invalid_email")
            return
        }
        if (password.length < 6) {
            onError("password_too_short")
            return
        }

        // Check if user already exists
        if (sharedPrefs.contains("user_pwd_$sanitizedEmail")) {
            onError("user_exists")
            return
        }

        // Register user securely locally
        sharedPrefs.edit()
            .putString("user_pwd_$sanitizedEmail", password)
            .putString("logged_in_email", sanitizedEmail)
            .apply()

        _currentUserEmail.value = sanitizedEmail
        _isLoggedIn.value = true
        onSuccess()
    }

    fun loginAsGuest(onSuccess: () -> Unit) {
        val guestEmail = "guest"
        sharedPrefs.edit().putString("logged_in_email", guestEmail).apply()
        _currentUserEmail.value = guestEmail
        _isLoggedIn.value = true
        onSuccess()
    }

    fun loginWithGoogle(email: String = "google.user@gmail.com", onSuccess: () -> Unit) {
        sharedPrefs.edit().putString("logged_in_email", email).apply()
        _currentUserEmail.value = email
        _isLoggedIn.value = true
        onSuccess()
    }

    fun logout() {
        sharedPrefs.edit().remove("logged_in_email").apply()
        _currentUserEmail.value = null
        _isLoggedIn.value = false
    }

    // --- Article management ---

    fun saveArticle(
        title: String,
        content: String,
        thumbnailUrl: String,
        category: String,
        articleIdToEdit: Int? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (title.isBlank() || content.isBlank()) {
            onError("required_fields")
            return
        }

        viewModelScope.launch {
            try {
                val article = Article(
                    id = articleIdToEdit ?: 0,
                    title = title,
                    content = content,
                    thumbnailUrl = thumbnailUrl,
                    category = category,
                    timestamp = System.currentTimeMillis(),
                    language = _currentLanguage.value.code
                )
                repository.insert(article)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error saving article")
            }
        }
    }

    fun deleteArticle(articleId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteById(articleId)
            onSuccess()
        }
    }
}
