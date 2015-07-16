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

package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class CommentMeasuresStepTest {

  private static final int ROOT_REF = 1;
  private static final int MODULE_REF = 12;
  private static final int SUB_MODULE_REF = 123;
  private static final int DIRECTORY_REF = 1234;
  private static final int FILE_1_REF = 12341;
  private static final int FILE_2_REF = 12342;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NCLOC)
    .add(COMMENT_LINES)
    .add(COMMENT_LINES_DENSITY)
    .add(PUBLIC_API)
    .add(PUBLIC_UNDOCUMENTED_API)
    .add(PUBLIC_DOCUMENTED_API_DENSITY);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  ComputationStep underTest = new CommentMeasuresStep(treeRootHolder, metricRepository, measureRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(
      builder(PROJECT, ROOT_REF)
        .addChildren(
          builder(MODULE, MODULE_REF)
            .addChildren(
              builder(MODULE, SUB_MODULE_REF)
                .addChildren(
                  builder(DIRECTORY, DIRECTORY_REF)
                    .addChildren(
                      builder(FILE, FILE_1_REF).build(),
                      builder(FILE, FILE_2_REF).build()
                    ).build()
                ).build()
            ).build()
        ).build());
  }

  @Test
  @Ignore("Could be reactivated when formula will be added")
  public void aggregate_comment_lines() {
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(400));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(COMMENT_LINES_KEY)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(COMMENT_LINES_KEY)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(COMMENT_LINES_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(COMMENT_LINES_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(COMMENT_LINES_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(COMMENT_LINES_KEY)).containsOnly(newMeasureBuilder().create(500));
  }

  @Test
  public void compute_comment_density() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(150));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(SUB_MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(60d));
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(20d));
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(40d));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(40d));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(40d));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(40d));
  }

  @Test
  public void compute_zero_comment_density_when_zero_comment() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(SUB_MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(300));
    measureRepository.addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(0d));
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(0d));
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(0d));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(0d));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(0d));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(COMMENT_LINES_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(0d));
  }

  @Test
  public void not_compute_comment_density_when_zero_ncloc_and_zero_comment() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(SUB_MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(0));

    underTest.execute();
    assertNoNewMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_comment_density_when_no_ncloc() {
    measureRepository.addRawMeasure(FILE_1_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(150));
    measureRepository.addRawMeasure(FILE_2_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(DIRECTORY_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(SUB_MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(MODULE_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(ROOT_REF, COMMENT_LINES_KEY, newMeasureBuilder().create(200));

    underTest.execute();
    assertNoNewMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void not_compute_comment_density_when_no_comment() {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(DIRECTORY_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(SUB_MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(MODULE_REF, NCLOC_KEY, newMeasureBuilder().create(200));
    measureRepository.addRawMeasure(ROOT_REF, NCLOC_KEY, newMeasureBuilder().create(200));

    underTest.execute();
    assertNoNewMeasures(COMMENT_LINES_DENSITY_KEY);
  }

  @Test
  public void aggregate_public_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(400));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(PUBLIC_API_KEY)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(PUBLIC_API_KEY)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(PUBLIC_API_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(PUBLIC_API_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(PUBLIC_API_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(PUBLIC_API_KEY)).containsOnly(newMeasureBuilder().create(500));
  }

  @Test
  public void aggregate_public_undocumented_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(400));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(PUBLIC_UNDOCUMENTED_API_KEY)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(PUBLIC_UNDOCUMENTED_API_KEY)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(PUBLIC_UNDOCUMENTED_API_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(PUBLIC_UNDOCUMENTED_API_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(PUBLIC_UNDOCUMENTED_API_KEY)).containsOnly(newMeasureBuilder().create(500));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(PUBLIC_UNDOCUMENTED_API_KEY)).containsOnly(newMeasureBuilder().create(500));
  }

  @Test
  public void compute_public_documented_api_density() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(50));

    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(400));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(50d));
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(75d));
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(70d));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(70d));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(70d));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(70d));
  }

  @Test
  public void not_compute_public_documented_api_density_when_no_public_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));

    underTest.execute();
    assertNoNewMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void not_compute_public_documented_api_density_when_no_public_undocumented_api() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(50));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));

    underTest.execute();
    assertNoNewMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void not_compute_public_documented_api_density_when_public_api_is_zero() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(50));

    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(100));

    underTest.execute();
    assertNoNewMeasures(PUBLIC_DOCUMENTED_API_DENSITY_KEY);
  }

  @Test
  public void compute_100_percent_public_documented_api_density_when_public_undocumented_api_is_zero() {
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_API_KEY, newMeasureBuilder().create(100));
    measureRepository.addRawMeasure(FILE_1_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(0));

    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_API_KEY, newMeasureBuilder().create(400));
    measureRepository.addRawMeasure(FILE_2_REF, PUBLIC_UNDOCUMENTED_API_KEY, newMeasureBuilder().create(0));

    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(100d));
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(100d));
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(100d));
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(100d));
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(100d));
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(PUBLIC_DOCUMENTED_API_DENSITY_KEY)).containsOnly(newMeasureBuilder().create(100d));
  }

  @Test
  public void compute_nothing_when_no_data() {
    underTest.execute();

    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF)).isEmpty();
  }

  private void assertNoNewMeasures(String metric) {
    assertThat(measureRepository.getNewRawMeasures(FILE_1_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(FILE_2_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(DIRECTORY_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(SUB_MODULE_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(MODULE_REF).get(metric)).isEmpty();
    assertThat(measureRepository.getNewRawMeasures(ROOT_REF).get(metric)).isEmpty();
  }
}
