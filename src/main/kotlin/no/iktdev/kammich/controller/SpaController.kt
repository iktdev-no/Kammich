package no.iktdev.kammich.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Controller
class SpaController {

    @GetMapping(value = ["/{path:[^\\.]*}", "/**/{path:[^\\.]*}"])
    fun forward(): String {
        return "forward:/index.html"
    }
}
