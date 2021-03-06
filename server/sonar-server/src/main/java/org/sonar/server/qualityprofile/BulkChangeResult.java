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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.qualityprofile.QualityProfileDto;

public class BulkChangeResult {

  private final QualityProfileDto profile;
  private final List<String> errors = new ArrayList<>();
  private int succeeded = 0;
  private int failed = 0;
  private final List<ActiveRuleChange> changes = Lists.newArrayList();

  public BulkChangeResult() {
    this(null);
  }

  public BulkChangeResult(@Nullable QualityProfileDto profile) {
    this.profile = profile;
  }

  @CheckForNull
  public QualityProfileDto profile() {
    return profile;
  }

  public List<String> getErrors() {
    return errors;
  }

  public int countSucceeded() {
    return succeeded;
  }

  public int countFailed() {
    return failed;
  }

  void incrementSucceeded() {
    succeeded++;
  }

  void incrementFailed() {
    failed++;
  }

  void addChanges(Collection<ActiveRuleChange> c) {
    this.changes.addAll(c);
  }

  public List<ActiveRuleChange> getChanges() {
    return changes;
  }
}
