package es.ucm.fdi.edd.ui.views;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.ViewPart;

public class EDDebugView extends ViewPart {

	public final static String VIEW_ID = "es.ucm.fdi.edd.ui.views.EDDebugView";

	private FormToolkit toolkit;
	private ScrolledForm form;

	/**
	 * The constructor
	 */
	public EDDebugView() {
		//
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 2;
		TableWrapData td1 = new TableWrapData(TableWrapData.FILL_GRAB);
		td1.colspan = 2;
		TableWrapData td2 = new TableWrapData(TableWrapData.FILL_GRAB);
		td2.colspan = 2;
		
		ToolBarManager manager = (ToolBarManager) form.getToolBarManager();
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		toolkit.decorateFormHeading(form.getForm());
		IMenuService menuService = (IMenuService) getSite().getService(IMenuService.class);
		menuService.populateContributionManager(manager, "toolbar:formsEDDebugToolBar");
//		menuService.populateContributionManager(manager, "formheading:" + getSite().getId());
		manager.update(true);
		
		form.setText(Messages.getString("FormView.title")); 
		Composite body = form.getBody();
		body.setLayout(layout);
		
		Label label = toolkit.createLabel(body, Messages.getString("FormView.textLabel"));
		label.setLayoutData(td1);
		Text text = toolkit.createText(body, "");
		text.setLayoutData(td2);
		
		Section section = toolkit.createSection(body, Section.DESCRIPTION | Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
		section.setText(Messages.getString("FormView.stitle")); 
		section.setDescription(Messages.getString("FormView.sdesc"));
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				form.reflow(true);
			}
		});
		section.setLayoutData(td);
		
		
		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		toolkit.createButton(sectionClient, Messages.getString("FormView.radio1"), SWT.RADIO); 
		toolkit.createButton(sectionClient, Messages.getString("FormView.radio2"), SWT.RADIO);
		toolkit.createButton(sectionClient, Messages.getString("FormView.radio3"), SWT.RADIO); 
		toolkit.createButton(sectionClient, Messages.getString("FormView.radio4"), SWT.RADIO); 
		toolkit.createButton(sectionClient, Messages.getString("FormView.radio5"), SWT.RADIO); 
		
		Composite composite = new Composite(sectionClient, SWT.NULL);
	    composite.setLayout(new RowLayout());
		toolkit.createButton(composite, Messages.getString("FormView.button1"), SWT.PUSH);
		toolkit.createButton(composite, Messages.getString("FormView.button2"), SWT.PUSH);
		toolkit.createButton(composite, Messages.getString("FormView.button3"), SWT.PUSH);
		toolkit.createButton(composite, Messages.getString("FormView.button4"), SWT.PUSH);
				
		section.setClient(sectionClient);
				
		toolkit.paintBordersFor(body);
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	/**
	 * Disposes the toolkit
	 */
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}
}
