package uk.org.kano.maven.plugins;
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorWriter;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions;
import org.apache.ivy.plugins.parser.m2.PomWriterOptions.ExtraDependency;
import org.apache.ivy.util.FileUtil;

/**
 * Essentially a copy of {@link org.apache.ivy.ant.IvyTask}
 * @author timothyh
 *
 */
public class PomWriterUtil {
	public class Mapping {
		private String conf;

		private String scope;

		public String getConf() {
			return conf;
		}

		public void setConf(String conf) {
			this.conf = conf;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}
	}

	public class Dependency {
		private String group = null;

		private String artifact = null;

		private String version = null;

		private String scope = null;

		private String type = null;

		private String classifier = null;

		private boolean optional = false;

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public String getArtifact() {
			return artifact;
		}

		public void setArtifact(String artifact) {
			this.artifact = artifact;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getScope() {
			return scope;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getClassifier() {
			return classifier;
		}

		public void setClassifier(String classifier) {
			this.classifier = classifier;
		}

		public boolean getOptional() {
			return optional;
		}

		public void setOptional(boolean optional) {
			this.optional = optional;
		}
	}

	private String artifactName;

	private String artifactPackaging;

	private File pomFile = null;

	private File headerFile = null;

	private File templateFile = null;

	private boolean printIvyInfo = true;

	private String conf;

	private File ivyFile = null;

	private String description;

	private Collection<Mapping> mappings = new ArrayList<Mapping>();

	private Collection<Dependency> dependencies = new ArrayList<Dependency>();

	public File getPomFile() {
		return pomFile;
	}

	public void setPomFile(File file) {
		pomFile = file;
	}

	public File getIvyFile() {
		return ivyFile;
	}

	public void setIvyFile(File ivyFile) {
		this.ivyFile = ivyFile;
	}

	public File getHeaderFile() {
		return headerFile;
	}

	public void setHeaderFile(File headerFile) {
		this.headerFile = headerFile;
	}

	public File getTemplateFile() {
		return templateFile;
	}

	public void setTemplateFile(File templateFile) {
		this.templateFile = templateFile;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPrintIvyInfo() {
		return printIvyInfo;
	}

	public void setPrintIvyInfo(boolean printIvyInfo) {
		this.printIvyInfo = printIvyInfo;
	}

	public String getConf() {
		return conf;
	}

	public void setConf(String conf) {
		this.conf = conf;
	}

	public String getArtifactName() {
		return artifactName;
	}

	public void setArtifactName(String artifactName) {
		this.artifactName = artifactName;
	}

	public String getArtifactPackaging() {
		return artifactPackaging;
	}

	public void setArtifactPackaging(String artifactPackaging) {
		this.artifactPackaging = artifactPackaging;
	}

	public Mapping createMapping() {
		Mapping mapping = new Mapping();
		this.mappings.add(mapping);
		return mapping;
	}

	public Dependency createDependency() {
		Dependency dependency = new Dependency();
		this.dependencies.add(dependency);
		return dependency;
	}

	public void createPom(ModuleDescriptor md, File pomFile) throws IOException {
		PomModuleDescriptorWriter.write(md, pomFile, getPomWriterOptions());
	}
	public void createPom(ModuleDescriptor md, String pomFile) throws IOException {
			PomModuleDescriptorWriter.write(md, new File(pomFile), getPomWriterOptions());
	}

	private PomWriterOptions getPomWriterOptions() throws IOException {
		PomWriterOptions options = new PomWriterOptions();
		options.setConfs(splitConfs(conf)).setArtifactName(getArtifactName())
		.setArtifactPackaging(getArtifactPackaging()).setPrintIvyInfo(isPrintIvyInfo())
		.setDescription(getDescription()).setExtraDependencies(getDependencies())
		.setTemplate(getTemplateFile());

		if (!mappings.isEmpty()) {
			options.setMapping(new PomWriterOptions.ConfigurationScopeMapping(getMappingsMap()));
		}

		if (headerFile != null) {
			options.setLicenseHeader(FileUtil.readEntirely(getHeaderFile()));
		}

		return options;
	}

	private Map<String, String> getMappingsMap() {
		Map<String, String> mappingsMap = new HashMap<String, String>();
		for (Iterator<Mapping> iter = mappings.iterator(); iter.hasNext();) {
			Mapping mapping = iter.next();
			String[] mappingConfs = splitConfs(mapping.getConf());
			for (int i = 0; i < mappingConfs.length; i++) {
				if (!mappingsMap.containsKey(mappingConfs[i])) {
					mappingsMap.put(mappingConfs[i], mapping.getScope());
				}
			}
		}
		return mappingsMap;
	}

	private List<ExtraDependency> getDependencies() {
		List<ExtraDependency> result = new ArrayList<ExtraDependency>();
		for (Iterator<Dependency> iter = dependencies.iterator(); iter.hasNext();) {
			Dependency dependency = iter.next();
			result.add(new PomWriterOptions.ExtraDependency(dependency.getGroup(), dependency
					.getArtifact(), dependency.getVersion(), dependency.getScope(), dependency
					.getType(), dependency.getClassifier(), dependency.getOptional()));
		}
		return result;
	}
	
	protected String[] splitConfs(String conf) {
		if (conf == null) {
			return null;
		}
		String[] confs = conf.split(",");
		for (int i = 0; i < confs.length; i++) {
			confs[i] = confs[i].trim();
		}
		return confs;
	}

}
