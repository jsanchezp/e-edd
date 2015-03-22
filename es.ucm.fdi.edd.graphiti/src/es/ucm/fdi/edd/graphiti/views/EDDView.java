package es.ucm.fdi.edd.graphiti.views;

import org.eclipse.emf.common.util.URI;
import org.eclipse.graphiti.ui.editor.DiagramComposite;
import org.eclipse.graphiti.ui.editor.DiagramEditorInput;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;


public class EDDView extends ViewPart {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "es.ucm.fdi.edd.graphiti.views.EDDView";
	private static final String SAMPLE_URI = "platform:/plugin/es.ucm.fdi.edd.graphiti/diagram/SampleEdd.diagram#/0";
	private static final String PROVIDER_ID = "es.ucm.fdi.edd.graphiti.diagram.eddDiagramTypeProvider";
	
	private DiagramComposite diagramComposite;

	@Override
	public void createPartControl(Composite parent) {
		diagramComposite = new DiagramComposite(this, parent, SWT.NONE);
		diagramComposite.setLayout(new FillLayout());
		diagramComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		URI uri = URI.createURI(SAMPLE_URI);
		diagramComposite.setInput(new DiagramEditorInput(uri, PROVIDER_ID));
	}

	@Override
	public void setFocus() {
		diagramComposite.setFocus();
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
		Object returnObj = null;

		if (diagramComposite != null && !diagramComposite.isDisposed()) {
			returnObj = diagramComposite.getAdapter(type);
		}

		if (returnObj != null) {
			return returnObj;
		}

		return super.getAdapter(type);
	}
}