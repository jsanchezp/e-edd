package es.ucm.fdi.edd.ui.views;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import es.ucm.fdi.edd.ui.Activator;

public class EDDSequenceDiagramsView extends ViewPart implements IResourceChangeListener, IPartListener2, ISelectionListener {

	private String basePartName;
	private IFile selectedFile;
//	private IProviderDescription providerDefinition;
	
	public EDDSequenceDiagramsView() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		basePartName = getPartName();
		installResourceListener();
		installSelectionListener();
		installPartListener();
		
		initialize();
	}
	
	private void installResourceListener() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	private void installSelectionListener() {
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	private void installPartListener() {
		getSite().getPage().addPartListener(this);
		// tries to load an image for the current active part, if any
		final IWorkbenchPartReference activePartReference = getSite().getPage().getActivePartReference();
		if (activePartReference != null)
			reactToPartChange(activePartReference);
	}
	
	private void initialize() {
		// FIXME 
		Bundle bundle = Platform.getBundle("es.ucm.fdi.edd.graphiti");
		URL fileURL = bundle.getEntry("diagram/Sample.dot");
		File file = null;
		try {
		    file = new File(FileLocator.resolve(fileURL).toURI());
		} catch (URISyntaxException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		IFile iFile = null;
		selectedFile = iFile;
		requestUpdate();		
	}

	@Override
	public void setFocus() {
		// FIXME viewer.getControl().setFocus();
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection structured = (IStructuredSelection) selection;
		if (structured.size() != 1)
			return;
		Object selected = structured.getFirstElement();
		IFile file = (IFile) Platform.getAdapterManager().getAdapter(selected, IFile.class);
		reload(file);
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		// Ignore
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		// Ignore
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		// Ignore
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (selectedFile == null || !selectedFile.exists())
			return;
		if (event.getDelta() == null)
			return;
		IResourceDelta interestingChange = event.getDelta().findMember(selectedFile.getFullPath());
		if (interestingChange != null)
			requestUpdate();
	}
	
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getSite().getPage().removePartListener(this);
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		super.dispose();
	}
	
	private void reactToPartChange(IWorkbenchPartReference part) {
		if (!(part.getPart(false) instanceof IEditorPart))
			return;
		IEditorPart editorPart = (IEditorPart) part.getPart(false);
		if (!getViewSite().getPage().isPartVisible(editorPart))
			return;
//		IGraphicalFileProvider graphicalSource = (IGraphicalFileProvider) editorPart.getAdapter(IGraphicalFileProvider.class);
//		if (graphicalSource != null)
//			selectedFile = graphicalSource.getGraphicalFile();
//		else 
//			selectedFile = null;
		if (selectedFile == null) {
			IFile asFile = (IFile) editorPart.getAdapter(IFile.class);
			if (asFile == null)
				asFile = (IFile) editorPart.getEditorInput().getAdapter(IFile.class);
			if (asFile == null)
				return;
			selectedFile = asFile;
		}
		requestUpdate();
	}
	
	private void requestUpdate() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (getSite() == null || !EDDSequenceDiagramsView.this.getSite().getPage().isPartVisible(EDDSequenceDiagramsView.this))
					// don't do anything if we are not showing
					return;
				reload(selectedFile);
			}
		});
	}

	private void reload(IFile file) {
		setPartName(basePartName);
//		this.providerDefinition = null;
		if (file == null || !file.exists())
			return;
		selectedFile = null;
//		if (viewer.getContentProvider() != null)
//			// to avoid one provider trying to interpret an incompatible input
//			viewer.setInput(null);
		IContentDescription contentDescription = null;
		try {
			contentDescription = file.getContentDescription();
		} catch (CoreException e) {
			if (Platform.inDebugMode())
				Activator.logUnexpected(null, e);
		}
		if (contentDescription == null || contentDescription.getContentType() == null)
			return;
//		IProviderDescription providerDefinition =
//						ContentSupport.getContentProviderRegistry().findContentProvider(
//										contentDescription.getContentType(), IGraphicalContentProvider.class);
//		if (providerDefinition == null) {
//			return;
//		}
		setPartName(basePartName + " - " + file.getName());
		selectedFile = file;
//		this.providerDefinition = providerDefinition;
//		IGraphicalContentProvider provider = (IGraphicalContentProvider) providerDefinition.getProvider();
//		viewer.setContentProvider(provider);
//		Object input = providerDefinition.read(file);
//		viewer.setInput(input);
	}
}
