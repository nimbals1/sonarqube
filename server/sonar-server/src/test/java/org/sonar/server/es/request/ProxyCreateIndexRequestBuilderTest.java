/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.es.request;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ProxyCreateIndexRequestBuilderTest {

  @ClassRule
  public static EsTester esTester = new EsTester();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void create_index() {
    CreateIndexRequestBuilder requestBuilder = esTester.client().prepareCreate(generateNewIndexName());
    requestBuilder.get();
  }

  @Test
  public void to_string() {
    String indexName = generateNewIndexName();
    assertThat(esTester.client().prepareCreate(indexName).toString()).contains("ES create index '" + indexName + "'");
  }

  @Test
  public void trace_logs() {
    logTester.setLevel(LoggerLevel.TRACE);

    CreateIndexRequestBuilder requestBuilder = esTester.client().prepareCreate(generateNewIndexName());
    requestBuilder.get();
    assertThat(logTester.logs()).hasSize(1);
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() {
    try {
      esTester.client().prepareCreate(generateNewIndexName()).get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() {
    try {
      esTester.client().prepareCreate(generateNewIndexName()).get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() {
    try {
      esTester.client().prepareCreate(generateNewIndexName()).execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

  private static String generateNewIndexName(){
    return "index_" + Long.toString(System2.INSTANCE.now());
  }

}