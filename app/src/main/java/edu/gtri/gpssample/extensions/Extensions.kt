package edu.gtri.gpssample.extensions

fun Int.toBoolean() = this == 1

fun Boolean.toInt() = if (this) 1 else 0
