package com.example.fitnessapp

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {

    val client = createSupabaseClient(
        supabaseUrl = "https://qwiseuymgtftcdpstfzn.supabase.co",
        supabaseKey = "sb_publishable_OWL-_AjdckpY5DRuwJIbrg_MZUm7aT_"
    ) {
        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
    }
}