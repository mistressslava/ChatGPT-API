package com.example.chatgptbasedcookingingredients;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final RestClient restClient;

    @Autowired
    public IngredientController(@Value("${app.openai-api-key}") String openaiApiKey) {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .build();
    }

    @PostMapping
    String categorizeIngredient(@RequestBody String ingredient) {

        ChatGPTResponse response = restClient.post()
                .uri("/chat/completions")
                .body(new ChatGPTRequest(
                        "Classify the following as a food ingredient. " +
                                "If it is an edible ingredient, reply with exactly one word: vegan, vegetarian, or normal. " +
                                "If it is not edible (like 'dog', 'stone', 'plastic'), reply: not eatable. " +
                                "Answer only with one word.\n\nIngredient: " + ingredient
                ))
                .retrieve()
                .body(ChatGPTResponse.class);

        assert response != null;
        String result = response.text().toLowerCase();
        return switch (result) {
            case "not eatable" -> "not eatable";
            case "vegan" -> "vegan";
            case "vegetarian" -> "vegetarian";
            case "regular", "normal", "meat" -> "regular";
            default -> "unknown";
        };

    }

}

record ChatGPTRequestMessage(
        String role,
        String content
) {
}

record ChatGPTMessage(
        String role,
        String content
) {
}

record ChatGPTRequest(
        String model,
        List<ChatGPTRequestMessage> messages
) {
    ChatGPTRequest(String message) {
        this("gpt-5", Collections.singletonList(new ChatGPTRequestMessage("user", message)));
    }
}

record ChatGPTChoice(
        ChatGPTMessage message
) {
}

record ChatGPTResponse(
        List<ChatGPTChoice> choices
) {
    public String text() {
        return choices.get(0).message().content();
    }
}
