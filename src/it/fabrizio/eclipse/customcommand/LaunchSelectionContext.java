package it.fabrizio.eclipse.customcommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;

final class LaunchSelectionContext {
	private static final Map<String, SelectedResource> CONTEXTS = new ConcurrentHashMap<>();

	private LaunchSelectionContext() {
	}

	static void remember(ILaunchConfiguration configuration, IResource resource) {
		if (configuration == null || resource == null || resource.getLocation() == null) {
			return;
		}

		IResource container = resource.getType() == IResource.FILE ? resource.getParent() : resource;
		String resourcePath = resource.getLocation().toOSString();
		String projectPath = resource.getProject() != null && resource.getProject().getLocation() != null
			? resource.getProject().getLocation().toOSString()
			: resourcePath;
		String containerPath = container != null && container.getLocation() != null
			? container.getLocation().toOSString()
			: resourcePath;

		CONTEXTS.put(configuration.getName(), new SelectedResource(resourcePath, projectPath, containerPath));
	}

	static SelectedResource get(ILaunchConfiguration configuration) {
		return configuration == null ? null : CONTEXTS.get(configuration.getName());
	}

	record SelectedResource(String resourcePath, String projectPath, String containerPath) {
	}
}
