package com.jpa.financemonkey.service;

import com.google.api.services.gmail.model.Message;
import com.jpa.financemonkey.entity.FinanceTransaction;
import com.jpa.financemonkey.repository.FinanceTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;


import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FinanceExtractionService {
    @Autowired
    private GmailService gmailService;
    @Autowired
    private FinanceTransactionRepository transactionRepository;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> TRANSACTION_KEYWORDS = new HashSet<>(Arrays.asList(
            "transaction", "credited", "debited", "spent", "paid", "received", "purchase", "payment", "transferred", "withdrawal", "deposit", "billed", "order", "upi", "imps", "neft", "rtgs", "card", "transfer", "refund", "Amount", "amount"
    ));

    public int extractAndSaveTransactions() throws Exception {
        List<Message> messages = gmailService.listMessages();
        int saved = 0;
        YearMonth currentMonth = YearMonth.now();
        int aiLimit = 50; // Limit the number of emails sent to AI per run
        int aiCount = 0;
        for (Message msg : messages) {
            if (aiCount >= aiLimit) break;
            Message fullMsg = gmailService.getMessageById(msg.getId());
            String subject = gmailService.getHeader(fullMsg, "Subject");
            String body = getMessageBody(fullMsg);
            String dateHeader = gmailService.getHeader(fullMsg, "Date");
            LocalDate mailDate = parseMailDate(dateHeader);
            if (mailDate == null || !YearMonth.from(mailDate).equals(currentMonth)) {
                continue; // Skip if not this month
            }
            if (containsTransactionKeyword(subject) || containsTransactionKeyword(body)) {
                aiCount++;
                FinanceTransaction tx = aiExtractTransaction(body, subject);
                if (tx != null) {
                    tx.setDate(mailDate);
                    transactionRepository.save(tx);
                    saved++;
                }
            }
        }
        return saved;
    }

    private FinanceTransaction aiExtractTransaction(String body, String subject) {
        // Prepare prompt for Gemini
        String prompt = "Given the following email content, determine if it describes a financial transaction. If yes, extract the amount and type (Credit/Debit). Respond in JSON: {\"is_transaction\":true/false,\"amount\":number,\"type\":\"Credit/Debit\"}. Email: " + body;
        try {
            JSONObject request = new JSONObject();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            JSONObject content = new JSONObject();
            content.put("parts", new org.json.JSONArray().put(part));
            request.put("contents", new org.json.JSONArray().put(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
            String url = GEMINI_URL + geminiApiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse Gemini response
                JSONObject resp = new JSONObject(response.getBody());
                String aiText = resp.getJSONArray("candidates")
                                   .getJSONObject(0)
                                   .getJSONObject("content")
                                   .getJSONArray("parts")
                                   .getJSONObject(0)
                                   .getString("text");
                JSONObject aiResult = new JSONObject(aiText);
                if (aiResult.optBoolean("is_transaction", false)) {
                    Double amount = aiResult.optDouble("amount", Double.NaN);
                    String type = aiResult.optString("type", "");
                    if (!Double.isNaN(amount) && (type.equalsIgnoreCase("Credit") || type.equalsIgnoreCase("Debit"))) {
                        FinanceTransaction tx = new FinanceTransaction();
                        tx.setAmount(amount);
                        tx.setType(type);
                        tx.setMerchant("Unknown");
                        tx.setDescription(subject != null ? subject : "Transaction");
                        return tx;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AI ERROR] " + e.getMessage());
        }
        return null;
    }

    private boolean containsTransactionKeyword(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        String[] words = lower.split("\\W+"); // Split by non-word characters

        for (String keyword: words ){

            System.out.println(keyword);
            if (TRANSACTION_KEYWORDS.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private LocalDate parseMailDate(String dateHeader) {
        if (dateHeader == null) return null;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);
            return java.time.ZonedDateTime.parse(dateHeader, fmt).toLocalDate();
        } catch (Exception e) {
            try {
                DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);
                return java.time.ZonedDateTime.parse(dateHeader, fmt2).toLocalDate();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String getMessageBody(Message message) {
        try {
            String body = message.getPayload().getParts() != null && !message.getPayload().getParts().isEmpty()
                    ? message.getPayload().getParts().get(0).getBody().getData()
                    : message.getPayload().getBody().getData();
            byte[] decodedBytes = Base64.getUrlDecoder().decode(body);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private FinanceTransaction parseTransactionFromBody(String body, String subject) {
        if (body == null) return null;

        // Find the first number in the body as the amount
        Pattern amountPattern = Pattern.compile("(\\d+[\\d,]*\\.?[\\d]*)");
        Matcher m = amountPattern.matcher(body.replace(",", ""));
        Double amount = null;
        if (m.find()) {
            try {
                amount = Double.parseDouble(m.group(1));
            } catch (Exception ignored) {}
        }
        if (amount == null) {
            System.out.println("[DEBUG] Skipping: Amount not found in email body. Subject: " + subject);
            return null;
        }
        // Determine type based on keywords
        String lowerBody = body.toLowerCase();
        String type = (lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("deposit")) ? "Credit" : "Debit";
        // Try to extract merchant (optional, fallback to Unknown)
        String merchant = "Unknown";
        Matcher merchMatcher = Pattern.compile("at ([A-Za-z0-9 &]+)").matcher(body);
        if (merchMatcher.find()) merchant = merchMatcher.group(1);
        String description = subject != null ? subject : "Transaction";
        FinanceTransaction tx = new FinanceTransaction();
        tx.setAmount(amount);
        tx.setMerchant(merchant);
        tx.setType(type);
        tx.setDescription(description);
        return tx;
    }
}
