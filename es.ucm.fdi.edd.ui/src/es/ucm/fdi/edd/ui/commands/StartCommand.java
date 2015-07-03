package es.ucm.fdi.edd.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;

import es.ucm.fdi.edd.ui.dialogs.ErlangFileDialog;
import es.ucm.fdi.edd.ui.views.EDDebugView;

public class StartCommand extends AbstractHandler {
	
	public StartCommand() {
		super();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IViewPart part = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(EDDebugView.VIEW_ID);
		if (part instanceof EDDebugView) {
			EDDebugView view = (EDDebugView)part;
			
			ErlangFileDialog dialog = new ErlangFileDialog(view.getSite().getShell());
			if (dialog.open() == Window.OK) {
		    	IFile iFile = (IFile) dialog.getFirstResult();
		    	System.out.println(iFile.getLocation());
//		    	view.setLocationText(iFile.getLocation());
		    	view.setLocationText(iFile.getParent().getFullPath());
		    	view.startDebugger();
			}
		}
		
		return null;
	}
	
}
