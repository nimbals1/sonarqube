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
package org.sonar.server.qualityprofile.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewParam;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;

@ServerSide
public class QProfileWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public QProfileWsSupport(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public String getOrganizationKey(QualityProfileDto profile, DbSession dbSession) {
    String organizationUuid = profile.getOrganizationUuid();
    return dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
      .orElseThrow(() -> new IllegalStateException("Cannot load organization with uuid=" + organizationUuid))
      .getKey();
  }

  public OrganizationDto getOrganizationByKey(DbSession dbSession, @Nullable String organizationKey) {
    String organizationOrDefaultKey = Optional.ofNullable(organizationKey)
      .orElseGet(defaultOrganizationProvider.get()::getKey);
    return WsUtils.checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationOrDefaultKey),
      "No organization with key '%s'", organizationOrDefaultKey);
  }

  /**
   * @deprecated provide orgnization
   *
   * Use this code instead:
   * <pre>
   *   userSession.checkLoggedIn();
   *   ...
   *   // open session, if needed to acquire organizationDto
   *   userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, organizationDto.getUuid());
   * </pre>
   */
  @Deprecated
  public void checkQProfileAdminPermission() {
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }

  public static NewParam createOrganizationParam(NewAction create) {
    return create
      .createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org");
  }

  /**
   * @deprecated should not be required anymore, once all quality profile webservices are migrated to use organizations.
   */
  @Deprecated
  public OrganizationDto getDefaultOrganization(DbSession dbSession) {
    return dbClient.organizationDao().selectByKey(dbSession, defaultOrganizationProvider.get().getKey())
      .orElseThrow(() -> new IllegalStateException("Could not find default organization"));
  }

  /**
   * Get the Quality profile specified by the reference {@code ref}.
   *
   * @throws org.sonar.server.exceptions.NotFoundException if the specified organization or profile do not exist
   */
  public QualityProfileDto getProfile(DbSession dbSession, QProfileReference ref) {
    QualityProfileDto profile;
    if (ref.hasKey()) {
      profile = dbClient.qualityProfileDao().selectByKey(dbSession, ref.getKey());
    } else {
      OrganizationDto org = getOrganizationByKey(dbSession, ref.getOrganizationKey().orElse(null));
      profile = dbClient.qualityProfileDao().selectByNameAndLanguage(org, ref.getName(), ref.getLanguage(), dbSession);
    }
    return checkFound(profile, "Quality Profile does not exist");
  }
}
