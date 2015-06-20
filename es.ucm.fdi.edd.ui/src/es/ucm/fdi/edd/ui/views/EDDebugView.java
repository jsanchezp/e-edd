package es.ucm.fdi.edd.ui.views;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
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
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.ViewPart;

import es.ucm.fdi.edd.ui.Activator;
import es.ucm.fdi.edd.ui.Messages;
import es.ucm.fdi.edd.ui.views.utils.EDDHelper;

public class EDDebugView extends ViewPart {

	public final static String VIEW_ID = "es.ucm.fdi.edd.ui.views.EDDebugView";
	
	private Section sectionDebugger;
	private Section sectionQuestion;
	private Composite contentQuestion;

	private FormToolkit toolkit;
	private ScrolledForm scrolledForm;
	private ScrolledPageBook pageBook;
	
	/** Remember the selected index. */
	private Integer index = 0;
	private Integer total = 0;
	
	private EDDHelper helper; 

	/**
	 * The constructor
	 */
	public EDDebugView() {
		helper = new EDDHelper(new File("C:\\Test2.json"));
	}

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
		
		final IMessageManager mmng = scrolledForm.getMessageManager();
		mmng.setAutoUpdate(true);
		Text text = createDecoratedTextField(Messages.getString("EDDebugView.textLabel"), contentDebugger, mmng);
		text.addKeyListener(new KeyListener() {			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR) {
					// FIXME Revisar... 
					// Clean previous content
					Control[] children = contentQuestion.getChildren();
					for (Control control : children) {
						control.dispose();
					}
					helper.readJson();
					index = 0;
					total = helper.getQuestionsSize();
					
					createPlainTabs(contentQuestion, total);
					sectionDebugger.setExpanded(false);
					sectionQuestion.setVisible(true);
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				// Empty
			}
		});

		sectionQuestion = createSection(body, Messages.getString("EDDebugView.sectionQuestion.title"), Messages.getString("EDDebugView.sectionQuestion.desc"), false);
		contentQuestion = createSectionClient(sectionQuestion);
		sectionQuestion.setVisible(false);
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
		sectionClient.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_CYAN));		
		
		section.setClient(sectionClient);
		section.pack();
		return sectionClient;
	}
	
	/**
	 * @param label
	 * @param parent
	 * @param mmng
	 */
	private Text createDecoratedTextField(String label, Composite parent, final IMessageManager mmng) {
		toolkit.createLabel(parent, label);
		final Text text = toolkit.createText(parent, "");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 150;
		text.setLayoutData(gd);
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
	 */
	public void createPlainTabs(Composite parent, int tabs) {
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
			pageContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));	        
	    }
	 
	    // Force to display the first tab
	    pageBook.showPage(0);
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
		content.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA));
		
		toolkit.createLabel(content, "", SWT.NONE);
		Text text = toolkit.createText(content, "", SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.setText(message);

	    Composite radioContainer = toolkit.createComposite(content);
	    radioContainer.setLayout(new GridLayout(5, false));
	    radioContainer.setLayoutData(new GridData(SWT.END, SWT.DEFAULT, true, false));
	    radioContainer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
	    
		toolkit.createButton(radioContainer, Messages.getString("EDDebugView.radio1"), SWT.RADIO);
		toolkit.createButton(radioContainer, Messages.getString("EDDebugView.radio2"), SWT.RADIO);
		toolkit.createButton(radioContainer, Messages.getString("EDDebugView.radio3"), SWT.RADIO);
		toolkit.createButton(radioContainer, Messages.getString("EDDebugView.radio4"), SWT.RADIO);
		toolkit.createButton(radioContainer, Messages.getString("EDDebugView.radio5"), SWT.RADIO);
		
//		Composite buttonsComposite = toolkit.createComposite(content);
////	    buttonsComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
//		buttonsComposite.setLayout(new GridLayout(4, false));
//	    buttonsComposite.setLayoutData(new GridData(SWT.END, SWT.DEFAULT, true, false));
//		toolkit.createButton(buttonsComposite, Messages.getString("FormView.button1"), SWT.PUSH);
//		toolkit.createButton(buttonsComposite, Messages.getString("FormView.button2"), SWT.PUSH);
//		toolkit.createButton(buttonsComposite, Messages.getString("FormView.button3"), SWT.PUSH);
//		toolkit.createButton(buttonsComposite, Messages.getString("FormView.button4"), SWT.PUSH);
		
		return content;
	}
	
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
		pageBook.showPage(index);
		sectionQuestion.setText(Messages.getString("EDDebugView.sectionQuestion.title") + " " + index + "/" + total );
	}
	
	public Integer getTotal() {
		return total;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		scrolledForm.setFocus();
	}

	/**
	 * Disposes the toolkit
	 */
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}
}