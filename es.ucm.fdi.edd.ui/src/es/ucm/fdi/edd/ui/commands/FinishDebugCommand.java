package es.ucm.fdi.edd.ui.commands;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.HandlerUtil;

import es.ucm.fdi.edd.ui.connection.ConnectionManager;
import es.ucm.fdi.edd.ui.views.EDDebugView;

/**
 * {@link DisconnectComand} which corresponds to "disconnect" action, subscribes
 * to listen {@link ConnectionManager} state changes and sets its sate by
 * calling <code>setBaseEnabled</code>. At the beginning, its sate is disabled:
 * <code>setBaseEnabled(false)</code>.
 */
public class FinishDebugCommand extends AbstractHandler implements Observer {
	
	/**
	 * 
	 */
	public FinishDebugCommand() {
		super();
		ConnectionManager.getInstance().addObserver(this);
        setBaseEnabled(false); // by default
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IViewPart part = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(EDDebugView.VIEW_ID);
		if (part instanceof EDDebugView) {
			EDDebugView view = (EDDebugView)part;
			boolean result = view.stopDebugger();
			if (result) {
				ConnectionManager.getInstance().disconnect();
				view.refresh();
			}
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable o, Object arg) {
		ConnectionManager connectionManager = (ConnectionManager) o;
		setBaseEnabled(connectionManager.isConnected());
	}
}