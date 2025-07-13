package rmas.proj.projekat.data

data class Rating(
    val userId: String,
    val score: Float,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

