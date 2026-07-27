package it.fabrizio.eclipse.customcommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;

public class CustomCommandLaunchDelegate extends LaunchConfigurationDelegate {
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String command = resolve(configuration, configuration.getAttribute(CustomCommandConstants.ATTR_COMMAND, "").trim());
		String arguments = resolve(configuration, configuration.getAttribute(CustomCommandConstants.ATTR_ARGUMENTS, "").trim());
		String workingDirectory = resolve(configuration, configuration.getAttribute(CustomCommandConstants.ATTR_WORKING_DIRECTORY, "").trim());
		if (command.isEmpty()) throw error("Command is required.", null);

		List<String> commandLine = new ArrayList<>();
		commandLine.add(command);
		commandLine.addAll(parseArguments(arguments));
		File directory = workingDirectory.isEmpty() ? null : new File(workingDirectory);
		if (directory != null && !directory.isDirectory()) throw error("Working directory does not exist: " + workingDirectory, null);
		Process process = DebugPlugin.exec(commandLine.toArray(String[]::new), directory, DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration));
		DebugPlugin.newProcess(launch, process, configuration.getName());
	}

	private String resolve(ILaunchConfiguration configuration, String value) throws CoreException {
		LaunchSelectionContext.SelectedResource selected = LaunchSelectionContext.get(configuration);
		if (selected != null) {
			value = value
				.replace("${resource_loc}", selected.resourcePath())
				.replace("${project_loc}", selected.projectPath())
				.replace("${container_loc}", selected.containerPath());
		}
		return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
	}

	private List<String> parseArguments(String input) throws CoreException {
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean doubleQuoted = false;
		boolean singleQuoted = false;
		for (int index = 0; index < input.length(); index++) {
			char character = input.charAt(index);
			if (character == '"' && !singleQuoted) doubleQuoted = !doubleQuoted;
			else if (character == '\'' && !doubleQuoted) singleQuoted = !singleQuoted;
			else if (Character.isWhitespace(character) && !doubleQuoted && !singleQuoted) {
				if (!current.isEmpty()) { result.add(current.toString()); current.setLength(0); }
			} else current.append(character);
		}
		if (doubleQuoted || singleQuoted) throw error("Arguments contain an unclosed quote.", null);
		if (!current.isEmpty()) result.add(current.toString());
		return result;
	}

	private CoreException error(String message, Throwable cause) {
		return new CoreException(new Status(IStatus.ERROR, CustomCommandConstants.PLUGIN_ID, message, cause));
	}
}
