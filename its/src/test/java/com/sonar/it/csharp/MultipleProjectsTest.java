/*
 * SonarSource :: C# :: ITs :: Plugin
 * Copyright (C) 2011-2023 SonarSource SA
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
package com.sonar.it.csharp;

import com.sonar.it.shared.TestUtils;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.ScannerForMSBuild;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Measures.Measure;

import static com.sonar.it.csharp.Tests.ORCHESTRATOR;
import static com.sonar.it.csharp.Tests.getComponent;
import static com.sonar.it.csharp.Tests.getIssues;
import static com.sonar.it.csharp.Tests.getMeasure;
import static com.sonar.it.csharp.Tests.getMeasureAsInt;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(Tests.class)
public class MultipleProjectsTest {

  @TempDir
  private static Path temp;

  private static final String PROJECT = "MultipleProjects";

  private static final String FIRST_PROJECT_DIRECTORY = "MultipleProjects:FirstProject";
  private static final String FIRST_PROJECT_FIRST_CLASS_FILE = "MultipleProjects:FirstProject/FirstClass.cs";
  private static final String FIRST_PROJECT_SECOND_CLASS_FILE = "MultipleProjects:FirstProject/SecondClass.cs";

  private static final String SECOND_PROJECT_DIRECTORY = "MultipleProjects:SecondProject";
  private static final String SECOND_PROJECT_FIRST_CLASS_FILE = "MultipleProjects:SecondProject/FirstClass.cs";

  private static BuildResult buildResult;

  @BeforeAll
  public static void beforeAll() throws Exception {
    Path projectDir = TestUtils.projectDir(temp, PROJECT);
    ScannerForMSBuild beginStep = TestUtils.createBeginStep(PROJECT, projectDir);

    ORCHESTRATOR.executeBuild(beginStep);
    TestUtils.runBuild(projectDir);
    buildResult = ORCHESTRATOR.executeBuild(TestUtils.createEndStep(projectDir));
  }

  @Test
  void projectTypesInfoIsLogged() {
    assertThat(buildResult.getLogs()).contains("Found 4 MSBuild C# projects: 2 MAIN projects. 2 TEST projects.");
  }

  @Test
  void roslynVersionIsLogged() {
    // FirstProject + FirstProjectTests + 2x SecondProject (each target framework has its log) + SecondProjectTests
    assertThat(buildResult.getLogsLines(x -> x.startsWith("INFO: Roslyn version: "))).hasSize(5);
  }

  @Test
  void projectIsAnalyzed() {
    assertThat(getComponent(PROJECT).getName()).isEqualTo("MultipleProjects");

    assertThat(getComponent(FIRST_PROJECT_DIRECTORY).getName()).isEqualTo("FirstProject");
    assertThat(getComponent(FIRST_PROJECT_FIRST_CLASS_FILE).getName()).isEqualTo("FirstClass.cs");
    assertThat(getComponent(FIRST_PROJECT_SECOND_CLASS_FILE).getName()).isEqualTo("SecondClass.cs");

    assertThat(getComponent(SECOND_PROJECT_DIRECTORY).getName()).isEqualTo("SecondProject");
    assertThat(getComponent(SECOND_PROJECT_FIRST_CLASS_FILE).getName()).isEqualTo("FirstClass.cs");
  }

  @Test
  void firstProjectFirstClassIssues() {
    List<Issues.Issue> barIssues = getIssues(FIRST_PROJECT_FIRST_CLASS_FILE)
      .stream()
      .filter(x -> x.getRule().startsWith("csharpsquid:"))
      .collect(Collectors.toList());

    assertThat(barIssues).hasSize(2);

    assertThat(barIssues)
      .filteredOn(e -> e.getRule().equalsIgnoreCase("csharpsquid:S907"))
      .hasOnlyOneElementSatisfying(e -> assertThat(e.getLine()).isEqualTo(18));
    assertThat(barIssues)
      .filteredOn(e -> e.getRule().equalsIgnoreCase("csharpsquid:S1481"))
      .hasOnlyOneElementSatisfying(e -> assertThat(e.getLine()).isEqualTo(18));
  }

  @Test
  void secondProjectFirstClassIssues() {
    List<Issues.Issue> barIssues = getIssues(SECOND_PROJECT_FIRST_CLASS_FILE)
      .stream()
      .filter(x -> x.getRule().startsWith("csharpsquid:"))
      .collect(Collectors.toList());

    assertThat(barIssues).hasOnlyOneElementSatisfying(e ->
    {
      assertThat(e.getLine()).isEqualTo(16);
      assertThat(e.getRule()).isEqualTo("csharpsquid:S3923");
    });
  }

  /* Files */

  @Test
  void filesAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("files")).isEqualTo(3);
  }

  @Test
  void filesAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "files")).isEqualTo(2);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "files")).isEqualTo(1);
  }

  @Test
  void filesAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "files")).isEqualTo(1);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "files")).isEqualTo(1);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "files")).isEqualTo(1);
  }

  /* Statements */

  @Test
  void statementsAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("statements")).isEqualTo(13);
  }

  @Test
  void statementsAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "statements")).isEqualTo(10);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "statements")).isEqualTo(3);
  }

  @Test
  void statementsAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "statements")).isEqualTo(8);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "statements")).isEqualTo(2);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "statements")).isEqualTo(3);
  }

  /* Complexity */

  @Test
  void complexityAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("complexity")).isEqualTo(20);
  }

  @Test
  void complexityAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "complexity")).isEqualTo(14);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "complexity")).isEqualTo(6);
  }

  @Test
  void complexityAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "complexity")).isEqualTo(13);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "complexity")).isEqualTo(1);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "complexity")).isEqualTo(6);
  }

  /* Cognitive Complexity */

  @Test
  void cognitiveComplexityAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "cognitive_complexity")).isEqualTo(2);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "cognitive_complexity")).isEqualTo(3);
  }

  @Test
  void cognitiveComplexityAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("cognitive_complexity")).isEqualTo(5);
  }

  @Test
  void cognitiveComplexityAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "cognitive_complexity")).isEqualTo(2);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "cognitive_complexity")).isZero();

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "cognitive_complexity")).isEqualTo(3);
  }

  /* Lines */

  @Test
  void linesAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("lines")).isEqualTo(71);
  }

  @Test
  void linesAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "lines")).isEqualTo(43);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "lines")).isEqualTo(28);
  }

  @Test
  void linesAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "lines")).isEqualTo(29);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "lines")).isEqualTo(14);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "lines")).isEqualTo(28);
  }

  /* Lines of code */

  @Test
  void linesOfCodeAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("ncloc")).isEqualTo(56);
  }

  @Test
  void linesOfCodeAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "ncloc")).isEqualTo(32);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "ncloc")).isEqualTo(24);
  }

  @Test
  void linesOfCodeAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "ncloc")).isEqualTo(20);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "ncloc")).isEqualTo(12);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "ncloc")).isEqualTo(24);
  }

  /* Comment lines */

  @Test
  void commentLinesAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("comment_lines")).isEqualTo(2);
  }

  @Test
  void commentLinesAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "comment_lines")).isEqualTo(1);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "comment_lines")).isEqualTo(1);
  }

  @Test
  void commentLinesAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "comment_lines")).isEqualTo(1);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "comment_lines")).isZero();

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "comment_lines")).isEqualTo(1);
  }

  /* Functions */

  @Test
  void functionsAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("functions")).isEqualTo(15);
  }

  @Test
  void functionsAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "functions")).isEqualTo(13);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "functions")).isEqualTo(2);
  }

  @Test
  void functionsAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "functions")).isEqualTo(12);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "functions")).isEqualTo(1);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "functions")).isEqualTo(2);
  }

  /* Classes */

  @Test
  void classesAtProjectLevel() {
    assertThat(getProjectMeasureAsInt("classes")).isEqualTo(3);
  }

  @Test
  void classesAtDirectoryLevel() {
    assertThat(getDirectoryMeasureAsInt(FIRST_PROJECT_DIRECTORY, "classes")).isEqualTo(2);
    assertThat(getDirectoryMeasureAsInt(SECOND_PROJECT_DIRECTORY, "classes")).isEqualTo(1);
  }

  @Test
  void classesAtFileLevel() {
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_FIRST_CLASS_FILE, "classes")).isEqualTo(1);
    assertThat(getFileMeasureAsInt(FIRST_PROJECT_SECOND_CLASS_FILE, "classes")).isEqualTo(1);

    assertThat(getFileMeasureAsInt(SECOND_PROJECT_FIRST_CLASS_FILE, "classes")).isEqualTo(1);
  }

  @Test
  void linesOfCodeByLine() {
    String firstProjectFirstClass = getFileMeasure(FIRST_PROJECT_FIRST_CLASS_FILE, "ncloc_data").getValue();
    assertThat(firstProjectFirstClass)
      .hasSize(92) // No other line
      .contains("1=1")
      .contains("2=1")
      .contains("4=1")
      .contains("5=1")
      .contains("6=1")
      .contains("7=1")
      .contains("8=1")
      .contains("10=1")
      .contains("12=1")
      .contains("14=1")
      .contains("16=1")
      .contains("17=1")
      .contains("18=1")
      .contains("20=1")
      .contains("22=1")
      .contains("23=1")
      .contains("25=1")
      .contains("26=1")
      .contains("27=1")
      .contains("28=1");

    String secondProjectFirstClassNcloc = getFileMeasure(SECOND_PROJECT_FIRST_CLASS_FILE, "ncloc_data").getValue();
    assertThat(secondProjectFirstClassNcloc)
      .hasSize(111) // No other line
      .contains("2=1")
      .contains("3=1")
      .contains("4=1")
      .contains("5=1")
      .contains("6=1")
      .contains("7=1")
      .contains("8=1")
      .contains("9=1")
      .contains("10=1")
      .contains("11=1")
      .contains("12=1")
      .contains("14=1")
      .contains("15=1")
      .contains("16=1")
      .contains("17=1")
      .contains("18=1")
      .contains("19=1")
      .contains("20=1")
      .contains("21=1")
      .contains("22=1")
      .contains("23=1")
      .contains("24=1")
      .contains("26=1")
      .contains("27=1");
  }

  /* Helper methods */

  private Integer getProjectMeasureAsInt(String metricKey) {
    return getMeasureAsInt(PROJECT, metricKey);
  }

  private Integer getDirectoryMeasureAsInt(String directory, String metricKey) {
    return getMeasureAsInt(directory, metricKey);
  }

  private Measure getFileMeasure(String file, String metricKey) {
    return getMeasure(file, metricKey);
  }

  private Integer getFileMeasureAsInt(String file, String metricKey) {
    return getMeasureAsInt(file, metricKey);
  }
}
