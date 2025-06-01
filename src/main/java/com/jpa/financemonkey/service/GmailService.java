package com.jpa.financemonkey.service;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service
public class GmailService {
    private static final String APPLICATION_NAME = "FinanceMonkey";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private Gmail getGmailService() throws Exception {
        InputStream in = getClass().getResourceAsStream("/credentials.json");
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in)),
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(Paths.get(TOKENS_DIRECTORY_PATH).toFile()))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY,
                new AuthorizationCodeInstalledApp(flow, receiver).authorize("user"))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Message> listMessages() throws Exception {
        Gmail service = getGmailService();
        ListMessagesResponse response = service.users().messages().list("me").setMaxResults(10L).execute();
        return response.getMessages();
    }

    public Message getMessageById(String messageId) throws Exception {
        Gmail service = getGmailService();
        return service.users().messages().get("me", messageId).execute();
    }

    public String getHeader(Message message, String headerName) {
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            return message.getPayload().getHeaders().stream()
                    .filter(h -> headerName.equalsIgnoreCase(h.getName()))
                    .map(com.google.api.services.gmail.model.MessagePartHeader::getValue)
                    .findFirst()
                    .orElse("");
        }
        return "";
    }
}