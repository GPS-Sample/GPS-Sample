package edu.gtri.gpssample.constants

enum class SampleType(val format : String) {
    None("None"),
    NumberHouseholds("# of Households"),
    PercentHouseholds("% of all Households"),
    PercentTotal("% of total population"),
}