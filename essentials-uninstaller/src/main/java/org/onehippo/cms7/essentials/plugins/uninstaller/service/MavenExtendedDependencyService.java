/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugins.uninstaller.service;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.onehippo.cms7.essentials.plugin.sdk.services.MavenDependencyServiceImpl;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.service.MavenDependencyService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.function.Predicate;

//@Service
public class MavenExtendedDependencyService extends MavenDependencyServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(MavenExtendedDependencyService.class);

    private final ProjectService projectService;

    @Inject
    public MavenExtendedDependencyService(final ProjectService projectService) {
        super(projectService);
        this.projectService = projectService;
    }

    public boolean removeDependency(final Module module, final MavenDependency dependency) {
        return updatePomModel(module, model -> {
            model.removeDependency(forMaven(dependency));
            return MavenModelUtils.writePom(model, projectService.getPomPathForModule(module).toFile());
        });
    }

    private Dependency forMaven(final MavenDependency dependency) {
        final Dependency dep = new Dependency();

        dep.setGroupId(dependency.getGroupId());
        dep.setArtifactId(dependency.getArtifactId());
        dep.setVersion(dependency.getVersion());
        dep.setType(dependency.getType());
        dep.setScope(dependency.getScope());

        return dep;
    }

    private boolean updatePomModel(final Module module, final Predicate<Model> checker) {
        final File pom = projectService.getPomPathForModule(module).toFile();
        final Model model = MavenModelUtils.readPom(pom);
        return model != null && checker.test(model);
    }
}
