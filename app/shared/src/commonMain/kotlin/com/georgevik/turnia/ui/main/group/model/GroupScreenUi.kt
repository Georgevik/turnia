package com.georgevik.turnia.ui.main.group.model

data class GroupScreenUi(
    val groups: List<GroupRowUi>,
    val colleagues: List<ColleageRowUi>,
    val showLoading: Boolean,
)
