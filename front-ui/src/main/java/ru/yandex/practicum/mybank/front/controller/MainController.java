package ru.yandex.practicum.mybank.front.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybank.common.dto.AccountUpdateRequest;
import ru.yandex.practicum.mybank.common.dto.CashAction;
import ru.yandex.practicum.mybank.common.dto.CashRequest;
import ru.yandex.practicum.mybank.common.dto.OperationResultDto;
import ru.yandex.practicum.mybank.common.dto.TransferRequest;
import ru.yandex.practicum.mybank.front.client.AccountPage;
import ru.yandex.practicum.mybank.front.client.FrontGatewayClient;
import ru.yandex.practicum.mybank.front.client.GatewayClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class MainController {
    private final FrontGatewayClient gatewayClient;

    public MainController(FrontGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model,
                             @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        fillModel(model, token(authorizedClient), null, null);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(Model model,
                              @RequestParam("name") String name,
                              @RequestParam("birthdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate,
                              @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        String token = token(authorizedClient);
        try {
            gatewayClient.updateAccount(token, new AccountUpdateRequest(name, birthdate));
            fillModel(model, token, null, "Данные аккаунта сохранены");
        } catch (GatewayClientException exception) {
            fillModel(model, token, exception.getErrors(), null);
        }
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(Model model,
                           @RequestParam("value") long value,
                           @RequestParam("action") CashAction action,
                           @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        String token = token(authorizedClient);
        try {
            OperationResultDto result = gatewayClient.cash(token, new CashRequest(value, action));
            fillModel(model, token, null, result.message());
        } catch (GatewayClientException exception) {
            fillModel(model, token, exception.getErrors(), null);
        }
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(Model model,
                           @RequestParam("value") long value,
                           @RequestParam("login") String login,
                           @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient) {
        String token = token(authorizedClient);
        try {
            OperationResultDto result = gatewayClient.transfer(token, new TransferRequest(login, value));
            fillModel(model, token, null, result.message());
        } catch (GatewayClientException exception) {
            fillModel(model, token, exception.getErrors(), null);
        }
        return "main";
    }

    private void fillModel(Model model, String token, List<String> errors, String info) {
        AccountPage page = gatewayClient.loadPage(token);
        model.addAttribute("name", page.account().name());
        model.addAttribute("birthdate", page.account().birthdate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("sum", page.account().sum());
        model.addAttribute("accounts", page.accounts());
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }

    private String token(OAuth2AuthorizedClient authorizedClient) {
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
