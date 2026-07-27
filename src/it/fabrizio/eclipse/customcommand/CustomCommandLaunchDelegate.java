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
		String command = substitute(configuration.getAttribute(CustomCommandConstants.ATTR_COMMAND, "").trim());
		String arguments = substitute(configuration.getAttribute(CustomCommandConstants.ATTR_ARGUMENTS, "").trim());
		String workingDirectory = substitute(configuration.getAttribute(CustomCommandConstants.ATTR_WORKING_DIRECTORY, "").trim());
		if (command.isEmpty()) throw error("Command is required.", null);
		List<String> commandLine = new ArrayList<>();
		commandLine.add(command);
		commandLine.addAll(parseArguments(arguments));
		File directory = workingDirectory.isEmpty() ? null : new File(workingDirectory);
		if (directory != null && !directory.isDirectory()) throw error("Working directory does not exist: " + workingDirectory, null);
		Process process = DebugPlugin.exec(commandLine.toArray(String[]::new), directory, DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration));
		DebugPlugin.newProcess(launch, process, configuration.getName());
	}

	private String substitute(String value) throws CoreException {
		return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(value);
	}

	private List<String> parseArguments(String input) throws CoreException {
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < input.length(); index++) {
			char character = input.charAt(index);
			if (character == '"') quoted = !quoted;
			else if (Character.isWhitespace(character) && !quoted) {
				if (!current.isEmpty()) { result.add(current.toString()); current.setLength(0); }
			} else current.append(character);
		}
		if (quoted) throw error("Arguments contain an unclosed quote.", null);
		if (!current.isEmpty()) result.add(current.toString());
		return result;
	}

	private CoreException error(String message, Throwable cause) {
		return new CoreException(new Status(IStatus.ERROR, CustomCommandConstants.PLUGIN_ID, message, cause));
	}
}
