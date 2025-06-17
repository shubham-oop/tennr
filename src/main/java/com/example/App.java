package com.example;

import com.example.automator.WebUIAutomator;
import com.example.model.FifaDataXPath; // Your XPath enum
import com.example.gsheets.GoogleSheetsWriter;
import com.example.model.FifaFinalResult;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class App {

    private final WebDriver driver;
    private final WebUIAutomator uiAutomator;
    private final GoogleSheetsWriter sheetsWriter;
    private final WebDriverWait wait;

    private static final String WIKIPEDIA_URL = "https://en.wikipedia.org/wiki/List_of_FIFA_World_Cup_finals";
    private static final int NUM_ITERATIONS_TO_ATTEMPT = 10;

    public App() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        this.driver = new ChromeDriver(options);
        this.uiAutomator = new WebUIAutomator();
        this.sheetsWriter = new GoogleSheetsWriter();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void runFifaDataFlow() {
        String tableBaseCheckXPath = "//*[@id=\"mw-content-text\"]/div[1]/table[4]";

        try {
            driver.get(WIKIPEDIA_URL);
            System.out.println("Opened Wikipedia page: " + WIKIPEDIA_URL);
            try {
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(tableBaseCheckXPath)));
                System.out.println("Target FIFA finals table appears to be initially loaded.");
            } catch (TimeoutException e) {
                System.err.println("ERROR: Target FIFA finals table container not found on initial load. XPath: " + tableBaseCheckXPath + ". Exiting.");
                return;
            }

            int successfullyExtractedAndOfferedForConfirmation = 0;
            List<FifaFinalResult> confirmedForAppendThisSession = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= NUM_ITERATIONS_TO_ATTEMPT; rowIndex++) {

                if (!driver.getCurrentUrl().startsWith(WIKIPEDIA_URL)) {
                    System.out.println("DEBUG: Navigating back to Wikipedia page for row index " + rowIndex + "...");
                    driver.get(WIKIPEDIA_URL);
                }

                System.out.println("\n--- Attempting to extract data for Wikipedia table row index: " + rowIndex + " ---");
                FifaFinalResult currentResult = null;
                String yearText = "", winnerName = "", scoreText = "", runnerUpName = "";

                try {
                    String yearXpath = FifaDataXPath.YEAR.getFormattedXPath(rowIndex);
                    yearText = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(yearXpath))).getText().trim();

                    String winnerXpath = FifaDataXPath.WINNER.getFormattedXPath(rowIndex);
                    winnerName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(winnerXpath))).getText().trim();

                    String scoreXpath = FifaDataXPath.SCORE.getFormattedXPath(rowIndex);
                    scoreText = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(scoreXpath))).getText().trim().replace("\n", " ");

                    String runnerUpXpath = FifaDataXPath.RUNNER_UP.getFormattedXPath(rowIndex);
                    runnerUpName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(runnerUpXpath))).getText().trim();

                    if (yearText.isEmpty() || winnerName.isEmpty() || scoreText.isEmpty() || runnerUpName.isEmpty()) {
                        System.err.println("WARN: One or more data fields are empty for row index " + rowIndex +
                                " (Y:'" + yearText + "', W:'" + winnerName + "', S:'" + scoreText + "', R:'" + runnerUpName + "'). Skipping this row.");
                        continue;
                    }

                    int year = Integer.parseInt(yearText.replaceAll("[^0-9]", ""));
                    currentResult = new FifaFinalResult(year, winnerName, scoreText, runnerUpName);
                    System.out.println("Successfully extracted: " + currentResult.toString());

                } catch (TimeoutException e) {
                    System.err.println("WARN: Timeout finding an element for row index " + rowIndex + ". This row might not exist or XPaths are incorrect. Skipping this row index. Details: " + e.getMessage());
                    continue;
                } catch (NumberFormatException e) {
                    System.err.println("ERROR: Could not parse year for row index " + rowIndex + " (Year text: '"+yearText+"'). Skipping this row.");
                    continue;
                } catch (Exception e) {
                    System.err.println("ERROR: Unexpected error extracting data for row index " + rowIndex + ": " + e.getMessage());
                    System.out.println("Skipping this row index.");
                    continue;
                }

                successfullyExtractedAndOfferedForConfirmation++;
                // This call will navigate the driver to a local file URL
                String userChoice = uiAutomator.getUserChoiceFromWebPage(driver, currentResult);
                System.out.println("DEBUG: User choice from web UI for year " + currentResult.getYear() + ": '" + userChoice + "'");

                if ("append".equals(userChoice)) {
                    System.out.println("Condition: User chose 'append'. Adding result for year " + currentResult.getYear() + " to batch.");
                    confirmedForAppendThisSession.add(currentResult);
                } else if ("quit".equals(userChoice)) {
                    System.out.println("Condition: User chose 'quit'. Terminating process.");
                    // Append any previously confirmed results before quitting
                    break;
                } else {
                    System.out.println("Condition: User chose or defaulted to '" + userChoice + "'. Skipping append for year " + currentResult.getYear() + ".");
                }
            }

            // After the loop, append any remaining confirmed results
            if (!confirmedForAppendThisSession.isEmpty()) {
                System.out.println("\n--- Step 5: Call API (Appending " + confirmedForAppendThisSession.size() + " final confirmed results to Google Sheets) ---");
                sheetsWriter.appendResults(confirmedForAppendThisSession);
            }

            System.out.println("\nFinished processing " + NUM_ITERATIONS_TO_ATTEMPT + " row indices. " + successfullyExtractedAndOfferedForConfirmation + " results were successfully extracted and offered for confirmation.");
            System.out.println(confirmedForAppendThisSession.size() + " results were confirmed by the user for appending.");

        } catch (Exception e) {
            System.err.println("FATAL ERROR in application flow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\n--- Application Flow Finished ---");
            if (driver != null) {
                System.out.println("Closing WebDriver.");
                driver.quit();
            }
        }
    }

    public static void main(String[] args) {
        App app = new App();
        app.runFifaDataFlow();
    }
}