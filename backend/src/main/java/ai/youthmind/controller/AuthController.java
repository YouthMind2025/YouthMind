package ai.youthmind.controller;

import ai.youthmind.entity.User;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private static final String MOCK_OTP = "123456";

    private UserService userService;

    private JWTService jwtService;

    private ObjectMapper objectMapper;

    public AuthController(UserService userService, JWTService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/register")
    public String register(@RequestBody final String requestJsonString) throws Exception {
        JsonNode requestNode = objectMapper.readTree(requestJsonString);
        JsonNode emailNode = requestNode.get("email");
        JsonNode otpNode = requestNode.get("otp");
        if (!(emailNode instanceof TextNode && otpNode instanceof TextNode))
            throw new IllegalArgumentException("Invalid request body");

        String email = requestNode.get("email").asText();
        String password = requestNode.get("otp").asText();
        if (!password.equals(MOCK_OTP))
            throw new IllegalArgumentException("Incorrect otp");

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        User savedUser = userService.saveUser(user);

        Pair<String, Timestamp> token = jwtService.generateToken(savedUser.getEmail());
        JsonNode responseNode = objectMapper.createObjectNode()
                .put("userId", savedUser.getId().toString())
                .put("token", token.getFirst())
                .put("expiresAt", String.valueOf(token.getSecond()));
        return objectMapper.writeValueAsString(responseNode);
    }

    @GetMapping("/test")
    public String testAuth() {
        return "Test Page";
    }

}
