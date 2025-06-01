package com.jpa.financemonkey.controller;

import com.google.api.services.gmail.model.Message;
import com.jpa.financemonkey.entity.FinanceTransaction;
import com.jpa.financemonkey.service.FinanceExtractionService;
import com.jpa.financemonkey.service.GmailService;
import com.jpa.financemonkey.repository.FinanceTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class GmailController {
    private final GmailService gmailService;
    private final FinanceExtractionService extractionService;
    private final FinanceTransactionRepository transactionRepository;

    @Autowired
    public GmailController(GmailService gmailService, FinanceExtractionService extractionService, FinanceTransactionRepository transactionRepository) {
        this.gmailService = gmailService;
        this.extractionService = extractionService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/gmail/messages")
    public List<String> getMessageIds() throws Exception {
        List<Message> messages = gmailService.listMessages();
        return messages.stream()
                .map(Message::getId)
                .collect(Collectors.toList());
    }

    @GetMapping("/gmail/messages/info")
    public List<Map<String, String>> getMessageInfos() throws Exception {
        List<Message> messages = gmailService.listMessages();
        return messages.stream()
                .map(msg -> {
                    try {
                        Message fullMsg = gmailService.getMessageById(msg.getId());
                        String subject = gmailService.getHeader(fullMsg, "Subject");
                        String from = gmailService.getHeader(fullMsg, "From");
                        String snippet = fullMsg.getSnippet();
                        return Map.of(
                                "id", fullMsg.getId(),
                                "subject", subject,
                                "from", from,
                                "snippet", snippet
                        );
                    } catch (Exception e) {
                        return Map.of("id", msg.getId(), "error", "Failed to fetch details");
                    }
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/finance/extract")
    public String extractFinanceTransactions() throws Exception {
        int count = extractionService.extractAndSaveTransactions();
        return count + " transactions extracted and saved.";
    }
}