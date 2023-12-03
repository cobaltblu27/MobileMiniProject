package com.tutorial.mobile.spinefairy.model

interface CsvData<T: CsvData<T>> {
    val header: String
    fun toCsv(): String
}