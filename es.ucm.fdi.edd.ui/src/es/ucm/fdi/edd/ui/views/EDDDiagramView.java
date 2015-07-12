package es.ucm.fdi.edd.ui.views;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.parts.ContentOutlinePage;

public class EDDDiagramView extends ContentOutlinePage implements IAdaptable {

	public EDDDiagramView(EditPartViewer viewer) {
		super(viewer);
	}

	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
}