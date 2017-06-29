package uk.org.kano.maven.plugins;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultIncludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadStatus;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * A Maven plugin to import dependencies from an Ivy repository. This works by invoking Ivy
 * to download all of the dependencies, then install them into the Maven repository.
 */
@Mojo(name = "ivy", defaultPhase = LifecyclePhase.INITIALIZE)
public class IvyMojo extends AbstractMojo {
	private static final String CONF = "default";
	private static final String JAR = "jar";
	
	private Ivy ivy = null;
	private ResolveOptions resolveOptions = null;
	private DefaultModuleDescriptor moduleDescriptor = null;

	@Parameter(defaultValue="${project}", readonly=true)
	private MavenProject mavenProject;

	@Parameter(defaultValue="${session}", readonly=true)
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	/**
	 * A file or a URL of the Ivy settings.
	 */
	@Parameter(property = "settings", required = true)
	public String settings;

	/**
	 * Should we get dependencies of the dependencies?
	 */
	@Parameter(property = "transitive", defaultValue = "false", required = false)
	public boolean transitive;


    public void execute() throws MojoExecutionException {    	
		getLog().debug("setting up plugin");
		setupIvy();

		getLog().debug("creating a dummy module to load dependencies");
		createContainerModule();
		
		// Add all of the dependencies from the main project
		for (Dependency item : mavenProject.getDependencies()) {
			if ("system".equals(item.getScope())) {
				getLog().debug("Skipping system dependency "+item.toString());
				continue;
			}
			getLog().debug("Adding "+item.toString());
			addDependency(item);
		}

		// Resolve all of the modules
		getLog().debug("Container module: "+moduleDescriptor.toString());		
		ResolveReport report = null;
		try {
			report = ivy.resolve(moduleDescriptor, resolveOptions);
		} catch (Exception e) {
			throw new MojoExecutionException("couldn't load ivy artifact", e);
		}

		if (report.getAllArtifactsReports() == null) {
			throw new MojoExecutionException("no ivy artifacts resolved for artifact");
		}

		// Process each downloaded module, regardless of the dependency for now
		for (ArtifactDownloadReport artifactReport : report.getAllArtifactsReports()) {
			if (!checkArtifactReport(artifactReport)) {
				getLog().debug("skipping bad download for download"+artifactReport.toString());
				continue;
			}
			
			ModuleRevisionId modId = artifactReport.getArtifactOrigin().getArtifact().getModuleRevisionId();
			getLog().debug("processing module: "+modId.toString());
				
			processModule(modId);
		}		
	}
	
	/**
	 * Perform a check to see if the artifact report was okay
	 * @param artifactReport
	 * @return
	 */
	public boolean checkArtifactReport(ArtifactDownloadReport artifactReport) {
		return (artifactReport != null 
				&& artifactReport.getArtifactOrigin() != null
			    && artifactReport.getArtifactOrigin().getArtifact() != null
			    && artifactReport.getArtifactOrigin().getArtifact().getName() != null
			    && artifactReport.getLocalFile() != null);
	}
	
	/**
	 * Configure the Ivy instance
	 * @throws MojoExecutionException
	 */
	private void setupIvy() throws MojoExecutionException {
		IvySettings settings = new IvySettings();
		try {
			File settingsFile = new File(this.settings);
			if (settingsFile.exists()) {
				settings.load(settingsFile);
			} else {
				// maybe a URL?
				settings.load(new URL(this.settings));
			}
		} catch (Exception e) {
			throw new MojoExecutionException("couldn't load ivy settings from '" + this.settings + "'", e);
		}
		ivy = Ivy.newInstance(settings);
		
		// How to resolve
		resolveOptions = new ResolveOptions();
		resolveOptions.setConfs(new String[] { CONF });
		resolveOptions.setLog("default");
		resolveOptions.setUseCacheOnly(false);
	}
		
	/**
	 * Create the Ivy module descriptor
	 */
	public void createContainerModule() {
		getLog().debug("Creating container module");
		moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion()));
		moduleDescriptor.addConfiguration(new Configuration(CONF, Configuration.Visibility.PUBLIC, "", null, true, null)); // create a dummy configuration
	}
	
	
	private void addDependency(Dependency dep) {
		ModuleRevisionId module = ModuleRevisionId.newInstance(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());

		// add our single dependency (transitive by default so we get everything)
		DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(moduleDescriptor, module, true, false, true);
		
		// May the "default" configuration to whatever the Maven include specified
		// Also add a default classified to download sources & javadocs
		dd.addDependencyConfiguration(CONF, mapScope(dep.getScope()));
		DefaultIncludeRule dir = new DefaultIncludeRule(
				new ArtifactId(module.getModuleId(), ".*", JAR, JAR),
				new ExactOrRegexpPatternMatcher(),
				Collections.emptyMap());
		dir.addConfiguration("*");
		dd.addIncludeRule("", dir);
		moduleDescriptor.addDependency(dd);
	}
		
	/**
	 * Convert a Maven scope to the related on in Ivy
	 * TODO: this can be done natively by Ivy
	 * @param inScope
	 * @return
	 */
	private String mapScope(String inScope) {
		if ("provided".equals(inScope)) {
			return "runtime";
		} else {
			return inScope;
		}
	}
	
	/**
	 * Create a POM file for each dependency
	 * @param dependencies
	 * @throws MojoExecutionException 
	 */
	private void processModule(ModuleRevisionId modId) throws MojoExecutionException {
		
		ResolvedModuleRevision module = ivy.findModule(modId);
		if (null == module) {
			throw new MojoExecutionException("unable to locate module for "+modId);
		}

		// Check to see if this has been processed already
		File pomFile = new File(module.getReport().getLocalFile().toString().replaceAll("\\.xml$", ".pom"));
		if (pomFile.exists()) {
			getLog().debug("skipping module "+module.getId().toString()+" as the POM file exists");
			return;
		}


		ArtifactDownloadReport main = null, src = null, doc = null;
		// Get a list of the artifacts to load
		for (Artifact a: module.getDescriptor().getAllArtifacts()) {
			getLog().debug("module contains artifact: "+a);
			ArtifactOrigin o = module.getArtifactResolver().locate(a);
			ArtifactDownloadReport r = module.getArtifactResolver().download(o, new DownloadOptions());
			
			if (r.getDownloadStatus() == DownloadStatus.FAILED) {
				getLog().debug("skipping artifact, download failed");
				continue;
			}
			getLog().debug("artifact located at "+r.getLocalFile());

			String type = a.getType();

			if (null == type || type.equals(a.getExt())) {
				getLog().debug("found main artifact "+a);
				main = r;
				continue;
			}
			if ("src".equals(type) || "source".equals(type)) {
				getLog().debug("found source artifact "+a);
				src = r;
				continue;
			}
			if ("doc".equals(type) || "javadoc".equals(type)) {
				getLog().debug("found javadoc artifact "+a);
				doc = r;
				continue;
			}
		}
		
		if (null == main) {
			getLog().debug("skipping module as no main artifact found");
			return;
		}
		
		// Write the POM file
		PomWriterUtil pomWriterUtil = new PomWriterUtil();
		try {
			pomWriterUtil.createPom(module.getDescriptor(), pomFile);
		} catch (IOException e) {
			throw new MojoExecutionException("unable to write POM", e);
		}
		
		// Setup the config
		Xpp3Dom config = configuration(
				element(name("pomFile"), pomFile.toString()),
		        element(name("file"), main.getLocalFile().toString())
		);
		if (null != src) {
			config.addChild(element(name("sources"), src.getLocalFile().toString()).toDom());
		}
		if (null != doc) {
			config.addChild(element(name("javadoc"), doc.getLocalFile().toString()).toDom());
		}

		// Install into the Maven repository
		executeMojo(
			    plugin(
			        groupId("org.apache.maven.plugins"),
			        artifactId("maven-install-plugin"),
			        version("2.5.2")
			    ),
			    goal("install-file"),
			    config,
			    executionEnvironment(
			        mavenProject,
			        mavenSession,
			        pluginManager
			    )
			);
	}	

}
