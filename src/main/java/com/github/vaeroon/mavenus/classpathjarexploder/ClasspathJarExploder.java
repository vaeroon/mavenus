package com.github.vaeroon.mavenus.classpathjarexploder;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;

@Mojo(name = "frameworkextractor", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES)
public class ClasspathJarExploder extends AbstractMojo {

	@Parameter (property="workingDir", defaultValue="${project.build.directory}")
	private String currentWorkingDirectory;

	@Parameter (defaultValue = "${session}")
	private MavenSession mavenSession;

	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	@Parameter (defaultValue = "${project}")
	private MavenProject project;

	@Component
	private BuildPluginManager pluginManager;

	public void execute() throws MojoExecutionException {
		String jarPath = getPathToEnclosingJar();
		createJSWhipFolder();
		explodeJarToOutputDirectory(jarPath);

		Plugin execPlugin = createMavenExecPlugin();
		Xpp3Dom configuration = MojoExecutor.configuration();
		configureExecPlugin(configuration);
		executeMojo(execPlugin, "exec", configuration, executionEnvironment(project,
				mavenSession, pluginManager));
	}

	private void configureExecPlugin(Xpp3Dom configuration) {
		Xpp3Dom executableConfiguration = new Xpp3Dom("executable");
		executableConfiguration.setValue("ant");
		configuration.addChild(executableConfiguration);

		Xpp3Dom argsConfiguration = new Xpp3Dom("arguments");

		Xpp3Dom arg0 = new Xpp3Dom("argument");
		arg0.setValue("-f");
		argsConfiguration.addChild(arg0);

		Xpp3Dom arg1 = new Xpp3Dom("argument");
		arg1.setValue("jswhip/zip.xml");
		argsConfiguration.addChild(arg1);

		/*if(getLog().isDebugEnabled()) {
			Xpp3Dom arg11 = new Xpp3Dom("argument");
			arg11.setValue("-d");
			argsConfiguration.addChild(arg11);
		}*/

		Xpp3Dom arg2 = new Xpp3Dom("argument");
		arg2.setValue("unzip");
		argsConfiguration.addChild(arg2);

		configuration.addChild(argsConfiguration);

		Xpp3Dom workingDirectory = new Xpp3Dom("workingDirectory");
		workingDirectory.setValue(currentWorkingDirectory);
		configuration.addChild(workingDirectory);
	}

	private void createJSWhipFolder() {
		File outputFile = new File(outputDirectory + File.separator + "jswhip");
		outputFile.mkdir();
		System.out.println("jswhip path = " + outputDirectory + File.separator + "jswhip");
		getLog().info("Created jswhip directory" + outputFile.getName());
	}

	private Plugin createMavenExecPlugin() {
		Plugin execPlugin = new Plugin();
		execPlugin.setArtifactId("exec-maven-plugin");
		execPlugin.setGroupId("org.codehaus.mojo");
		execPlugin.setVersion("1.1");
		return execPlugin;
	}

	private Plugin createAntRunPlugin() {
		Plugin antRunPlugin = new Plugin();
		antRunPlugin.setArtifactId("maven-antrun-plugin");
		antRunPlugin.setGroupId("org.apache.maven.plugins");
		antRunPlugin.setVersion("1.6");
		return antRunPlugin;
	}

	private String getPathToEnclosingJar() throws MojoExecutionException {
		URL resource = this.getClass().getClassLoader().getResource("com/github/vaeroon/mavenus/classpathjarexploder/ClasspathJarExploder.class");
		if(resource == null) {
			getLog().error("Unable to extract framework files.");
		}
		String filePath = resource.getPath().substring(5).split("!")[0];
		File file = new File(filePath);
		if(!file.exists()) {
			getLog().error("Something is terribly wrong, Jar file for this plugin doesn't exist? wierd!!!");
			throw new MojoExecutionException("Plugin cannot proceed, where is the JAR?");
		}
		return file.getAbsolutePath();
	}

	private void explodeJarToOutputDirectory(String jarPath) {
		try {
			JarFile jarFile = new JarFile(jarPath);
			Enumeration<JarEntry> jarEntries = jarFile.entries();
			while (jarEntries.hasMoreElements()) {
				JarEntry entry = jarEntries.nextElement();
				File outputFile = new File(outputDirectory + File.separator + "jswhip" + File.separator + entry.getName());
				if(entry.isDirectory()) {
					outputFile.mkdir();
					continue;
				}
				if(entry.getName().endsWith(".class")) {
					continue;
				}
				InputStream jarEntryInputStream = jarFile.getInputStream(entry);
				getLog().debug("Copying " + outputFile);
				FileOutputStream writeOutFileOutputStream = new FileOutputStream(outputFile);
				while(jarEntryInputStream.available() > 0) {
					writeOutFileOutputStream.write(jarEntryInputStream.read());
				}
				writeOutFileOutputStream.close();
				jarEntryInputStream.close();
			}
		} catch (IOException e) {
			getLog().error("Error reading the jar file to explode", e);
		}
	}
}

