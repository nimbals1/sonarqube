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

import java.io.InputStream;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileResult;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.util.LanguageParamUtils;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.client.qualityprofile.CreateRequest;

import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;

public class CreateAction implements QProfileWsAction {

  private static final String DEPRECATED_PARAM_PROFILE_NAME = "name";
  private static final String PARAM_BACKUP_FORMAT = "backup_%s";

  private final DbClient dbClient;
  private final QProfileFactory profileFactory;
  private final QProfileExporters exporters;
  private final Languages languages;
  private final ProfileImporter[] importers;
  private final QProfileWsSupport qProfileWsSupport;
  private final ActiveRuleIndexer activeRuleIndexer;

  public CreateAction(DbClient dbClient, QProfileFactory profileFactory, QProfileExporters exporters, Languages languages,
    QProfileWsSupport qProfileWsSupport, ActiveRuleIndexer activeRuleIndexer, ProfileImporter[] importers) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.exporters = exporters;
    this.languages = languages;
    this.qProfileWsSupport = qProfileWsSupport;
    this.activeRuleIndexer = activeRuleIndexer;
    this.importers = importers;
  }

  public CreateAction(DbClient dbClient, QProfileFactory profileFactory, QProfileExporters exporters, Languages languages,
    QProfileWsSupport qProfileWsSupport, ActiveRuleIndexer activeRuleIndexer) {
    this(dbClient, profileFactory, exporters, languages, qProfileWsSupport, activeRuleIndexer, new ProfileImporter[0]);
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction create = controller.createAction(ACTION_CREATE)
      .setSince("5.2")
      .setDescription("Create a quality profile.<br/>" +
        "Require Administer Quality Profiles permission.")
      .setPost(true)
      .setResponseExample(getClass().getResource("example-create.json"))
      .setHandler(this);

    create.createParam(PARAM_PROFILE_NAME)
      .setDescription("The name for the new quality profile. Since 6.1, this parameter has been renamed from '%s' to '%s'", DEPRECATED_PARAM_PROFILE_NAME, PARAM_PROFILE_NAME)
      .setExampleValue("My Sonar way")
      .setDeprecatedKey(DEPRECATED_PARAM_PROFILE_NAME, "6.3")
      .setRequired(true);

    create.createParam(PARAM_LANGUAGE)
      .setDescription("The language for the quality profile.")
      .setExampleValue("js")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setRequired(true);

    for (ProfileImporter importer : importers) {
      create.createParam(getBackupParamName(importer.getKey()))
        .setDescription(String.format("A configuration file for %s.", importer.getName()));
    }
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    qProfileWsSupport.checkQProfileAdminPermission();

    CreateRequest createRequest = toRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      writeProtobuf(doHandle(dbSession, createRequest, request), request, response);
    }
  }

  private CreateWsResponse doHandle(DbSession dbSession, CreateRequest createRequest, Request request) {
    QProfileResult result = new QProfileResult();
    QualityProfileDto profile = profileFactory.create(dbSession, QProfileName.createFor(createRequest.getLanguage(), createRequest.getProfileName()));
    result.setProfile(profile);
    for (ProfileImporter importer : importers) {
      String importerKey = importer.getKey();
      InputStream contentToImport = request.paramAsInputStream(getBackupParamName(importerKey));
      if (contentToImport != null) {
        result.add(exporters.importXml(profile, importerKey, contentToImport, dbSession));
      }
    }
    dbSession.commit();
    activeRuleIndexer.index(result.getChanges());
    return buildResponse(result);
  }

  private static CreateRequest toRequest(Request request) {
    CreateRequest.Builder builder = CreateRequest.builder()
      .setLanguage(request.mandatoryParam(PARAM_LANGUAGE))
      .setProfileName(request.mandatoryParam(PARAM_PROFILE_NAME));
    return builder.build();
  }

  private CreateWsResponse buildResponse(QProfileResult result) {
    String language = result.profile().getLanguage();
    CreateWsResponse.QualityProfile.Builder builder = CreateWsResponse.QualityProfile.newBuilder()
      .setKey(result.profile().getKey())
      .setName(result.profile().getName())
      .setLanguage(language)
      .setLanguageName(languages.get(result.profile().getLanguage()).getName())
      .setIsDefault(false)
      .setIsInherited(false);
    if (!result.infos().isEmpty()) {
      builder.getInfosBuilder().addAllInfos(result.infos());
    }
    if (!result.warnings().isEmpty()) {
      builder.getWarningsBuilder().addAllWarnings(result.warnings());
    }
    return CreateWsResponse.newBuilder().setProfile(builder.build()).build();
  }

  private static String getBackupParamName(String importerKey) {
    return String.format(PARAM_BACKUP_FORMAT, importerKey);
  }
}
