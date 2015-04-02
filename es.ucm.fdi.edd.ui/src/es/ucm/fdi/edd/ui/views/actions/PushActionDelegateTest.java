package es.ucm.fdi.edd.ui.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import es.ucm.fdi.edd.ui.views.GraphvizView;

public class PushActionDelegateTest implements IViewActionDelegate {

	/** pointer to image view */
	public GraphvizView view = null;
	/** Action id of this delegate */
	public String id;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart viewPart) {
		if (viewPart instanceof GraphvizView) {
			this.view = (GraphvizView) viewPart;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		String id = action.getId();
		 
		Canvas imageCanvas = view.getCanvas();
		if (imageCanvas != null) {
			if (id.equals("toolbar.zoomin")) {
				view.zoomIn();
				return;
			} else if (id.equals("toolbar.zoomout")) {
				view.zoomOut();
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// empty
	}
}
