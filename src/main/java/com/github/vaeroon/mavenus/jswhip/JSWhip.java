package com.github.vaeroon.mavenus.jswhip;

import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;

@Mojo(name = "jswhip", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class JSWhip extends AbstractMojo {

	@Parameter (property="workingDir", defaultValue="${project.build.directory}/../")
	private String currentWorkingDirectory;

	@Parameter (defaultValue = "${project}")
	private MavenProject project;

	@Parameter (defaultValue = "${session}")
	private MavenSession mavenSession;

	@Parameter (property="nodejs", defaultValue="node")
	private String nodeBinary;

	@Parameter (property="phantomjs", defaultValue="phantomjs")
	private String phantomBinary;

	@Parameter (property="test", defaultValue="**")
	private String testGlobPattern;

	@Parameter (property="tdd", defaultValue="")
	private boolean tdd;

	@Component
	private BuildPluginManager pluginManager;

	public void execute() throws MojoExecutionException, MojoFailureException {
		testGlobPattern = (testGlobPattern==null || testGlobPattern=="null") ? "**" : testGlobPattern;
		Plugin execPlugin = createMavenExecPlugin();
		Xpp3Dom configuration = MojoExecutor.configuration();
		configureExecPlugin(configuration);
		executeMojo(execPlugin, "exec", configuration, executionEnvironment(project,
				mavenSession, pluginManager));
	}

	private void configureExecPlugin(Xpp3Dom configuration) {
		Xpp3Dom executableConfiguration = new Xpp3Dom("executable");
		executableConfiguration.setValue(nodeBinary);
		configuration.addChild(executableConfiguration);

		Xpp3Dom argsConfiguration = new Xpp3Dom("arguments");

		Xpp3Dom arg0 = new Xpp3Dom("argument");
		arg0.setValue("target/jswhip/venus/node_modules/venus/bin/venus");
		argsConfiguration.addChild(arg0);

		Xpp3Dom arg1 = new Xpp3Dom("argument");
		arg1.setValue("run");
		argsConfiguration.addChild(arg1);

		if(getLog().isDebugEnabled()) {
			Xpp3Dom arg11 = new Xpp3Dom("argument");
			arg11.setValue("-d");
			argsConfiguration.addChild(arg11);
		}

		Xpp3Dom arg2 = new Xpp3Dom("argument");
		arg2.setValue("-t");
		argsConfiguration.addChild(arg2);

		Xpp3Dom arg3 = new Xpp3Dom("argument");
		getLog().debug(testGlobPattern);
		arg3.setValue("\"" + testGlobPattern + "\"");
		argsConfiguration.addChild(arg3);

		Xpp3Dom arg4 = new Xpp3Dom("argument");
		arg4.setValue("--require-annotations");
		argsConfiguration.addChild(arg4);

		Xpp3Dom arg5 = new Xpp3Dom("argument");
		arg5.setValue("--phantom");
		argsConfiguration.addChild(arg5);

		Xpp3Dom arg6 = new Xpp3Dom("argument");
		arg6.setValue(phantomBinary);
		argsConfiguration.addChild(arg6);

		if(tdd) {
			Xpp3Dom arg7 = new Xpp3Dom("argument");
			arg7.setValue("--tdd");
			argsConfiguration.addChild(arg7);
		}

		configuration.addChild(argsConfiguration);

		//getLog().info(args.toString());

		Xpp3Dom workingDirectory = new Xpp3Dom("workingDirectory");
		workingDirectory.setValue(currentWorkingDirectory);
		configuration.addChild(workingDirectory);
	}

	private Plugin createMavenExecPlugin() {
		Plugin execPlugin = new Plugin();
		execPlugin.setArtifactId("exec-maven-plugin");
		execPlugin.setGroupId("org.codehaus.mojo");
		execPlugin.setVersion("1.1");
		return execPlugin;
	}
}

