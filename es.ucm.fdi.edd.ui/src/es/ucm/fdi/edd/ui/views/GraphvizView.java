package es.ucm.fdi.edd.ui.views;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import com.abstratt.content.ContentSupport;
import com.abstratt.content.IContentProviderRegistry.IProviderDescription;
import com.abstratt.imageviewer.GraphicalViewer;
import com.abstratt.imageviewer.IGraphicalContentProvider;
import com.abstratt.imageviewer.IGraphicalFileProvider;

import es.ucm.fdi.edd.ui.Activator;

/**
 * A view that wraps a {@link GraphicalViewer}.
 */
public class GraphvizView extends ViewPart implements IResourceChangeListener, IPartListener2, ISelectionListener {
	
	public final static String VIEW_ID = "es.ucm.fdi.edd.ui.views.GraphvizView";

	private static final String EDD_PROJECT = "TmpEDD";
//	private static final String SAMPLE_URI = "platform:/plugin/es.ucm.fdi.edd.graphiti/diagram/Sample.dot";
	
	private Canvas canvas;
	private GraphicalViewer viewer;
	private String basePartName;
	private IFile selectedFile;
	private IProviderDescription providerDefinition;

	/**
	 * The constructor.
	 */
	public GraphvizView() {
		super();
	}

	/**
	 * This is a callback that will allow us to create the viewer and
	 * initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		basePartName = getPartName();
		canvas = new Canvas(parent, SWT.NONE);
		viewer = new GraphicalViewer(canvas);
		installResourceListener();
		installSelectionListener();
		installPartListener();
		
		initialize();
	}
	
	private void initialize() {
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
		
		IFile iFile = createIFile(file);
		selectedFile = iFile;
		requestUpdate();
	}
	
	private IFile createIFile(File file) {
		validateProject();
		try {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(EDD_PROJECT); //External Files 
			IPath location = new Path(file.getAbsolutePath());
			IFile iFile = project.getFile(location.lastSegment());
			if (!iFile.exists()) {
				iFile.createLink(location, IResource.NONE, null);
			}
			else {
				iFile.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
			return iFile;
		} catch (CoreException e) {
    		e.printStackTrace();
    		return null;
		}
	}
	
	private void validateProject() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(EDD_PROJECT); //External Files 
		try {
			if (!project.exists()) {
				project.create(new NullProgressMonitor());
			}
			else {
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
			
			if (!project.isOpen()) {
			    project.open(new NullProgressMonitor());
			}
		} catch (CoreException e) {
    		e.printStackTrace();
		}	
	}
	
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getSite().getPage().removePartListener(this);
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		super.dispose();
	}

	private void installPartListener() {
		getSite().getPage().addPartListener(this);
		// tries to load an image for the current active part, if any
		final IWorkbenchPartReference activePartReference = getSite().getPage().getActivePartReference();
		if (activePartReference != null)
			reactToPartChange(activePartReference);
	}

	private void installResourceListener() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	private void installSelectionListener() {
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

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

	public void partActivated(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partClosed(IWorkbenchPartReference partRef) {
		//
	}

	public void partDeactivated(IWorkbenchPartReference partRef) {
		//
	}

	public void partHidden(IWorkbenchPartReference partRef) {
		//
	}

	public void partInputChanged(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partOpened(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partVisible(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	private void reactToPartChange(IWorkbenchPartReference part) {
		if (!(part.getPart(false) instanceof IEditorPart))
			return;
		IEditorPart editorPart = (IEditorPart) part.getPart(false);
		if (!getViewSite().getPage().isPartVisible(editorPart))
			return;
		IGraphicalFileProvider graphicalSource =
						(IGraphicalFileProvider) editorPart.getAdapter(IGraphicalFileProvider.class);
		if (graphicalSource != null)
			selectedFile = graphicalSource.getGraphicalFile();
		else 
			selectedFile = null;
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

	private void reload(IFile file) {
		setPartName(basePartName);
		this.providerDefinition = null;
		if (file == null || !file.exists())
			return;
		selectedFile = null;
		if (viewer.getContentProvider() != null)
			// to avoid one provider trying to interpret an
			// incompatible input
			viewer.setInput(null);
		IContentDescription contentDescription = null;
		try {
			contentDescription = file.getContentDescription();
		} catch (CoreException e) {
			if (Platform.inDebugMode())
				Activator.logUnexpected(null, e);
		}
		if (contentDescription == null || contentDescription.getContentType() == null)
			return;
		IProviderDescription providerDefinition =
						ContentSupport.getContentProviderRegistry().findContentProvider(
										contentDescription.getContentType(), IGraphicalContentProvider.class);
		if (providerDefinition == null) {
			return;
		}
		setPartName(basePartName + " - " + file.getName());
		selectedFile = file;
		this.providerDefinition = providerDefinition;
		IGraphicalContentProvider provider = (IGraphicalContentProvider) providerDefinition.getProvider();
		viewer.setContentProvider(provider);
		viewer.setInput(providerDefinition.read(file));
	}

	private void requestUpdate() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (getSite() == null || !GraphvizView.this.getSite().getPage().isPartVisible(GraphvizView.this))
					// don't do anything if we are not showing
					return;
				reload(selectedFile);
			}
		});

	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (selectedFile == null || !selectedFile.exists())
			return;
		if (event.getDelta() == null)
			return;
		IResourceDelta interestingChange = event.getDelta().findMember(selectedFile.getFullPath());
		if (interestingChange != null)
			requestUpdate();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public IProviderDescription getContentProviderDescription() {
		return providerDefinition;
	}
	
	public IFile getSelectedFile() {
		return selectedFile;
	}
}