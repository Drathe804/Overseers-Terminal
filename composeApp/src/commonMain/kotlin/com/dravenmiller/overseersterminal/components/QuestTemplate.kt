import kotlinx.serialization.Serializable

@Serializable
data class QuestTemplate(
    val title: String,
    val description: String,
    val xpReward: Int,
    // Add other fields you want to auto-fill
)
