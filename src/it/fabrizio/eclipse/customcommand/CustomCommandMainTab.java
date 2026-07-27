package it.fabrizio.eclipse.customcommand;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class CustomCommandMainTab extends AbstractLaunchConfigurationTab {
	private Text commandText;
	private Text argumentsText;
	private Spinner orderSpinner;
	private Text projectNamesText;
	private Text workingDirectoryText;

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));
		setControl(container);
		ModifyListener listener = event -> changed();

		new Label(container, SWT.NONE).setText("Command:");
		commandText = new Text(container, SWT.BORDER);
		commandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		commandText.addModifyListener(listener);
		Button commandBrowse = new Button(container, SWT.PUSH);
		commandBrowse.setText("Browse...");
		commandBrowse.addListener(SWT.Selection, event -> browseCommand());

		new Label(container, SWT.NONE).setText("Arguments:");
		argumentsText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData argumentsData = new GridData(SWT.FILL, SWT.FILL, true, true);
		argumentsData.heightHint = 100;
		argumentsText.setLayoutData(argumentsData);
		argumentsText.addModifyListener(listener);
		new Label(container, SWT.NONE);

		new Label(container, SWT.NONE).setText("Order (optional):");
		orderSpinner = new Spinner(container, SWT.BORDER);
		orderSpinner.setMinimum(0);
		orderSpinner.setMaximum(9999);
		orderSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		orderSpinner.addModifyListener(listener);
		new Label(container, SWT.NONE);

		new Label(container, SWT.NONE).setText("Project name (optional):");
		projectNamesText = new Text(container, SWT.BORDER);
		projectNamesText.setMessage("Example: fabrizio-angular-library");
		projectNamesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectNamesText.addModifyListener(listener);
		new Label(container, SWT.NONE);

		new Label(container, SWT.NONE).setText("Working directory:");
		workingDirectoryText = new Text(container, SWT.BORDER);
		workingDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		workingDirectoryText.addModifyListener(listener);
		Button directoryBrowse = new Button(container, SWT.PUSH);
		directoryBrowse.setText("Browse...");
		directoryBrowse.addListener(SWT.Selection, event -> browseWorkingDirectory());
	}

	private void changed() {
		setDirty(true);
		updateLaunchConfigurationDialog();
	}

	private void browseCommand() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		String current = commandText.getText().trim();
		if (!current.isEmpty()) {
			File file = new File(current);
			if (file.getParent() != null) dialog.setFilterPath(file.getParent());
		}
		String selected = dialog.open();
		if (selected != null) commandText.setText(selected);
	}

	private void browseWorkingDirectory() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		String current = workingDirectoryText.getText().trim();
		if (!current.isEmpty() && !current.contains("${")) dialog.setFilterPath(current);
		String selected = dialog.open();
		if (selected != null) workingDirectoryText.setText(selected);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CustomCommandConstants.ATTR_COMMAND, "");
		configuration.setAttribute(CustomCommandConstants.ATTR_ARGUMENTS, "");
		configuration.setAttribute(CustomCommandConstants.ATTR_ORDER, 0);
		configuration.setAttribute(CustomCommandConstants.ATTR_PROJECT_NAMES, "");
		configuration.setAttribute(CustomCommandConstants.ATTR_WORKING_DIRECTORY, "${resource_loc}");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			commandText.setText(configuration.getAttribute(CustomCommandConstants.ATTR_COMMAND, ""));
			argumentsText.setText(configuration.getAttribute(CustomCommandConstants.ATTR_ARGUMENTS, ""));
			orderSpinner.setSelection(configuration.getAttribute(CustomCommandConstants.ATTR_ORDER, 0));
			projectNamesText.setText(configuration.getAttribute(CustomCommandConstants.ATTR_PROJECT_NAMES, ""));
			workingDirectoryText.setText(configuration.getAttribute(CustomCommandConstants.ATTR_WORKING_DIRECTORY, "${resource_loc}"));
		} catch (CoreException exception) {
			setErrorMessage(exception.getMessage());
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CustomCommandConstants.ATTR_COMMAND, commandText.getText().trim());
		configuration.setAttribute(CustomCommandConstants.ATTR_ARGUMENTS, argumentsText.getText().trim());
		configuration.setAttribute(CustomCommandConstants.ATTR_ORDER, orderSpinner.getSelection());
		configuration.setAttribute(CustomCommandConstants.ATTR_PROJECT_NAMES, projectNamesText.getText().trim());
		configuration.setAttribute(CustomCommandConstants.ATTR_WORKING_DIRECTORY, workingDirectoryText.getText().trim());
	}

	@Override
	public boolean isValid(ILaunchConfiguration configuration) {
		setErrorMessage(null);
		if (commandText.getText().trim().isEmpty()) {
			setErrorMessage("Command is required.");
			return false;
		}
		return true;
	}

	@Override
	public String getName() {
		return "Command";
	}
}
