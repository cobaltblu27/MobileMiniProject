package com.tutorial.mobile.spinefairy.model

data class PoseMeasurement(
    val neckToNose: Float,
    val shouldersDist: Float,
) : CsvData<PoseMeasurement> {
    override val header: String
        get() = "neckToNose, shouldersDist"

    override fun toCsv(): String {
        return "$neckToNose, $shouldersDist"
    }

    companion object {
        fun fromCsv(data: String): PoseMeasurement {
            val columns = data.split(",").map(String::trim)
            return PoseMeasurement(
                neckToNose = columns[0].toFloat(),
                shouldersDist = columns[1].toFloat()
            )
        }
    }
}