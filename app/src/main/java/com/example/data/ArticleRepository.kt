package com.example.data

import kotlinx.coroutines.flow.Flow

class ArticleRepository(private val articleDao: ArticleDao) {
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()

    fun searchArticles(query: String): Flow<List<Article>> {
        return articleDao.searchArticles("%$query%")
    }

    fun getArticleById(id: Int): Flow<Article?> {
        return articleDao.getArticleById(id)
    }

    suspend fun insert(article: Article) {
        articleDao.insertArticle(article)
    }

    suspend fun delete(article: Article) {
        articleDao.deleteArticle(article)
    }

    suspend fun deleteById(id: Int) {
        articleDao.deleteArticleById(id)
    }

    suspend fun deleteAll() {
        articleDao.deleteAllArticles()
    }
}
