package dev.bmac.github.web

import dev.bmac.github.storage.KeyStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class WebController(keyStorage: KeyStorage, @Value("\${client.id}") clientId: String,
                    @Value("\${client.secret}") clientSecret: String) {
    val keyStorage = keyStorage
    val clientId = clientId
    val clientSecret = clientSecret

    @GetMapping("/initiate")
    fun initiate(model: Model, @RequestParam("code") code: String,
                 @RequestParam("state") state: String): String {
        model.addAttribute("code", code)
        model.addAttribute("state", state)
        return "verification"
    }

}