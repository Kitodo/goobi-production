/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.selenium.testframework.pages;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.kitodo.selenium.testframework.Browser;
import org.kitodo.selenium.testframework.Pages;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class ProcessFromTemplatePage extends EditPage<ProcessFromTemplatePage> {

    private static final String TAB_VIEW = EDIT_FORM + ":processFromTemplateTabView";

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW)
    private WebElement processFromTemplateTabView;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":processTitle")
    private WebElement processTitleInput;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":guessImages")
    private WebElement guessImagesInput;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":fieldList:2:additionalInputField")
    private WebElement titleInput;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":fieldList:3:additionalInputField")
    private WebElement titleSortInput;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":fieldList:6:additionalInputField")
    private WebElement ppnAnalogInput;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":fieldList:7:additionalInputField")
    private WebElement ppnDigitalInput;

    @SuppressWarnings("unused")
    @FindBy(id = TAB_VIEW + ":generateTitleButton")
    private WebElement generateTitleButton;

    public ProcessFromTemplatePage() {
        super("pages/processFromTemplate.jsf");
    }

    @Override
    public ProcessFromTemplatePage goTo() {
        return null;
    }

    public String createProcess() throws Exception {
        guessImagesInput.sendKeys("299");
        Browser.getDriver().findElements(By.cssSelector(".ui-selectonemenu-trigger")).get(1).click();
        Browser.getDriver().findElement(By.id(TAB_VIEW + ":Regelsatz_2")).click();
        switchToTabByIndex(1);
        titleInput.sendKeys("TestProcess");
        titleSortInput.sendKeys("TestProcess");
        ppnAnalogInput.sendKeys("12345");
        ppnDigitalInput.sendKeys("12345");
        switchToTabByIndex(0);
        generateTitleButton.click();
        await("Wait for title generation").pollDelay(3, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS)
                .ignoreExceptions().until(() -> isInputValueNotEmpty.matches(titleInput));
        String generatedTitle = processTitleInput.getAttribute("value");
        save();
        return generatedTitle;
    }

    public ProcessFromTemplatePage switchToTabByIndex(int index) throws Exception {
        return switchToTabByIndex(index, processFromTemplateTabView);
    }

    public ProcessesPage save() throws IllegalAccessException, InstantiationException {
        clickButtonAndWaitForRedirect(saveButton, Pages.getProcessesPage().getUrl());
        return Pages.getProcessesPage();
    }
}
