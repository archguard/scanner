package org.archguard.architecture.detect

import org.archguard.architecture.core.CodeStructureStyle

var CONTROLLERS: List<String> = listOf("controller", "interface")
var DOMAIN: List<String> = listOf("domain")
var REPO_IMPL: List<String> = listOf("repositoryImpl", "repoImpl")
var REPO: List<String> = listOf("repo", "repository")
var INFRA: List<String> = listOf("infrastructure", "infra")
var APPLICATION: List<String> = listOf("application", "app")
var SERVICES: List<String> = listOf("service", "services")

class DddLayeredIdentify() {
    private var hasDomain: Boolean = false
    private var hasInfra: Boolean = false
    private var hasInterfaces: Boolean = false
    private var hasApp: Boolean = false

    fun addToIdentify(it: String) {
        val split = it.split(".")
        if (!this.hasDomain) {
            this.hasDomain = DOMAIN.contains(split.last())
        }

        if (!this.hasInterfaces) {
            this.hasInterfaces = CONTROLLERS.contains(split.last())
        }

        if (!this.hasInfra) {
            this.hasInfra = INFRA.contains(split.last())
        }

        if (!this.hasApp) {
            this.hasApp = APPLICATION.contains(split.last())
        }
    }

    fun canMarked(): Boolean {
        return this.hasInterfaces && this.hasDomain && this.hasApp && this.hasInfra
    }
}

class MvcLayeredIdentify() {
    private var hasController: Boolean = false
    private var hasService: Boolean = false
    private var hasRepo: Boolean = false

    fun addToIdentify(it: String) {
        val split = it.split(".")
        if (!this.hasController) {
            this.hasController = CONTROLLERS.contains(split.last())
        }

        if (!this.hasService) {
            this.hasService = SERVICES.contains(split.last())
        }

        if (!this.hasRepo) {
            this.hasRepo = REPO.contains(split.last())
        }
    }

    fun canMarked(): Boolean {
        return this.hasController && this.hasService && this.hasRepo
    }
}

class LayeredIdentify(private val packages: List<String>) {
    private var ddd = DddLayeredIdentify();
    private var mvc = MvcLayeredIdentify();
    fun identify(): CodeStructureStyle {
        packages.forEach {
            ddd.addToIdentify(it)
            mvc.addToIdentify(it)
        }

        if (ddd.canMarked()) {
            return CodeStructureStyle.DDD
        }

        if (mvc.canMarked()) {
            return CodeStructureStyle.MVC
        }

        return CodeStructureStyle.UNKNOWN
    }
}