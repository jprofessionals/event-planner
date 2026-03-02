package com.meet.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "shopping-list")
interface ShoppingListConfig {
    fun apiUrl(): String
}
