package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.dto.AssistantAnswer;
import com.retailops.retailops_ai.dto.AssistantQuestion;
import com.retailops.retailops_ai.service.RetailAssistantService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final RetailAssistantService assistantService;

    public AssistantController(RetailAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public AssistantAnswer ask(@RequestBody AssistantQuestion request) {
        return assistantService.ask(request);
    }
}