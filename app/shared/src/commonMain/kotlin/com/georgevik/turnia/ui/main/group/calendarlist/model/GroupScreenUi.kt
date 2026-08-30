package com.georgevik.turnia.ui.main.group.calendarlist.model

data class GroupScreenUi(
    val groups: List<GroupRowUi>,
    val colleagues: List<ColleageRowUi>,
    val showLoading: Boolean,
)
