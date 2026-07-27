package it.fabrizio.eclipse.customcommand;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public abstract class NamedCustomCommandLaunchShortcut implements ILaunchShortcut {
	private static final String TYPE_ID = "it.fabrizio.eclipse.customcommand.launchConfigurationType";
	private static final String RUN_GROUP_ID = "org.eclipse.debug.ui.launchGroup.run";

	protected abstract String getConfigurationName();

	@Override
	public final void launch(ISelection selection, String mode) {
		launchConfiguration(mode);
	}

	@Override
	public final void launch(IEditorPart editor, String mode) {
		launchConfiguration(mode);
	}

	private void launchConfiguration(String mode) {
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(TYPE_ID);

			for (ILaunchConfiguration configuration : manager.getLaunchConfigurations(type)) {
				if (getConfigurationName().equals(configuration.getName())) {
					DebugUITools.launch(configuration, mode);
					return;
				}
			}

			MessageDialog.openInformation(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				"Custom Command",
				"Configuration not found: " + getConfigurationName()
			);
			DebugUITools.openLaunchConfigurationDialogOnGroup(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				StructuredSelection.EMPTY,
				RUN_GROUP_ID
			);
		} catch (CoreException exception) {
			MessageDialog.openError(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
				"Custom Command",
				"Unable to run " + getConfigurationName() + ": " + exception.getStatus().getMessage()
			);
		}
	}
}
