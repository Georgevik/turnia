package com.georgevik.turnia.ui.main.group.model

/**
 * A colleague row in the Groups tab. [subtitle] is the role/department line
 * shown under the name (e.g. "RN, ER Dept").
 */
data class ColleageRowUi(
    val id: String,
    val name: String,
    val subtitle: String,
)
