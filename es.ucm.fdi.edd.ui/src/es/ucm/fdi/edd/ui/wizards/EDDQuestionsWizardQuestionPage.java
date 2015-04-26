package es.ucm.fdi.edd.ui.wizards;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class EDDQuestionsWizardQuestionPage extends WizardPage {
	
	private static final Map<String, String> myMap;
    static {
        myMap = new LinkedHashMap<String, String>();
        myMap.put("y", "Yes");
        myMap.put("n", "No");
        myMap.put("t", "Trusted");
        myMap.put("d", "Don't know");
        myMap.put("i", "Inadmissible");
//        myMap.put("u", "Undo ");
//        myMap.put("a", "Abort");
    }

	private ISelection selection;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public EDDQuestionsWizardQuestionPage(ISelection selection) {
		super("wizardPage");
		setTitle("EDD Questions Wizard");
		setDescription("The debugger asks for a list of trusted functions, i.e., functions that the programmer is sure are correct, so that 'edd' will never ask about them.");
		this.selection = selection;
	}

	/**
	 * @param selection
	 * @param i
	 */
	public EDDQuestionsWizardQuestionPage(ISelection selection, int i) {
		super("wizardPage_"+i);
		setTitle("EDD Questions Wizard");
		setDescription("Wizard page " + i);
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 9;
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(container, SWT.NULL);
		label.setText("Question:");

		Text fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fileText.setLayoutData(gd);
		
	    createRadioButtonsSection(container);
	    createComponentsButton(container);
		
		initialize();
		dialogChanged();
		setControl(container);
	}

	private void createRadioButtonsSection(Composite parent) {
		for(Entry<String, String> entry : myMap.entrySet()) {
		    String key = entry.getKey();
		    String value = entry.getValue();
			
		    Button radioButton = new Button(parent, SWT.RADIO);
		    radioButton.setText(value);
		    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		    gd.horizontalSpan = 2;
		    radioButton.setLayoutData(gd);
		}
	}
	
	protected void createComponentsButton(Composite parent) {
		Composite compButtons = new Composite(parent, SWT.NONE);
		compButtons.setLayout(new GridLayout(5, false));
		compButtons.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_END));

		Button button1 = new Button(compButtons, SWT.PUSH);
		button1.setText("Yes");
		button1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateStatusPage();
			}
		});

		Button button2 = new Button(compButtons, SWT.PUSH);
		button2.setText("No");
		button2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateStatusPage();
			}
		});

		Button button3 = new Button(compButtons, SWT.PUSH);
		button3.setText("Trusted");
		button3.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateStatusPage();
			}
		});
		
		Button button4 = new Button(compButtons, SWT.PUSH);
		button4.setText("Don't know");
		button4.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateStatusPage();
			}
		});

		Button button5 = new Button(compButtons, SWT.PUSH);
		button5.setText("Inadmissible");
		button5.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateStatusPage();
			}
		});
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	private void initialize() {
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */
	private void dialogChanged() {
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	/**
	 * Valida el estado de la página, si todo esta OK
	 */
	protected void updateStatusPage() {
		setPageComplete(false);
		if (false) {
			setErrorMessage(null);
			setMessage("", WARNING);
			return;
		} else if (true) {
			setErrorMessage(null);
			setMessage(getDescription(), INFORMATION);
			setPageComplete(true);
			return;
		}

		setErrorMessage(null);
		setMessage(null);
		setPageComplete(true);
	}
	
	private boolean validatePage() {
		return false;
	}
}