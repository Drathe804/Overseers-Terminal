package com.dravenmiller.overseersterminal

// The standard format for our incoming data
data class CalendarEvent(
    val id: String,
    val title: String,
    val startTimeMs: Long
)

// The template for our auto-generated quests
data class QuestTemplate(
    val title: String,
    val description: String,
    val xpReward: Int
)

object CalendarProcessor {
    // This is where you put the logic you asked about!
    fun processEventsToQuests(events: List<CalendarEvent>, templates: List<QuestTemplate>) {
        events.forEach { event ->
            val matchingTemplate = templates.find { it.title == event.title }

            if (matchingTemplate != null) {
                // If it matches a template, we generate the quest!
                println("RobCo Auto-Generator: Creating quest for ${event.title}!")
                // TODO: Save this to your actual Quest List / Database
            }
        }
    }
}
