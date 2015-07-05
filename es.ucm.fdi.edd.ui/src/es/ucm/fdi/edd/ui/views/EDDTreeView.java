package es.ucm.fdi.edd.ui.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.view.ExtendedPropertySheetPage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import es.ucm.fdi.edd.ui.views.listeners.EDDViewSelectionListener;
import es.ucm.fdi.edd.ui.views.providers.MNComposedAdapterFactory;
import es.ucm.fdi.edd.ui.views.providers.MNViewContentProvider;
import es.ucm.fdi.edd.ui.views.providers.MNViewLabelProvider;
import es.ucm.fdi.edd.ui.views.providers.TreeObject;
import es.ucm.fdi.edd.ui.views.providers.TreeParent;
import es.ucm.fdi.edd.ui.views.sorter.NameSorter;
import es.ucm.fdi.emf.model.ed2.ED2;
import es.ucm.fdi.emf.model.ed2.Ed2Factory;
import es.ucm.fdi.emf.model.ed2.Leaf;
import es.ucm.fdi.emf.model.ed2.Node;
import es.ucm.fdi.emf.model.ed2.TreeElement;

public class EDDTreeView extends ViewPart implements IAdaptable {

	/** The ID of the view as specified by the extension. */
	public static final String ID = "es.ucm.fdi.edd.ui.views.EDDTreeView";

	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private Action action1;
	private Action action2;
	private Action doubleClickAction;
	
	private TreeParent invisibleRoot;
	private Map<Integer, TreeElement> nodesContentList;
	
	// the listener we register with the selection service 
	private ISelectionListener listener;

	/**
	 * The constructor.
	 */
	public EDDTreeView() {
		super();
		listener = new EDDViewSelectionListener(this);
		nodesContentList = new HashMap<Integer, TreeElement>();
		initialize();
	}
	
	/**
	 * We will set up a dummy model to initialize tree hierarchy. In a real
	 * code, you will connect to a real model and expose its hierarchy.
	 */
	private void initialize() {
		TreeObject to1 = new TreeObject("Leaf 1");
		TreeObject to2 = new TreeObject("Leaf 2");
		TreeObject to3 = new TreeObject("Leaf 3");
		TreeParent p1 = new TreeParent("Parent 1");
		p1.addChild(to1);
		p1.addChild(to2);
		p1.addChild(to3);

		TreeObject to4 = new TreeObject("Leaf 4");
		TreeParent p2 = new TreeParent("Parent 2");
		p2.addChild(to4);

		TreeParent root = new TreeParent("Root");
		root.addChild(p1);
		root.addChild(p2);

		invisibleRoot = new TreeParent("");
		invisibleRoot.addChild(root);
	}
	
	/** 
	 * This is a callback that will allow us to create the viewer and initialize it.
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
//		viewer.setContentProvider(new ViewContentProvider());
//		viewer.setLabelProvider(new ViewLabelProvider());
//		viewer.setInput(getViewSite());
//		viewer.setInput(invisibleRoot);
		
		viewer.setContentProvider(new MNViewContentProvider());
		viewer.setLabelProvider(new MNViewLabelProvider());
		viewer.setInput(getDataModel());
		
		viewer.setSorter(new NameSorter());

		drillDownAdapter = new DrillDownAdapter(viewer);

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "es.ucm.fdi.edd.ui.viewer");
		
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		
		// we're cooperative and also provide our selection
		getSite().setSelectionProvider(viewer);
		
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
	}
	
	@Override
	public void setContentDescription(String description) {
		super.setContentDescription(description);
	}
	
	/**
	 * @param input
	 */
	public void updateContent(ED2 input) {
		if (viewer != null) {
			viewer.setInput(input);
			viewer.expandAll();
			
			nodesContentList.clear();
			walk(input.getTreeElements());
		}
	}
	
	/**
	 * @param list
	 */
	private void walk(EList<TreeElement> list) {
		if (list == null)
			return;
		for (TreeElement item : list) {
			if (item instanceof Node) {
				Node node = (Node) item;
//				System.out.println("Node: " + node.getIndex() + " :: " + node.getName());
				nodesContentList.put(node.getIndex(), node);
				EList<Node> nodes = node.getNodes();
				EList<Leaf> leaves = node.getLeaves();
				EList<TreeElement> treeElementsList = new BasicEList<TreeElement>();
				treeElementsList.addAll(nodes);
				treeElementsList.addAll(leaves);
				walk(treeElementsList);
			} else if (item instanceof Leaf) {
				Leaf leaf = (Leaf) item;
//				System.out.println("Leaf: " + leaf.getIndex() + " :: " + leaf.getName());
				nodesContentList.put(leaf.getIndex(), leaf);
			}
		}
	}
	
	private ED2 getDataModel() {
		Leaf leaf1 = Ed2Factory.eINSTANCE.createLeaf();
		leaf1.setIndex(1);
		leaf1.setName("Leaf 1");
		
		Leaf leaf2 = Ed2Factory.eINSTANCE.createLeaf();
		leaf2.setIndex(2);
		leaf2.setName("Leaf 2");
		
		Leaf leaf3 = Ed2Factory.eINSTANCE.createLeaf();
		leaf3.setIndex(3);
		leaf3.setName("Leaf 3");
		
		Node node1 = Ed2Factory.eINSTANCE.createNode();
		node1.setIndex(4);
		node1.setName("Parent 1");	
		node1.getLeaves().add(leaf1);
		node1.getLeaves().add(leaf2);
		node1.getLeaves().add(leaf3);

		Leaf leaf4 = Ed2Factory.eINSTANCE.createLeaf();
		leaf4.setIndex(1);
		leaf4.setName("Leaf 4");
		
		Leaf _leaf = Ed2Factory.eINSTANCE.createLeaf();
		_leaf.setIndex(1);
		_leaf.setName("Leaf");
		
		Node nodeZ = Ed2Factory.eINSTANCE.createNode();
		nodeZ.setIndex(4);
		nodeZ.setName("Node Z");
		nodeZ.getLeaves().add(_leaf);
		
		Node nodeY = Ed2Factory.eINSTANCE.createNode();
		nodeY.setIndex(4);
		nodeY.setName("Node Y");
		nodeY.getNodes().add(nodeZ);
		
		Node nodeX = Ed2Factory.eINSTANCE.createNode();
		nodeX.setIndex(4);
		nodeX.setName("Node X");	
		nodeX.getNodes().add(nodeY);
		
		Node node2 = Ed2Factory.eINSTANCE.createNode();
		node2.setIndex(4);
		node2.setName("Parent 2");	
		node2.getLeaves().add(leaf4);
		node2.getNodes().add(nodeX);
		
		Leaf leaf = Ed2Factory.eINSTANCE.createLeaf();
		leaf.setIndex(4);
		leaf.setName("Leaf");
		
		TreeElement te = Ed2Factory.eINSTANCE.createNode();
		te.setIndex(1);
		te.setName("TreeElement");

		ED2 root = Ed2Factory.eINSTANCE.createED2();
		root.setName("Root");
		EList<TreeElement> elements = root.getTreeElements();
		elements.add(node1);
		elements.add(node2);
		elements.add(leaf);
		elements.add(te);
		
		return root;
	}

	/**
	 * 
	 */
	private void makeActions() {
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				showMessage("Double-click detected on " + obj.toString());
			}
		};
	}

	/**
	 * 
	 */
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				EDDTreeView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}
	
	/**
	 * @param manager
	 */
	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	/**
	 * 
	 */
	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * 
	 */
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * @param manager
	 */
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}
	
	/**
	 * @param manager
	 */
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {
//			return showOutlineView() ? getContentOutlinePage() : null;
		}
		else if (key.equals(IPropertySheetPage.class)) {
			return getPropertySheetPage();
		}
		else if (key.equals(IGotoMarker.class)) {
			return this;
		}
		
		return super.getAdapter(key);
	}
	
	/**
	 * This accesses a cached version of the property sheet.
	 */
	public IPropertySheetPage getPropertySheetPage() {
		AdapterFactory adapterFactory = MNComposedAdapterFactory.getAdapterFactory();
		CommandStack commandStack = new BasicCommandStack();
		AdapterFactoryEditingDomain editingDomain = new AdapterFactoryEditingDomain(adapterFactory, commandStack, new HashMap<Resource, Boolean>());
		
		PropertySheetPage propertySheetPage = new ExtendedPropertySheetPage(editingDomain) {
				@Override
				public void setSelectionToViewer(List<?> selection) {
//					EddEditor.this.setSelectionToViewer(selection);
//					EddEditor.this.setFocus();
				}

				@Override
				public void setActionBars(IActionBars actionBars) {
					super.setActionBars(actionBars);
//					getActionBarContributor().shareGlobalActions(this, actionBars);
				}
			};
		propertySheetPage.setPropertySourceProvider(new AdapterFactoryContentProvider(adapterFactory));

		return propertySheetPage;
	}
		
	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "EDD Tree View", message);
	}
	
	/**
	 * @param Index
	 */
	public void updateSelectionByIndex(int index) {
		if (nodesContentList != null) {
			TreeElement selectedNode = nodesContentList.get(index);
			if (selectedNode != null) {
				viewer.setSelection(new StructuredSelection(selectedNode));
			}
		}
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	@Override
	public void dispose() {
		// important: We need do unregister our listener when the view is disposed
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(listener);
		super.dispose();
	}
}