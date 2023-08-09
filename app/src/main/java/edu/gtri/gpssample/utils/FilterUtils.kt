package edu.gtri.gpssample.utils

import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule

object FilterUtils {
    fun findLastRule(filter: Filter): Rule? {
        var loopRule : Rule? = filter.rule
        var run = true
        while (run) {
            val op = loopRule?.filterOperator
            op?.let { op ->
                op.rule?.let{rule ->
                    loopRule = op.rule
                }

            } ?: run { run = false }
        }
        return loopRule
    }
}