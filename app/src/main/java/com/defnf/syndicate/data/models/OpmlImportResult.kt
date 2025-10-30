package com.defnf.syndicate.data.models

data class OpmlImportResult(
    val feedsToImport: List<OpmlFeed>,
    val groupsToCreate: List<String>,
    val totalFeeds: Int,
    val duplicateFeeds: List<String> = emptyList()
)

data class OpmlFeed(
    val url: String,
    val title: String,
    val description: String? = null,
    val groupName: String? = null
)

data class OpmlGroup(
    val name: String,
    val feeds: List<OpmlFeed>
)