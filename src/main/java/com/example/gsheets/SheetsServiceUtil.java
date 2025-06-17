package com.example.gsheets;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class SheetsServiceUtil {

    private static final String APPLICATION_NAME = "FunctionalRPAAppWithServiceAccount";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // Scopes required for reading and writing to Google Sheets.
    // For service accounts, ensure these scopes are appropriate for the actions performed.
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    // Path to your service account key JSON file in the resources directory.
    // IMPORTANT: This file should now be your Service Account Key JSON.
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Sheets API client service using a service account.
     *
     * @return an authorized Sheets API client service
     * @throws IOException if the credentials file cannot be found or read.
     * @throws GeneralSecurityException if there's a security issue with HTTP transport.
     */
    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = SheetsServiceUtil.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

        if (in == null) {
            throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH +
                    ". Ensure your service account JSON key file is in src/main/resources/" +
                    " and named correctly (e.g., credentials.json).");
        }

        GoogleCredential credential = GoogleCredential.fromStream(in, httpTransport, JSON_FACTORY)
                .createScoped(SCOPES);

        return new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}