/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package pageobjects.projects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebElement;

public class FacetItem {

  private final SelenideElement elt;

  public FacetItem(SelenideElement elt) {
    this.elt = elt;
  }

  public FacetItem shouldHaveValue(String key, String value) {
    this.elt.$(".facet[data-key=\"" + key + "\"] .facet-stat").shouldHave(Condition.text(value));
    return this;
  }

  public void selectValue(String key) {
    this.elt.$(".facet[data-key=\"" + key + "\"]").click();
  }

  public FacetItem selectOptionItem(String value) {
    this.elt.$(".Select-input input").val(value).pressEnter();
    return this;
  }

  private SelenideElement getSortingButton(String selector) {
    ElementsCollection buttons = this.elt.$$(".projects-facet-sort a");
    return buttons.find(new Condition("AttributeMatch") {
      @Override
      public boolean apply(WebElement webElement) {
        return webElement.getAttribute("href").matches(".*sort=" + selector + ".*");
      }
    });
  }

  public FacetItem sortListDesc() {
    this.getSortingButton("-").click();
    return this;
  }

  public FacetItem sortListAsc() {
    this.getSortingButton("[a-zA-Z ]").click();
    return this;
  }
}
