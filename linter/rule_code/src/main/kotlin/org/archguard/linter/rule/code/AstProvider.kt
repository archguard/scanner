package org.archguard.rule.impl.ast

import org.archguard.rule.core.RuleSet
import org.archguard.rule.core.RuleSetProvider
import org.archguard.rule.core.RuleType
import org.archguard.rule.common.CasingRule

/*
 * Low level provider
 */
class AstProvider: RuleSetProvider {
    override fun get(): RuleSet {
        return RuleSet(
            RuleType.CHANGE_SMELL,
            "normal",
            CasingRule(),
        )
    }
}