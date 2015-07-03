package es.ucm.fdi.edd.ui.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import es.ucm.fdi.edd.ui.Activator;

public class ErlangFileDialog extends ElementTreeSelectionDialog{
	
	private static final String TITLE = "Open File";
	private static final String MESSAGE = "Choose an Erlang file for debug";
	
	public ErlangFileDialog(Shell parent) {
		super(parent, new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
		initialize();
	}

	private void initialize() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		setTitle(TITLE);
		setMessage(MESSAGE);
	    setInput(root);
	    setAllowMultiple(false);
	    setValidator(new ErlangStatusValidator());
	    addFilter(new ErlangViewerFilter());
	}
	
	@Override
	public Object getFirstResult() {
		Object selected = super.getFirstResult();
		if (selected instanceof IFile) {
			IFile iFile = (IFile) selected;
			String srcName = iFile.getName();
			String binName = srcName.replace(".erl", ".beam");
			
			String srcPath = iFile.getLocation().toPortableString();
			String binPath1 = srcPath.replace("/src/", "/ebin/");
			String binPath2 = binPath1.replace(".erl", ".beam");
			
			IProject iProject = iFile.getProject();
//			IResource beamFile1 = iProject.getFile(binName);
//			System.out.println(beamFile1);
			
			IResource beamFile = iProject.findMember(binPath2);
			//D:\workspace\runtime-tests\EDDAckermann\ebin\ackermann.beam
			System.out.println(beamFile);
		}
		
		return selected;
	}
}

final class ErlangStatusValidator implements ISelectionStatusValidator {
	@Override
	public IStatus validate(Object[] selection) {
	    if (selection != null && selection.length == 1) {
	        if (selection[0] instanceof IFile) {
	            return new Status(IStatus.OK, Activator.PLUGIN_ID, ((IFile)selection[0]).getFullPath().toString());
	        }
	    }
	    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Please select a valid *.erl file.");
	}
}

final class ErlangViewerFilter extends ViewerFilter {
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		try {
			if (element instanceof IProject) {
				IProject iProject = (IProject)element;
				return iProject.hasNature("org.erlide.core.erlnature");
			}
			if (element instanceof IFolder) {
				IFolder iFolder = (IFolder)element;
				IResource[] members;
				members = iFolder.members();
//				return members.length > 0;
				for (IResource iResource : members) {
					String extension = iResource.getFileExtension();
					return extension == null ? false : extension.equals("erl");
				}
				return false;
			}

			if (element instanceof IFile) {
				IFile iFile = (IFile)element;
				String extension = iFile.getFileExtension();
				return extension == null ? false : extension.equals("erl");	
			}
		} catch (CoreException e) {
			return false;
		}
		
		return true;
	}
}