package no.iktdev.kammich.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {

    @GetMapping("/{path:^(?!api).*}/**")
    fun forward(): String {
        return "forward:/index.html"
    }
}
