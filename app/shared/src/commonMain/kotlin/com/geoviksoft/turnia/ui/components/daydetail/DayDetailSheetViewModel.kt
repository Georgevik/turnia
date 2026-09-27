package com.geoviksoft.turnia.ui.components.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.model.EventId
import com.geoviksoft.turnia.core.domain.model.EventKind
import com.geoviksoft.turnia.core.domain.model.EventType
import com.geoviksoft.turnia.core.domain.model.Group
import com.geoviksoft.turnia.core.domain.model.GroupEvent
import com.geoviksoft.turnia.core.domain.model.GroupEventType
import com.geoviksoft.turnia.core.domain.model.PersonalEventType
import com.geoviksoft.turnia.core.domain.model.PersonalOneOffEvent
import com.geoviksoft.turnia.core.domain.model.PersonalTypedEvent
import com.geoviksoft.turnia.core.domain.model.SwapError
import com.geoviksoft.turnia.core.domain.repository.AdRepository
import com.geoviksoft.turnia.core.domain.repository.GroupRepository
import com.geoviksoft.turnia.core.domain.repository.PersonalEventRepository
import com.geoviksoft.turnia.core.domain.repository.SharePromptRepository
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.system.onFailure
import com.geoviksoft.turnia.ui.components.calendar.model.DayEventUi
import com.geoviksoft.turnia.ui.components.calendar.model.EventSource
import com.geoviksoft.turnia.ui.components.daydetail.components.EventTypeChipUi
import com.geoviksoft.turnia.ui.components.daydetail.model.AddEventTypesUi
import com.geoviksoft.turnia.ui.components.daydetail.model.DaySwapMessage
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeSectionUi
import com.geoviksoft.turnia.ui.components.daydetail.model.EventTypeUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventFormUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventMessage
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffEventUi
import com.geoviksoft.turnia.ui.components.daydetail.model.OneOffFormAction
import com.geoviksoft.turnia.ui.system.color.EntityPalette
import com.geoviksoft.turnia.ui.system.color.entityColor
import com.geoviksoft.turnia.ui.system.color.toComposeColorOr
import com.geoviksoft.turnia.ui.system.color.toHex
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

class DayDetailSheetViewModel(
    private val date: LocalDate,
    private val addMode: DayAddMode,
    private val groupRepository: GroupRepository,
    private val personalRepository: PersonalEventRepository,
    private val userRepository: UserRepository,
    private val adRepository: AdRepository,
    private val sharePromptRepository: SharePromptRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddEventTypesUi>(AddEventTypesUi.Loading)
    val uiState = _uiState.asStateFlow()

    private val _oneOffForm = MutableStateFlow<OneOffEventFormUi?>(null)
    val oneOffForm = _oneOffForm.asStateFlow()

    private val _oneOffMessage = MutableStateFlow<OneOffEventMessage?>(null)
    val oneOffMessage = _oneOffMessage.asStateFlow()

    fun oneOffMessageShown() {
        _oneOffMessage.value = null
    }

    init {
        viewModelScope.launch {
            viewModelScope.launch {
                _uiState.update { AddEventTypesUi.Loading }

                combine(
                    groupRepository.getGroups(),
                    personalRepository.getMyEventTypes()
                ) { groups, personalTypes ->
                    buildSections(personalTypes, groups)
                }.collect { sections ->
                    _uiState.update { AddEventTypesUi.Success(sections) }
                }
            }
        }
    }

    fun retry() = Unit //loadEventTypes()

    fun removeEvent(event: DayEventUi) {
        viewModelScope.launch {
            when (event.source) {
                EventSource.GROUP -> deleteGroupEvent(event)
                EventSource.PERSONAL -> personalRepository.deleteEvent(event.id, event.date)
            }
        }
    }

    private suspend fun deleteGroupEvent(event: DayEventUi) {
        val groupId = event.groupId ?: return
        val ownerId = event.ownerId ?: return
        val assigneeId = event.assigneeId ?: return
        groupRepository.deleteEvent(groupId, event.id, event.date, ownerId, assigneeId)
    }

    private val _noteError = MutableStateFlow(false)
    val noteError = _noteError.asStateFlow()

    fun saveNotes(event: DayEventUi, notes: String) {
        if (event.source != EventSource.PERSONAL) return

        viewModelScope.launch {
            personalRepository.saveNotes(event.id, event.date, notes).onFailure {
                _noteError.value = true
            }
        }
    }

    fun noteErrorShown() {
        _noteError.value = false
    }

    private val _swapMessage = MutableStateFlow<DaySwapMessage?>(null)
    val swapMessage = _swapMessage.asStateFlow()

    fun setOnSwap(event: DayEventUi, onSwap: Boolean) {
        val groupId = event.groupId ?: return
        val assigneeId = event.assigneeId ?: return

        viewModelScope.launch {
            groupRepository.setOnSwap(
                groupId = groupId,
                eventId = event.id,
                eventDate = event.date,
                assigneeId = assigneeId,
                swappable = event.swappable,
                onSwap = onSwap,
            ).onFailure { error -> _swapMessage.value = error.toMessage() }
        }
    }

    fun swapMessageShown() {
        _swapMessage.value = null
    }

    fun takeEvent(event: DayEventUi) {
        val groupId = event.groupId ?: return

        viewModelScope.launch {
            groupRepository.takeEvent(groupId, event.id)
                .onFailure { error -> _swapMessage.value = error.toMessage() }
        }
    }

    fun returnEvent(event: DayEventUi) {
        val groupId = event.groupId ?: return

        viewModelScope.launch {
            groupRepository.returnEvent(groupId, event.id)
                .onFailure { error -> _swapMessage.value = error.toMessage() }
        }
    }

    private fun SwapError.toMessage(): DaySwapMessage = when (this) {
        SwapError.NotAssignee -> DaySwapMessage.NotAssignee
        SwapError.NotSwappable -> DaySwapMessage.NotSwappable
        SwapError.NotMember -> DaySwapMessage.NotMember
        SwapError.OwnShift -> DaySwapMessage.OwnShift
        SwapError.NotFound -> DaySwapMessage.NotFound
        SwapError.TakenBySomeoneElse -> DaySwapMessage.TakenBySomeoneElse
        SwapError.NothingToReturn -> DaySwapMessage.NothingToReturn
        SwapError.PreviousHolderLeft -> DaySwapMessage.PreviousHolderLeft
        SwapError.SaveFailed -> DaySwapMessage.SaveFailed
    }

    fun onOneOffAction(action: OneOffFormAction) {
        when (action) {
            OneOffFormAction.Open -> _oneOffForm.value = OneOffEventFormUi(
                start = LocalDateTime(date, DEFAULT_ONE_OFF_START),
                end = LocalDateTime(date, DEFAULT_ONE_OFF_START).plusOneHour(),
                color = EntityPalette.first(),
            )

            is OneOffFormAction.Edit -> {
                val event = action.event
                _oneOffForm.value = OneOffEventFormUi(
                    name = event.name,
                    notes = event.notes.orEmpty(),
                    start = event.start,
                    end = event.end,
                    allDay = event.allDay,
                    color = event.color,
                    editing = event,
                )
            }

            OneOffFormAction.Cancel -> _oneOffForm.value = null

            OneOffFormAction.Delete -> {
                val event = _oneOffForm.value?.editing ?: return
                _oneOffForm.value = null
                deleteOneOffEvent(event)
            }

            OneOffFormAction.Save -> {
                val form = _oneOffForm.value?.takeIf { it.canSave } ?: return
                _oneOffForm.value = null
                saveOneOff(form)
            }

            is OneOffFormAction.NameChanged -> updateForm { copy(name = action.name) }
            is OneOffFormAction.NotesChanged -> updateForm { copy(notes = action.notes) }
            // Moving the start past the end would leave a form that cannot be saved until the end
            // is fixed too, so the end follows it.
            is OneOffFormAction.StartChanged -> updateForm {
                copy(start = action.start, end = endFollowing(action.start))
            }

            is OneOffFormAction.EndChanged -> updateForm { copy(end = action.end) }
            is OneOffFormAction.AllDayChanged -> updateForm { copy(allDay = action.allDay) }
            is OneOffFormAction.ColorPicked -> updateForm { copy(color = action.color) }
        }
    }

    private fun updateForm(transform: OneOffEventFormUi.() -> OneOffEventFormUi) =
        _oneOffForm.update { it?.transform() }

    private fun OneOffEventFormUi.endFollowing(start: LocalDateTime): LocalDateTime = when {
        allDay -> if (end.date >= start.date) end else LocalDateTime(start.date, end.time)
        end > start -> end
        else -> start.plusOneHour()
    }

    private fun saveOneOff(form: OneOffEventFormUi) {
        viewModelScope.launch {
            val previous = form.editing?.toDomain()
            val outcome = if (previous == null) {
                // Counted as it is issued, like a typed event: the write only returns once the
                // server confirms it, which the prompt has no reason to wait for.
                launch { sharePromptRepository.eventAdded(EventKind.OneOff) }
                personalRepository.addOneOffEvent(form.toDomain(EventId(Uuid.random().toString())))
                    .also { adRepository.actionPerformed() }
            } else {
                personalRepository.updateOneOffEvent(previous, form.toDomain(previous.id))
            }
            outcome.onFailure { _oneOffMessage.value = OneOffEventMessage.SaveFailed }
        }
    }

    private fun deleteOneOffEvent(event: OneOffEventUi) {
        viewModelScope.launch {
            personalRepository.deleteOneOffEvent(event.toDomain())
                .onFailure { _oneOffMessage.value = OneOffEventMessage.DeleteFailed }
        }
    }

    private fun OneOffEventFormUi.toDomain(id: EventId) = PersonalOneOffEvent(
        id = id,
        name = name.trim(),
        notes = notes.trim().ifBlank { null },
        start = start,
        end = end,
        allDay = allDay,
        color = color.toHex(),
    )

    fun addEventOfType(eventTypeUi: EventTypeUi) {
        when (val eventType = eventTypeUi.eventType) {
            is GroupEventType -> addNewEvent(eventType, eventTypeUi)
            is PersonalEventType -> addNewEvent(eventType)
        }
        adRepository.actionPerformed()
        viewModelScope.launch { sharePromptRepository.eventAdded(EventKind.Typed) }
    }

    private fun addNewEvent(type: GroupEventType, eventTypeUi: EventTypeUi) {
        viewModelScope.launch {
            val userId = userRepository.loggedUser?.id ?: return@launch // TODO Emit error
            groupRepository.addEvent(
                GroupEvent(
                    id = EventId(Uuid.random().toString()),
                    groupId = type.groupId,
                    groupName = type.groupName,
                    ownerId = userId,
                    assigneeId = userId,
                    assigneeName = "",
                    type = type,
                    date = date,
                    onSwap = false,
                    colorHex = eventTypeUi.chipUi.color.toHex(),
                    history = emptyList(),
                )
            )
        }
    }

    private fun addNewEvent(type: PersonalEventType) {
        viewModelScope.launch {
            personalRepository.addEvent(
                PersonalTypedEvent(
                    id = EventId(Uuid.random().toString()),
                    type = type,
                    date = date,
                    notes = null,
                )
            )
        }
    }

    private fun buildSections(
        personalTypes: List<PersonalEventType>,
        groups: List<Group>,
    ): List<EventTypeSectionUi> {
        // A group the user was removed from still shows its leftover events, but offers no types
        // to add: they are out of it, and the security rules refuse the write anyway.
        val addableGroups = groups.filterNot { it.isRevoked }
        val visibleGroups = when (addMode) {
            DayAddMode.Disabled -> return emptyList()
            is DayAddMode.GroupOnly -> addableGroups.filter { it.id == addMode.groupId }
            DayAddMode.Full -> addableGroups
        }
        val personalSection = when (addMode) {
            DayAddMode.Full -> listOf(
                EventTypeSectionUi(
                    source = EventTypeSectionUi.Source.Personal,
                    events = personalTypes.map { it.toUi() },
                )
            )

            else -> emptyList()
        }
        val groupSections = visibleGroups.map { group ->
            EventTypeSectionUi(
                source = EventTypeSectionUi.Source.Group(group.id.value, group.name, group.isAdmin),
                events = group.types.map { it.toUi() },
            )
        }

        return personalSection + groupSections
    }

    private fun EventType.toUi() = EventTypeUi(
        chipUi = EventTypeChipUi(
            title = acronym ?: name,
            color = color.toComposeColorOr(entityColor(id.value))
        ),
        eventType = this,
    )
}

private val DEFAULT_ONE_OFF_START = LocalTime(9, 0)

private fun LocalDateTime.plusOneHour(): LocalDateTime =
    if (hour < 23) LocalDateTime(date, LocalTime(hour + 1, minute))
    else LocalDateTime(date.plus(1, DateTimeUnit.DAY), LocalTime(0, minute))
