package edu.gtri.gpssample.constants

enum class SamplingMethod(val format : String) {
    SimpleRandom("Simple Random Sampling"),
    Cluster("Cluster Sampling"),
    Subsets("Subsets: May Overlap"),
    Strata("Strata: Mutually Exclusive"),
}