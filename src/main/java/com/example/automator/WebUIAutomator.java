package com.example.automator;

import com.example.model.FifaFinalResult; // Import your POJO
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;

public class WebUIAutomator {

    private WebDriverWait getWait(WebDriver driver, long seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }
    private WebDriverWait getWait(WebDriver driver) {
        return getWait(driver, 10); // Default wait
    }


    /** Step 7: Fill text field */
    public void fillTextField(WebDriver driver, By locator, String textToInput) {
        if (driver == null) {
            System.out.println("[Conceptual] Fill TextField: '" + textToInput + "' into '" + locator + "'");
            return;
        }
        try {
            WebElement textField = getWait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
            textField.clear();
            textField.sendKeys(textToInput);
            System.out.println("SUCCESS (Fill Text): Filled '" + textToInput + "' into element located by: " + locator);
        } catch (Exception e) {
            System.err.println("ERROR (Fill Text) for locator " + locator + ": " + e.getMessage());
        }
    }

    /** Step 8: Click */
    public void clickElement(WebDriver driver, By locator) {
        if (driver == null) {
            System.out.println("[Conceptual] Click Element: '" + locator + "'");
            return;
        }
        try {
            WebElement elementToClick = getWait(driver).until(ExpectedConditions.elementToBeClickable(locator));
            elementToClick.click();
            System.out.println("SUCCESS (Click): Clicked element located by: " + locator);
        } catch (Exception e) {
            System.err.println("ERROR (Click) for locator " + locator + ": " + e.getMessage());
        }
    }

    /** Step 9: Check Box (Set to a desired state) */
    public void setCheckboxState(WebDriver driver, By locator, boolean shouldBeSelected) {
        if (driver == null) {
            System.out.println("[Conceptual] Set Checkbox '" + locator + "' to: " + shouldBeSelected);
            return;
        }
        try {
            WebElement checkbox = getWait(driver).until(ExpectedConditions.presenceOfElementLocated(locator));
            if (checkbox.isSelected() != shouldBeSelected) {
                checkbox.click();
                System.out.println("SUCCESS (Set Checkbox): State changed for " + locator + " to: " + shouldBeSelected);
            } else {
                System.out.println("INFO (Set Checkbox): " + locator + " is already in desired state: " + shouldBeSelected);
            }
        } catch (Exception e) {
            System.err.println("ERROR (Set Checkbox) for locator " + locator + ": " + e.getMessage());
        }
    }

    /**
     * Displays data on a temporary HTML page and gets user's choice via button clicks.
     * This simulates Step 9 (Check Box for confirmation) using a web UI.
     *
     * @param driver WebDriver instance
     * @param data The FifaFinalResult data to display
     * @return String "append", "skip", "quit", or "timeout"
     */
    public String getUserChoiceFromWebPage(WebDriver driver, FifaFinalResult data) {
        if (driver == null) {
            System.out.println("[Conceptual] Displaying data: " + data.toString());
            System.out.println("[Conceptual] Assuming user chose 'skip' as WebDriver is null.");
            return "skip"; // Default for conceptual run without driver
        }

        String htmlContent = String.format(
                "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>User Confirmation</title>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; margin: 20px; background-color: #f4f4f4; color: #333; }" +
                        "h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }" +
                        ".container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }" +
                        ".data-item { margin-bottom: 10px; } .data-item strong { color: #007bff; }" +
                        "button { background-color: #007bff; color: white; padding: 10px 20px; margin: 10px 5px 0 0; border: none; border-radius: 5px; cursor: pointer; font-size: 16px; }" +
                        "button:hover { background-color: #0056b3; }" +
                        "#choiceMessage { margin-top: 20px; font-weight: bold; color: green; }" +
                        "</style>" +
                        "<script>" +
                        "function handleChoice(choice) {" +
                        "  document.title = 'CHOICE_' + choice;" + // Signal choice via title
                        "  document.getElementById('buttons').style.display = 'none';" +
                        "  document.getElementById('choiceMessage').innerText = 'You selected: ' + choice.toUpperCase() + '. Please check your application. This window may close.';"+
                        "}" +
                        "</script></head><body><div class='container'>" +
                        "<h1>Confirm Action for FIFA Final Result</h1>" +
                        "<div class='data-item'><strong>Year:</strong> %s</div>" +
                        "<div class='data-item'><strong>Winner:</strong> %s</div>" +
                        "<div class='data-item'><strong>Score:</strong> %s</div>" +
                        "<div class='data-item'><strong>Runner-Up:</strong> %s</div>" +
                        "<hr><div id='buttons'>" +
                        "<button onclick=\"handleChoice('append')\">Yes, Append to Sheet</button>" +
                        "<button onclick=\"handleChoice('skip')\">No, Skip This Result</button>" +
                        "<button onclick=\"handleChoice('quit')\">Quit Entire Process</button>" +
                        "</div><div id='choiceMessage'></div>" +
                        "</div></body></html>",
                escapeHtml(String.valueOf(data.getYear())),
                escapeHtml(data.getWinner()),
                escapeHtml(data.getScore()),
                escapeHtml(data.getRunnerUp())
        );

        File tempHtmlFile = null;
        try {
            tempHtmlFile = File.createTempFile("user_choice_fifa_", ".html");
            try (FileWriter writer = new FileWriter(tempHtmlFile)) {
                writer.write(htmlContent);
            }

            driver.get(tempHtmlFile.toURI().toString());
            System.out.println("INFO: User confirmation page opened at: " + tempHtmlFile.toURI().toString());
            System.out.println("INFO: Please interact with the browser window to make your selection.");

            String choice = "timeout"; // Default if no choice made
            // Wait for up to 5 minutes for the title to change, indicating a choice
            WebDriverWait wait = getWait(driver, 300);
            try {
                wait.until(d -> d.getTitle().startsWith("CHOICE_"));
                String title = driver.getTitle();
                if (title.startsWith("CHOICE_")) {
                    choice = title.substring("CHOICE_".length()).toLowerCase();
                    System.out.println("INFO: User selected: " + choice);
                }
            } catch (TimeoutException e) {
                System.err.println("WARN: User did not make a choice on the web page within the time limit (5 minutes). Defaulting to 'skip'.");
                choice = "skip"; // Or "quit", depending on desired default behavior
            }
            return choice;

        } catch (IOException e) {
            System.err.println("ERROR: Could not create or write temporary HTML file for user choice: " + e.getMessage());
            return "skip"; // Default to skip on error
        } finally {
            if (tempHtmlFile != null && !tempHtmlFile.delete()) {
                System.err.println("WARN: Could not delete temporary HTML file: " + tempHtmlFile.getAbsolutePath());
                // tempHtmlFile.deleteOnExit(); // Alternative
            }
        }
    }

    // Simple HTML escape utility
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}