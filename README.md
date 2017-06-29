# Ivy Maven Plugin

A plugin to add apache Ivy dependencies to a Maven project. This is a fork of [Remis Thoughs's Ivy Maven Plugin](https://github.com/remis-thoughts/ivy-maven-plugin). It has been reworked to pre-populate the Maven repository
from the Ivy repository, allowing all the Maven plugins to use the dependencies transparently. The key reason
behind this approach was the use OSGi modules from various repositories, that do not use the Maven structure.
What happens behind the schenes is:
1. Create a dummy Ivy module in memory
2. Populate that will all the dependencies from the Maven project
3. Try to resolve all the dependencies
4. If the POM file already exists, don't continue.
5. Convert the Ivy modules descriptor to a POM file.
6. Call the Maven install file plugin to load the module into the Maven repository. If the file already exists in Maven, the install file plugin will not overwrite.

## Configuration

The *ivy* goal can be configured with

- *settings* a String file path or URL that points to an Ivy settings (xml) file.
- *transitive* a boolean (default false), whether to add the transitive dependencies of the configured dependencies too.

## Examples 

An example of what to put in your pom.xml:

```xml
<plugin>
	<groupId>uk.org.kano.maven.plugins</groupId>
	<artifactId>ivy-maven-plugin</artifactId>
	<version>1.1.2-SNAPSHOT</version>
	<executions>
		<execution>
			<id>add-dependencies</id>
			<phase>initialize</phase>
			<goals>
				<goal>ivy</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<settings>conf/ivysettings.xml</settings>
		<transitive>true</transitive>
	</configuration>		
</plugin>
```

## Ivy File

An example Ivy file

```xml
<ivysettings>
	<settings defaultResolver="chain"/>
	<statuses>
		<status name='integration' integration='true'/>
	</statuses>
	<caches default="maven">
		<cache name="maven" basedir="c:/Apps/ivyrepo"
	  	  ivyPattern="[organisation]/[module]/[revision]/ivy-[revision].xml"
		  artifactPattern="[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"/>
	</caches>
	<resolvers>
		<chain name="chain">
			<url name="eclipse-build" checkconsistency="false">
				<ivy pattern="http://build.eclipse.org/rt/virgo/ivy/bundles/release/[organisation]/[module]/[revision]/ivy-[revision].xml" />
				<artifact pattern="http://build.eclipse.org/rt/virgo/ivy/bundles/release/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]" />
			</url>
			<url name="springsource-release" checkconsistency="false">
				<ivy pattern="http://repository.springsource.com/ivy/bundles/release/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]" />
				<artifact pattern="http://repository.springsource.com/ivy/bundles/release/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]" />
			</url>
			<url name="springsource-external" checkconsistency="false">
				<ivy pattern="http://repository.springsource.com/ivy/bundles/external/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]" />
				<artifact pattern="http://repository.springsource.com/ivy/bundles/external/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]" />
			</url>
			<url name="grails-build" checkconsistency="false">
				<ivy pattern="http://repo.grails.org/grails/core/[orgPath]/[module]/[revision]/ivy-[revision].xml" />
				<artifact pattern="http://repo.grails.org/grails/core/[orgPath]/[module]/[revision]/[artifact]-[revision].[ext]" />
			</url>
		</chain>
	</resolvers>
</ivysettings>
```
