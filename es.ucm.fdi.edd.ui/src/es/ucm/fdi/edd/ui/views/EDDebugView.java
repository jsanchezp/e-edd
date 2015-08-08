package es.ucm.fdi.edd.ui.views;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.ScrolledPageBook;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.ViewPart;
import org.erlide.engine.internal.model.erlang.ErlFunction;
import org.erlide.engine.model.IParent;
import org.erlide.engine.model.erlang.IErlModule;
import org.erlide.engine.model.root.ErlElementKind;
import org.erlide.engine.model.root.IErlElement;
import org.erlide.ui.editors.erl.ErlangEditor;
import org.erlide.ui.editors.util.EditorUtility;
import org.erlide.ui.util.ErlModelUtils;
import org.erlide.util.StringUtils;

import es.ucm.fdi.edd.core.exception.EDDException;
import es.ucm.fdi.edd.ui.Activator;
import es.ucm.fdi.edd.ui.Messages;
import es.ucm.fdi.edd.ui.dialogs.ErlangFileDialog;
import es.ucm.fdi.edd.ui.emf2gv.StandaloneApp;
import es.ucm.fdi.edd.ui.views.listeners.EDDViewSelectionListener;
import es.ucm.fdi.edd.ui.views.utils.EDDHelper;
import es.ucm.fdi.emf.model.ed2.Model;

@SuppressWarnings("restriction")
public class EDDebugView extends ViewPart {

	public final static String VIEW_ID = "es.ucm.fdi.edd.ui.views.EDDebugView";
	
	private Section sectionDebugger;
	private Section sectionQuestion;
	private Composite contentQuestion;

	private FormToolkit toolkit;
	private IMessageManager mmng;
	private ScrolledForm scrolledForm;
	private ScrolledPageBook pageBook;
	private Text buggyCallText;
	private Text locationText;
	
	/** Remember the selected index. */
	private Integer index = 0;
	private Integer total = 0;
	
	private IFile debugFile;
	private EDDHelper helper;
	
	// the listener we register with the selection service 
	private ISelectionListener listener;
	private final Set<ISelectionChangedListener> selectionChangeListeners = new LinkedHashSet<ISelectionChangedListener>();
	
	/**
	 * The constructor
	 */
	public EDDebugView() {
		super();
		listener = new EDDViewSelectionListener(this);
		helper = new EDDHelper();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		
		scrolledForm = toolkit.createScrolledForm(parent);		
		scrolledForm.setText(Messages.getString("EDDebugView.title"));
		scrolledForm.setBackgroundImage(Activator.getDefault().getImage(Activator.IMG_FORM_BG));
		
		toolkit.decorateFormHeading(scrolledForm.getForm());
		createNotificationsArea();
		showFormMenu();
		
		ToolBarManager manager = (ToolBarManager) scrolledForm.getToolBarManager();
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		
		IMenuService menuService = (IMenuService) getSite().getService(IMenuService.class);
//		menuService.populateContributionManager(manager, "formheading:" + getSite().getId());		
		menuService.populateContributionManager(manager, "toolbar:formsEDDebugToolBar");
//		manager.add(new Action("This is the toolbar") { });
		manager.update(true);
		
		Composite body = scrolledForm.getBody();
		body.setLayout(new GridLayout(1, false));
		
		createContent(body);

		toolkit.paintBordersFor(body);
		
		// we're cooperative and also provide our selection
		getSite().setSelectionProvider(new ISelectionProvider() {
			@Override
			public void setSelection(ISelection newSelection) {
//				System.out.println("Selection <-- " + newSelection);
				for (ISelectionChangedListener selectionChangedListener : selectionChangeListeners) {  
				   selectionChangedListener.selectionChanged(new SelectionChangedEvent(this, newSelection));  
				} 
			}
			
			@Override
			public ISelection getSelection() {
				ISelection selection = new StructuredSelection(index);
//				System.out.println("Selection --> " + selection);
				return selection;
			}
			
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				selectionChangeListeners.add(listener);
			}
			
			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				selectionChangeListeners.remove(listener);
			}
		});
				
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.ViewPart#setContentDescription(java.lang.String)
	 */
	@Override
	public void setContentDescription(String description) {
		super.setContentDescription(description);
	}

	/**
	 * 
	 */
	private void createNotificationsArea() {
		Form form = scrolledForm.getForm();
//		form.setHeadClient(toolkit.createButton(form.getHead(), "This is the head client", SWT.PUSH));
		form.addMessageHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				String title = e.getLabel();
				Object href = e.getHref();
				
				Shell shell = new Shell(scrolledForm.getShell(), SWT.ON_TOP | SWT.TOOL);
				shell.setImage(getImage(scrolledForm.getMessageType()));
				shell.setText(title);
				shell.setLayout(new FillLayout());
				
				FormText text = toolkit.createFormText(shell, true);
				configureFormText(form, text);
				if (href instanceof IMessage[]) {
					text.setText(createFormTextContent((IMessage[]) href),true, false);
				}
				
				Point hl = ((Control) e.widget).toDisplay(0,0);
				hl.x += 10;
				hl.y += 10;
				shell.setLocation(hl);
				shell.pack();
				shell.open();
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
				// 
			}
			
			@Override
			public void linkExited(HyperlinkEvent e) {
				// 
			}
		});
	}
	
	/**
	 * @param form
	 * @param text
	 */
	private void configureFormText(final Form form, FormText text) {
		text.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				String is = (String)e.getHref();
				try {
					int index = Integer.parseInt(is);
					IMessage[] messages = form.getChildrenMessages();
					if (messages != null) {
						if (index>=0 && messages.length>=0 && index<=messages.length) { 
							IMessage message = messages[index];
							Control c = message.getControl();
							((FormText)e.widget).getShell().dispose();
							if (c!=null)
								c.setFocus();
						}
					}
				}
				catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			}
		});
		text.setImage("error", getImage(IMessageProvider.ERROR));
		text.setImage("warning", getImage(IMessageProvider.WARNING));
		text.setImage("info", getImage(IMessageProvider.INFORMATION));
	}
	
	/**
	 * @param type
	 * @return
	 */
	private Image getImage(int type) {
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		switch (type) {
			case IMessageProvider.ERROR:
				return sharedImages.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			case IMessageProvider.WARNING:
				return sharedImages.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			case IMessageProvider.INFORMATION:
				return sharedImages.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
		
		return null;
	}	
	
	/**
	 * @param messages
	 * @return
	 */
	private String createFormTextContent(IMessage[] messages) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("<form>");
		for (int i = 0; i < messages.length; i++) {
			IMessage message = messages[i];
			pw.print("<li vspace=\"false\" style=\"image\" indent=\"16\" value=\"");
			switch (message.getMessageType()) {
				case IMessageProvider.ERROR:
					pw.print("error");
					break;
				case IMessageProvider.WARNING:
					pw.print("warning");
					break;
				case IMessageProvider.INFORMATION:
					pw.print("info");
					break;
			}
			pw.print("\"> <a href=\"");
			pw.print(i+"");
			pw.print("\">");
//			if (message.getPrefix() != null)
//				pw.print(message.getPrefix());
			pw.print(message.getMessage());
			pw.println("</a></li>");
		}
		pw.println("</form>");
		pw.flush();
		
		return sw.toString();
	}
			
	/**
	 * @return
	 */
	private void showFormMenu() {
//		Form form = scrolledForm.getForm();
//		form.getMenuManager().add(new Action("This is the menu") { });
	}

	/**
	 * Create the form area. 
	 * 
	 * @param body
	 */
	private void createContent(Composite body) {
		sectionDebugger = createSection(body, Messages.getString("EDDebugView.sectionDebugger.title"), Messages.getString("EDDebugView.sectionDebugger.desc"), true);
		Composite contentDebugger = createSectionClient(sectionDebugger);
		
		mmng = scrolledForm.getMessageManager();
		mmng.setAutoUpdate(true);
		
		GridLayout fileSelectorLayout = new GridLayout(3, false);
		fileSelectorLayout.marginHeight = 0;
		fileSelectorLayout.marginWidth = 0;
	    
		Composite fileSelector = toolkit.createComposite(contentDebugger);
		fileSelector.setLayout(fileSelectorLayout);
		fileSelector.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		toolkit.createLabel(fileSelector, "File: ");
		locationText = toolkit.createText(fileSelector, "", SWT.BORDER | SWT.SINGLE);
//		locationText.setText("/EDDAckermann/src/ackermann.erl");
		locationText.setEditable(false);
		locationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		locationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
//				dialogChanged();
			}
		});
		Button button = toolkit.createButton(fileSelector, "Browse...", SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();				
			}
		});
		
		buggyCallText = createDecoratedTextField(Messages.getString("EDDebugView.textLabel"), contentDebugger, true);
//		buggyCallText.setText("ackermann:main([3,4])");
		buggyCallText.addKeyListener(new KeyListener() {			
			@Override
			public void keyPressed(KeyEvent event) {
				final boolean ctrlOrCommandPressed = (event.stateMask & SWT.MOD1) == SWT.MOD1;
				// Ctrl + Space
				if (ctrlOrCommandPressed && event.keyCode == SWT.SPACE) {
					// ErlangSourceViewerConfiguration
//                    buggyCallText.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
                }
				// Enter
				else if (event.keyCode == SWT.CR) {
//					startDebugger();
				}
			}

			@Override
			public void keyReleased(KeyEvent event) {
				// Empty
			}
		});
		
		Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
		final ControlDecoration deco = new ControlDecoration(buggyCallText, SWT.TOP | SWT.LEFT);
		deco.setDescriptionText("Use Ctrl+Space to see possible values");
		deco.setImage(image);
		deco.setShowOnlyOnFocus(false);
		
		buggyCallText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text text = (Text) e.getSource();
				if (!text.getText().isEmpty()) {
					deco.hide();
				} else {
					deco.show();
				}
			}
		});
		
		// help the user with the possible inputs "." and "#" activate the content proposals
		char[] autoActivationCharacters = new char[] { '.', '#' };
		try {
			KeyStroke keyStroke = KeyStroke.getInstance("Ctrl+Space");
			@SuppressWarnings("unused")
			ContentProposalAdapter adapter = new ContentProposalAdapter(buggyCallText, 
				  new TextContentAdapter(), 
				  new SimpleContentProposalProvider(new String[] {"ackermann:main([3,4])", "merge:mergesort([b,a], fun merge:comp/2)", "merge:mergesort([o,h,i,o], fun merge:comp/2)", "ProposalOne", "ProposalTwo", "ProposalThree"}),
				  keyStroke, autoActivationCharacters);
		} catch (ParseException e) {
			e.printStackTrace();
		} 

		sectionQuestion = createSection(body, Messages.getString("EDDebugView.sectionQuestion.title"), Messages.getString("EDDebugView.sectionQuestion.desc"), false);
		contentQuestion = createSectionClient(sectionQuestion);
		sectionQuestion.setVisible(false);
	}
	
	/**
	 * Ensures that both text fields are set.
	 */
	private void dialogChanged() {
		String fileName = locationText.getText();
		String containerName = fileName.substring(0, fileName.lastIndexOf("/"));
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource iFile = root.findMember(new Path(fileName));
		IResource container = root.findMember(new Path(containerName));
		if (container != null) {
			if (container.getName().length() == 0) {
				updateStatus("File container must be specified");
				return;
			}
			if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
				updateStatus("File container must exist");
				return;
			}
			if (!container.isAccessible()) {
				updateStatus("Project must be writable");
				return;
			}
		}

		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		/*if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus(mmng, "File name must be valid");
			return;
		}*/
		int dotLoc = fileName.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = fileName.substring(dotLoc + 1);
			if (ext.equalsIgnoreCase("erl") == false) {
				updateStatus("File extension must be \"erl\"");
				return;
			}
		}
		
		updateStatus(null);
	}
	
	private void updateStatus(String message) {
		if (message != null) {
			mmng.addMessage("textFileType",	message, null, IMessageProvider.ERROR, locationText);
		} else {
			mmng.removeMessage("textFileType", locationText);
		}
	}

	/**
	 * Uses the Erlang file dialog to choose the new value for the erlang file field.
	 */
	private void handleBrowse() {
		ErlangFileDialog dialog = new ErlangFileDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			IFile iFile = (IFile) dialog.getFirstResult();
			setDebugFile(iFile);
		}
	}
	
	/**
	 * Uses the standard container selection dialog to choose the new value for the container field.
	 */
	protected void handleContainerBrowse() {
		IWorkspaceRoot root = Activator.getRoot();
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(scrolledForm.getShell(), root, false, "Select new file container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				Path path = (Path) result[0];
//				IResource container = root.findMember(path);
//				File file = container.getFullPath().toFile();
				locationText.setText(path.toPortableString());
			}
		}
	}
	
	/**
	 * Create a new section part. 
	 * 
	 * @param body
	 * @param title
	 * @param description
	 * @param shouldGiveUpVerticalSpaceWhenFolded
	 * @return
	 */
	private Section createSection(final Composite body, String title, String description, boolean expanded) {
		final Section section = toolkit.createSection(body, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED | Section.DESCRIPTION);
	    section.setText(title);
	    section.setDescription(description);
	    section.addExpansionListener(new ExpansionAdapter() {
	        public void expansionStateChanged(ExpansionEvent e) {
	            scrolledForm.reflow(true);
	            //scrolledForm.layout(true);
	        }
	    });
	    
	    if (expanded)
	    	section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    else
	    	section.setLayoutData(new GridData(GridData.FILL_BOTH));
	    
	    return section;
	}

	/**
	 * @param section
	 * @return
	 */
	private Composite createSectionClient(final Section section) {
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout(1, false));
		sectionClient.setLayoutData(new GridData(GridData.FILL_BOTH));
//		sectionClient.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_CYAN));		
		
		section.setClient(sectionClient);
		section.pack();
		return sectionClient;
	}
	
	/**
	 * @param label
	 * @param parent
	 * @param mmng
	 * @param b 
	 */
	private Text createDecoratedTextField(String label, Composite parent, boolean multi) {
		toolkit.createLabel(parent, label);
		final Text text;
		if (multi) {
			GridData gdTextArea = new GridData(GridData.FILL_HORIZONTAL);
			gdTextArea.heightHint = 50;
			
			text = toolkit.createText(parent, "", SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
			text.setLayoutData(gdTextArea);
		}
		else {
			text = toolkit.createText(parent, "");
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String s = text.getText();
				int length = s.length();
				
				// flag length
				if (length > 0 && length <= 5) {
					mmng.addMessage("textLength", "Text is longer than 0 characters", null, IMessageProvider.INFORMATION, text);
				} else if (length > 5 && length <= 10) {
					mmng.addMessage("textLength", "Text is longer than 5 characters", null,	IMessageProvider.WARNING, text);
				} else if (length > 10) {
					mmng.addMessage("textLength", "Text is longer than 10 characters", null, IMessageProvider.ERROR, text);
				} else {
					mmng.removeMessage("textLength", text);
				}
				
				// flag type
				boolean badType = false;
				for (int i = 0; i < length; i++) {
					if (!Character.isLetter(s.charAt(i))) {
						badType = true;
						break;
					}
				}
				if (badType) {
					mmng.addMessage("textType",	"Text must only contain letters", null,	IMessageProvider.ERROR, text);
				} else {
					mmng.removeMessage("textType", text);
				}
			}
		});
		
		return text;
	}
	
	/**
	 * @param parent
	 * @param tabs
	 * @throws EDDException 
	 */
	public void createPlainTabs(Composite parent, int tabs) throws EDDException {
	    GridLayout layout = new GridLayout(tabs, true);
	    layout.marginHeight = 0;
	    
	    // Add the page book
	    pageBook = new ScrolledPageBook(parent);
	    pageBook.setLayoutData(new GridData(GridData.FILL_BOTH));
	 
	    // The listener when the user clicks on a "tab"
//	    Listener listener = new Listener() {
//	        @Override
//	        public void handleEvent(Event event) {
//	            Object data = event.widget.getData();           
//	            pageBook.showPage(data);
//	            
//	        	// Remember the last selected index
//				EDDebugView.this.index = (Integer) data;
//	        }
//	    };
	    
//	    Composite container = new Composite(parent, SWT.NONE);
//		container.setLayout(layout);
//	    container.setLayoutData(new GridData(SWT.CENTER, SWT.DEFAULT, true, false));
//	    container.setBackground(Display.getDefault().getSystemColor( SWT.COLOR_DARK_GREEN ));
	    
	    // Register the pages and bind it all
	    for(int i=0; i<tabs; i++) {
//	    	Label label = new Label(container, SWT.BORDER);
//	        label.setText("Item " + i);
//	        label.setData(i);
//	        label.addListener( SWT.MouseDown, listener );
	        
	    	Composite pageContainer = pageBook.getContainer();
	        pageBook.registerPage(i, createTabContent(pageContainer, helper.getQuestion(i)));
//			pageContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));	        
	    }
	 
	    // Force to display the first tab
//	    pageBook.showPage(0);
//	    updateSelection(0);
	}
	
	/**
	 * Creates the content of a tab.
	 * 
	 * @param parent the parent
	 */
	private Composite createTabContent(Composite parent, String message) {
		GridLayout layout = new GridLayout(1, false);
	    layout.marginHeight = 5;
	    
		Composite content = toolkit.createComposite(parent);
		content.setLayout(layout);
		content.setLayoutData(new GridData(GridData.FILL_BOTH));
//		content.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA));
		
		GridData gdTextArea = new GridData(GridData.FILL_HORIZONTAL);
		gdTextArea.heightHint = 50;
		
		toolkit.createLabel(content, "Please answer the next questions.", SWT.NONE);
//		Text text = createDecoratedTextField("", content, true);
		Text text = toolkit.createText(content, "", SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		text.setLayoutData(gdTextArea);
		text.setText(message);
		text.setEditable(false);

	    Composite buttonsComposite = toolkit.createComposite(content);
	    buttonsComposite.setLayout(new GridLayout(5, false));
	    buttonsComposite.setLayoutData(new GridData(SWT.END, SWT.DEFAULT, true, false));
//	    radioContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
	    
//		toolkit.createButton(buttonsComposite, Messages.getString("EDDebugView.buttonYes"), SWT.RADIO);		
		Button btnY = toolkit.createButton(buttonsComposite, Messages.getString("EDDebugView.buttonYes"), SWT.PUSH);
		btnY.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
//					helper.setAnswer("y"); // Yes
					waitForNextQuestionAndUpdate("y");
					break;
				}
			}
		});
		Button btnN = toolkit.createButton(buttonsComposite, Messages.getString("EDDebugView.buttonNo"), SWT.PUSH);
		btnN.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
//					helper.setAnswer("n"); // No
					waitForNextQuestionAndUpdate("n");
					break;
				}
			}
		});
		Button btnT = toolkit.createButton(buttonsComposite, Messages.getString("EDDebugView.buttonTrusted"), SWT.PUSH);
		btnT.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
//					helper.setAnswer("t"); // Trusted
					waitForNextQuestionAndUpdate("t");
					break;
				}
			}
		});
		Button btnD = toolkit.createButton(buttonsComposite, Messages.getString("EDDebugView.buttonDontKnow"), SWT.PUSH);
		btnD.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
//					helper.setAnswer("d"); // Don't know
					waitForNextQuestionAndUpdate("d");
					break;
				}
			}
		});
		Button btnI = toolkit.createButton(buttonsComposite, Messages.getString("EDDebugView.buttonInadmissible"), SWT.PUSH);
		btnI.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
//					helper.setAnswer("i"); // Inadmissible
					waitForNextQuestionAndUpdate("i");
					break;
				}
			}
		});
		
		return content;
	}
	
	private void waitForNextQuestionAndUpdate(String sentence) {
		try {
//			if (helper.isBuggyNode()) {
//				int buggyNodeIndex = helper.getBuggyNode();
//				MessageDialog.openInformation(getSite().getShell(), "EDD - Information", "Se ha detectado el 'buggy node' es la pregunta: " + buggyNodeIndex);
//			}
//			else {
			{
				boolean band = helper.setAnswer(sentence); // Yes
				if (band) {
					//FIXME Revisar buggy_node...
					int buggyNodeIndex = helper.getBuggyNode();
					MessageDialog.openInformation(getSite().getShell(), "EDD - Information", "Se ha detectado el 'buggy node' es la pregunta: " + buggyNodeIndex);
					helper.stopEDDebugger();
				}
				
				//FIXME next...
				Thread.sleep(1000); // 1s.
				int goToIndex = helper.getCurrentQuestion();
				updateSelection(goToIndex);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (EDDException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	/**
	 * Sets the debug file.
	 * 
	 * @param iFile
	 * 			The debug file
	 */
	private void setDebugFile(IFile iFile) {
		debugFile = iFile;
		String path = iFile != null ? iFile.getFullPath().toPortableString() : ""; 
		locationText.setText(path);
	}
	
	/**
	 * Gets the debug file.
	 * 
	 * @return the debug file.
	 */
	public IFile getDebugFile() {
		return debugFile;
	}
	
	public String getBuggyCall() {
		return buggyCallText.getText();
	}
	
	public Integer getIndex() {
		return index;
	}

	/**
	 * @param index
	 */
	public void updateSelection(Integer index) {
		if (pageBook != null) {
			try {
				this.index = index;
				pageBook.showPage(index);
				sectionQuestion.setText(Messages.getString("EDDebugView.sectionQuestion.title") + " " + index + "/" + total );
				
				// Forzar un changeListener()...
				getSite().getSelectionProvider().setSelection(new StructuredSelection(index));
				
				String msg = helper.getQuestion(index);
				String questionUnformated = helper.getInfoQuestionUnformated();
				int clause = helper.getInfoClause();
				String file = helper.getInfoFile();
				int line = helper.getInfoLine();
				selectAndReveal(msg, questionUnformated, clause, file, line);
				
				String dotContent = helper.buildDOT(index, false);
				writeDotFile(dotContent);
				
				IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(GraphvizView.VIEW_ID);
				if (part instanceof GraphvizView) {
					GraphvizView view = (GraphvizView) part;
					view.requestUpdate();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (EDDException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void selectAndReveal(String message, String questionUnformated, int clause, String file, int line) {
		try {
			IFile erlFile = ResourcesPlugin.getWorkspace().getRoot().getFile(debugFile.getFullPath());
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart editorPart = IDE.openEditor(page, erlFile);
			ErlangEditor editor = (ErlangEditor) editorPart;
			
			int fxIndex = message.indexOf(":");
			int startIndex = message.indexOf("(");
			int endIndex = message.indexOf(")");
			String fxName = message.substring(fxIndex+1, startIndex);
			int fxArity = message.substring(startIndex+1, endIndex).split(",").length;
			System.out.println("Function: " + fxName + "/" + fxArity);
			
			IErlModule fModule = ErlModelUtils.getModule(editor.getEditorInput());
            if (fModule != null) {
                fModule.open(null);
            }
            List<IErlElement> children = fModule.getChildren();
            for (IErlElement erlElement : children) {
            	String normalizedName = StringUtils.normalizeSpaces(erlElement.toString());
				System.out.println(normalizedName);
            	
            	ErlElementKind kind = erlElement.getKind();
            	String name = erlElement.getName();
            	switch (kind) {
					case ATTRIBUTE:
						break;
					case EXPORT:
						break;
					case FUNCTION: {
						if (name.equals(fxName)) {
							ErlFunction erlFunction = (ErlFunction) erlElement;
							if (fxArity == erlFunction.getArity()) {
								line+= erlFunction.getLineStart();
							}
						}
						break;
					}

					default:
						break;
				}
            	
            	if (erlElement instanceof IParent) {
            		IParent p = (IParent) erlElement;
            		List<IErlElement> sub_childrens = p.getChildren();
            		for (IErlElement child : sub_childrens) {
            			String label = StringUtils.normalizeSpaces(child.toString());
            			System.out.println(label);	
					}
            	}
			}           
            
			final IDocument document = editor.getDocument();
			int lines = document.getNumberOfLines();
			if (line > 0 && line < lines) {
				int lineStart = document.getLineOffset(line);
				int lineLength = document.getLineLength(line);
				EditorUtility.revealInEditor(editorPart, lineStart, lineLength);
			}
			else {
				String name = erlFile.getName();
				String modelName = name.replace(".erl", "");
				String text = modelName;
				FindReplaceDocumentAdapter dad = new FindReplaceDocumentAdapter(document);
				IRegion region = dad.find(0, text, true, false, false, false);
				EditorUtility.revealInEditor(editorPart, region);
			}
        } catch (PartInitException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
		
	public Integer getTotal() {
		return total;
	}
	
	public boolean isQuestionPanelVisible() {
		return sectionQuestion.isVisible();
	}
	
	public boolean stopServer() throws Exception {
		clearAndResetDebugger();
		helper.stopEDDebugger();
		
		return true;
	}
	
	/**
	 * Starts the debugging. 
	 */
	public boolean startServer() {
		cleanQuestionSection();
		
		try {
			// FIXME Erlang Server
			String folder = debugFile.getParent().getLocation().toPortableString();
			StringBuilder sb = new StringBuilder();
			sb.append(folder);
			if (!folder.endsWith("/")) {
				sb.append("/");
			}
			String buggyCall = buggyCallText.getText();
			String location = sb.toString();
			assert (buggyCall != null && location != null) : "Los parámetros de entrada no pueden ser nulos";
			boolean wasOK = helper.startEDDebugger(buggyCall, location);
			if (wasOK) {
				index = 0;
				total = helper.getQuestionsSize();
				createPlainTabs(contentQuestion, total);
				sectionDebugger.setExpanded(false);
				sectionQuestion.setVisible(true);
				sectionQuestion.setExpanded(true);
				
				int goToIndex = helper.getCurrentQuestion();
				updateSelection(goToIndex);
				
				writeJsonFile();
				writeDiagramFiles();
//				writeEmf2Graphviz();
				
				return true;
			}
			else {
				MessageDialog.openError(getSite().getShell(), "EDD - Error", "Ha ocurrido un error interno al intentar cargar el servidor de depuración...");
				Activator.logUnexpected("Revisar consola de servidor...", new EDDException());
			}
		} catch (EDDException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private void writeJsonFile() throws EDDException, CoreException {
		IProject iProject = debugFile.getProject();
		IFolder eddFolder = iProject.getFolder("edd");
		String jsonFile = debugFile.getName().replace(".erl", ".json");
		IPath jsonPath = new Path(eddFolder.getFullPath() + File.separator + jsonFile);
//		FIXME helper.writeJsonDocument(jsonPath);
	}
	
	private void writeDotFile(String dotContent) throws EDDException, CoreException {
		IProject iProject = debugFile.getProject();
		IFolder eddFolder = iProject.getFolder("edd");
		String dotFile = debugFile.getName().replace(".erl", ".dot");
		IPath dotPath = new Path(eddFolder.getFullPath() + File.separator + dotFile);
		helper.writeFile(dotContent , dotPath);
	}
	
	private void writeDiagramFiles() throws EDDException {
		IProject iProject = debugFile.getProject();
		IFolder eddFolder = iProject.getFolder("edd");
		String name = debugFile.getName();
		String modelName = name.replace(".erl", "");
		String ed2File = name.replace(".erl", ".ed2");
		IPath ed2Path = new Path(eddFolder.getFullPath() + File.separator + ed2File);
		String ed2DiagramFile = name.replace(".erl", ".ed2_diagram");
		IPath ed2DiagramPath = new Path(eddFolder.getFullPath() + File.separator + ed2DiagramFile);
		
		IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(EDDTreeView.ID);
		if (part instanceof EDDTreeView) {
			EDDTreeView view = (EDDTreeView) part;
//			System.out.println(modelName);
			Model model = helper.buildEMF(modelName, ed2Path, ed2DiagramPath);
			view.updateContent(model);
		}
	}
	
	private void writeEmf2Graphviz() {
		IProject iProject = debugFile.getProject();
		IFolder eddFolder = iProject.getFolder("edd");
		String name = debugFile.getName();
		String model = debugFile.getLocation().toPortableString();
		String workDirectory = eddFolder.getLocation().toPortableString();
		String graphFilename = name.replace(".erl", ".jpg");
		
		StandaloneApp app = new StandaloneApp(model, workDirectory, graphFilename);
		app.execute();
		
//		EMF2GvProcessorShortcut launchShortcut = new EMF2GvProcessorShortcut();
//		launchShortcut.launch("Pass the Java Project containing JUnits Classes", "run");
	}

	public void clearAndResetDebugger() {
		cleanQuestionSection();
		index = 0;
		total = 0;
		sectionDebugger.setExpanded(true);
		sectionQuestion.setVisible(false);
		sectionQuestion.setExpanded(false);
		setDebugFile(null);
		buggyCallText.setText("");
	}
	
	private void cleanQuestionSection() {
		// Clean previous content
		Control[] children = contentQuestion.getChildren();
		for (Control control : children) {
			control.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		scrolledForm.setFocus();
	}
	
	public void refresh() {
		scrolledForm.redraw();
	}

	/**
	 * Disposes the toolkit
	 */
	public void dispose() {
		// important: We need do unregister our listener when the view is disposed
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(listener);
		toolkit.dispose();
		super.dispose();
	}
}