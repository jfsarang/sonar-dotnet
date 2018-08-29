/*
 * SonarQube .NET Tests Library
 * Copyright (C) 2014-2018 SonarSource SA
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
package org.sonar.plugins.dotnet.tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NUnitTestResultsFileParserTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void no_counters() {
    thrown.expect(ParseErrorException.class);
    thrown.expectMessage("Missing attribute \"total\" in element <test-results> in ");
    thrown.expectMessage(new File("src/test/resources/nunit/no_counters.xml").getAbsolutePath());
    new NUnitTestResultsFileParser().accept(new File("src/test/resources/nunit/no_counters.xml"), mock(UnitTestResults.class));
  }

  @Test
  public void wrong_passed_number() {
    thrown.expect(ParseErrorException.class);
    thrown.expectMessage("Expected an integer instead of \"invalid\" for the attribute \"total\" in ");
    thrown.expectMessage(new File("src/test/resources/nunit/invalid_total.xml").getAbsolutePath());
    new NUnitTestResultsFileParser().accept(new File("src/test/resources/nunit/invalid_total.xml"), mock(UnitTestResults.class));
  }

  @Test
  public void valid() {
    UnitTestResults results = new UnitTestResults();
    new NUnitTestResultsFileParser().accept(new File("src/test/resources/nunit/valid.xml"), results);

    assertThat(results.errors()).isEqualTo(30);
    assertThat(results.failures()).isEqualTo(20);
    assertThat(results.tests()).isEqualTo(200);
    assertThat(results.skipped()).isEqualTo(9); // 4 + 3 + 2
    assertThat(results.executionTime()).isEqualTo(51);
  }

  @Test
  public void valid_comma_in_double() {
    UnitTestResults results = new UnitTestResults();
    new NUnitTestResultsFileParser().accept(new File("src/test/resources/nunit/valid_comma_in_double.xml"), results);

    assertThat(results.executionTime()).isEqualTo(1051);
  }

  @Test
  public void valid_no_execution_time() {
    UnitTestResults results = new UnitTestResults();
    new NUnitTestResultsFileParser().accept(new File("src/test/resources/nunit/valid_no_execution_time.xml"), results);

    assertThat(results.failures()).isEqualTo(20);
    assertThat(results.errors()).isEqualTo(30);
    assertThat(results.tests()).isEqualTo(200);
    assertThat(results.skipped()).isEqualTo(9); // 4 + 3 + 2
    assertThat(results.executionTime()).isNull();
  }

  @Test
  public void nunit3_sample() {
    UnitTestResults results = new UnitTestResults();
    new NUnitTestResultsFileParser().accept(new File("src/test/resources/nunit/nunit3_sample.xml"), results);

    assertThat(results.failures()).isEqualTo(2);
    assertThat(results.errors()).isEqualTo(1);
    assertThat(results.tests()).isEqualTo(18);
    assertThat(results.skipped()).isEqualTo(4); // 1 + 3
    assertThat(results.executionTime()).isEqualTo(154);
  }

}
