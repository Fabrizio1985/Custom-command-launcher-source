package it.fabrizio.eclipse.customcommand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class CustomCommandLaunchShortcut implements ILaunchShortcut2 {
	@Override
	public void launch(ISelection selection, String mode) {
		IResource resource = getSelectedResource(selection);
		ILaunchConfiguration[] configurations = getVisibleConfigurations(resource);
		remember(configurations, resource);
		launch(configurations, mode);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		IResource resource = getSelectedResource(editor);
		ILaunchConfiguration[] configurations = getVisibleConfigurations(resource);
		remember(configurations, resource);
		launch(configurations, mode);
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		IResource resource = getSelectedResource(selection);
		ILaunchConfiguration[] configurations = getVisibleConfigurations(resource);
		remember(configurations, resource);
		return configurations;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
		IResource resource = getSelectedResource(editor);
		ILaunchConfiguration[] configurations = getVisibleConfigurations(resource);
		remember(configurations, resource);
		return configurations;
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		return getSelectedResource(selection);
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		return getSelectedResource(editor);
	}

	private void remember(ILaunchConfiguration[] configurations, IResource resource) {
		for (ILaunchConfiguration configuration : configurations) LaunchSelectionContext.remember(configuration, resource);
	}

	private ILaunchConfiguration[] getVisibleConfigurations(IResource resource) {
		String selectedFolderName = getSelectedFolderName(resource);
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(CustomCommandConstants.TYPE_ID);
			List<ILaunchConfiguration> specific = new ArrayList<>();
			List<ILaunchConfiguration> defaults = new ArrayList<>();
			for (ILaunchConfiguration configuration : manager.getLaunchConfigurations(type)) {
				String names = configuration.getAttribute(CustomCommandConstants.ATTR_PROJECT_NAMES, "").trim();
				if (names.isEmpty()) defaults.add(configuration);
				else if (matchesName(names, selectedFolderName)) specific.add(configuration);
			}
			List<ILaunchConfiguration> visible = specific.isEmpty() ? defaults : specific;
			visible.sort(Comparator.comparingInt(this::getOrder).thenComparing(ILaunchConfiguration::getName, String.CASE_INSENSITIVE_ORDER));
			return visible.toArray(ILaunchConfiguration[]::new);
		} catch (CoreException exception) {
			return new ILaunchConfiguration[0];
		}
	}

	private boolean matchesName(String configuredNames, String selectedFolderName) {
		if (selectedFolderName == null || selectedFolderName.isBlank()) return false;
		for (String configuredName : configuredNames.split(";")) {
			if (selectedFolderName.equalsIgnoreCase(configuredName.trim())) return true;
		}
		return false;
	}

	private String getSelectedFolderName(IResource resource) {
		if (resource == null) return null;
		if (resource.getType() == IResource.FILE) return resource.getParent() == null ? null : resource.getParent().getName();
		return resource.getName();
	}

	private int getOrder(ILaunchConfiguration configuration) {
		try {
			int order = configuration.getAttribute(CustomCommandConstants.ATTR_ORDER, 0);
			return order == 0 ? Integer.MAX_VALUE : order;
		} catch (CoreException exception) {
			return Integer.MAX_VALUE;
		}
	}

	private void launch(ILaunchConfiguration[] configurations, String mode) {
		if (configurations.length == 1) {
			DebugUITools.launch(configurations[0], mode);
			return;
		}
		if (configurations.length == 0) MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Custom Command", "No Custom Command matches the selected folder.");
	}

	private IResource getSelectedResource(ISelection selection) {
		if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty()) return null;
		return adaptToResource(structured.getFirstElement());
	}

	private IResource getSelectedResource(IEditorPart editor) {
		return editor == null ? null : adaptToResource(editor.getEditorInput());
	}

	private IResource adaptToResource(Object element) {
		if (element instanceof IResource resource) return resource;
		if (element instanceof IAdaptable adaptable) return adaptable.getAdapter(IResource.class);
		return null;
	}
}
