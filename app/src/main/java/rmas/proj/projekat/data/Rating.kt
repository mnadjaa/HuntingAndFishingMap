package rmas.proj.projekat.data

data class Rating(
    val userId: String="",
    val score: Float=0f,
    val comment: String="",
    val timestamp: Long = System.currentTimeMillis()
)

