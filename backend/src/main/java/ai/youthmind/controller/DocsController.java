package ai.youthmind.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {
    
    @GetMapping("/docs")
    public String redirectToSwaggerUI() {
        return "redirect:/swagger-ui.html";
    }
} 