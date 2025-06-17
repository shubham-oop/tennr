package com.example.gsheets;

import com.example.model.FifaFinalResult;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoogleSheetsWriter {

    private static final String PROPERTIES_FILE_PATH = "/google.sheets.properties.json";
    private static String SPREADSHEET_ID;
    private static String SHEET_NAME;
    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream inputStream = GoogleSheetsWriter.class.getResourceAsStream(PROPERTIES_FILE_PATH)) {
            if (inputStream == null) {
                System.err.println("ERROR: Properties file not found at " + PROPERTIES_FILE_PATH + ". Please ensure it exists in src/main/resources.");
                // Set defaults or throw an exception if properties are critical
                SPREADSHEET_ID = "FALLBACK_SPREADSHEET_ID_NOT_CONFIGURED";
                SHEET_NAME = "Sheet1";
                return;
            }
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            JsonObject properties = JsonParser.parseReader(reader).getAsJsonObject();

            SPREADSHEET_ID = properties.has("SPREADSHEET_ID") ? properties.get("SPREADSHEET_ID").getAsString() : "MISSING_SPREADSHEET_ID";
            SHEET_NAME = properties.has("SHEET_NAME") ? properties.get("SHEET_NAME").getAsString() : "Sheet1";

        } catch (IOException e) {
            System.err.println("ERROR: Could not read properties file: " + PROPERTIES_FILE_PATH + " - " + e.getMessage());
            e.printStackTrace();
            SPREADSHEET_ID = "ERROR_LOADING_SPREADSHEET_ID";
            SHEET_NAME = "Sheet1";
        } catch (Exception e) {
            System.err.println("ERROR: Could not parse properties file: " + PROPERTIES_FILE_PATH + " - " + e.getMessage());
            e.printStackTrace();
            SPREADSHEET_ID = "ERROR_PARSING_SPREADSHEET_ID";
            SHEET_NAME = "Sheet1";
        }
    }

    public boolean appendResults(List<FifaFinalResult> results) {
        if (SPREADSHEET_ID == null || SPREADSHEET_ID.contains("FALLBACK") || SPREADSHEET_ID.contains("MISSING") || SPREADSHEET_ID.contains("ERROR")) {
            System.err.println("ERROR (Call API): SPREADSHEET_ID is not configured correctly or failed to load. Current value: " + SPREADSHEET_ID);
            System.err.println("Please check '" + PROPERTIES_FILE_PATH + "' in your resources directory.");
            return false;
        }
        if (results == null || results.isEmpty()) {
            System.out.println("INFO (Call API): No results to append.");
            return true; // No error, just nothing to do
        }

        try {
            Sheets service = SheetsServiceUtil.getSheetsService(); // Assumes SheetsServiceUtil is correctly set up for auth

            // --- Step 6: Detour (Conditional: Check if sheet is empty to add headers) ---
            String checkRange = SHEET_NAME + "!A1"; // Use loaded SHEET_NAME
            ValueRange existingDataResponse = service.spreadsheets().values()
                    .get(SPREADSHEET_ID, checkRange) // Use loaded SPREADSHEET_ID
                    .execute();

            boolean isSheetEffectivelyEmpty = existingDataResponse.getValues() == null ||
                    existingDataResponse.getValues().isEmpty() ||
                    (existingDataResponse.getValues().get(0) != null &&
                            existingDataResponse.getValues().get(0).isEmpty());

            List<List<Object>> rowsToAppend = new ArrayList<>();
            if (isSheetEffectivelyEmpty) {
                System.out.println("INFO (Call API Detour): Sheet '" + SHEET_NAME + "' appears empty, adding header row.");
                rowsToAppend.add(Arrays.asList("Year", "Winner", "Score", "Runner-Up"));
            } else {
                System.out.println("INFO (Call API Detour): Sheet '" + SHEET_NAME + "' has data, not adding header row.");
            }

            for (FifaFinalResult result : results) {
                rowsToAppend.add(Arrays.asList(
                        result.getYear(),
                        result.getWinner(),
                        result.getScore(),
                        result.getRunnerUp()
                ));
            }

            ValueRange body = new ValueRange().setValues(rowsToAppend);
            // The range for append (e.g., SHEET_NAME + "!A:D") tells Sheets API to append after the last row with data.
            service.spreadsheets().values()
                    .append(SPREADSHEET_ID, SHEET_NAME + "!A:D", body) // Use loaded SPREADSHEET_ID and SHEET_NAME
                    .setValueInputOption("USER_ENTERED")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
            System.out.println("SUCCESS (Call API): Appended " + results.size() + " results to spreadsheet: " + SPREADSHEET_ID + ", sheet: " + SHEET_NAME);
            return true;

        } catch (IOException | GeneralSecurityException e) {
            System.err.println("ERROR (Call API): Failed to connect or authorize Google Sheets: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("ERROR (Call API): Failed to append data to Google Sheets: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}