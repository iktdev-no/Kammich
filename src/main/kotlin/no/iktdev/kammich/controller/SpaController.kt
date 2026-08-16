package no.iktdev.kammich.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class SpaController {

    @RequestMapping(
        value = ["/**"],
        produces = ["text/html"]
    )
    fun forward(): String {
        return "forward:/index.html"
    }
}
