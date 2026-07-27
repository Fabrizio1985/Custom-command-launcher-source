package it.fabrizio.eclipse.customcommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.actions.CompoundContributionItem;

public class DynamicCustomCommandContribution extends CompoundContributionItem {
	@Override
	protected IContributionItem[] getContributionItems() {
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(CustomCommandConstants.TYPE_ID);
			ILaunchConfiguration[] configurations = manager.getLaunchConfigurations(type);
			Arrays.sort(configurations, this::compare);

			List<IContributionItem> items = new ArrayList<>();
			for (ILaunchConfiguration configuration : configurations) {
				if (configuration.getAttribute(CustomCommandConstants.ATTR_SEPARATOR_BEFORE, false) && !items.isEmpty()) {
					items.add(new Separator());
				}
				items.add(new ActionContributionItem(new Action(configuration.getName()) {
					@Override
					public void run() {
						DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
					}
				}));
			}
			return items.toArray(IContributionItem[]::new);
		} catch (CoreException exception) {
			return new IContributionItem[0];
		}
	}

	private int compare(ILaunchConfiguration left, ILaunchConfiguration right) {
		try {
			int leftOrder = normalize(left.getAttribute(CustomCommandConstants.ATTR_ORDER, 0));
			int rightOrder = normalize(right.getAttribute(CustomCommandConstants.ATTR_ORDER, 0));
			int result = Integer.compare(leftOrder, rightOrder);
			return result != 0 ? result : left.getName().compareToIgnoreCase(right.getName());
		} catch (CoreException exception) {
			return left.getName().compareToIgnoreCase(right.getName());
		}
	}

	private int normalize(int order) {
		return order == 0 ? Integer.MAX_VALUE : order;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
}
