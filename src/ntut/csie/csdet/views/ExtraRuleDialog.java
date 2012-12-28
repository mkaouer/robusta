package ntut.csie.csdet.views;

import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;
import ntut.csie.rleht.common.ImageManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * ��user�]�w�n�����ժ�Dummy Handler
 * @author Shiau
 *
 */
public class ExtraRuleDialog extends Dialog{
	//���Rule��T��Table
	private Table displayTable;
	//Dialog�W�誺Text
	private Text tempText;
	//�ק�Item�����s
	private Button editBtn;
	//�s��Library��Statement��Rule���
	private TreeMap<String, Boolean> ruleMap;
	//����m�Ҧ�Button��Composite
	private Composite btnComposite;

	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	/**
	 * Create the dialog
	 * @param parent
	 * @param libMap
	 */
	public ExtraRuleDialog(Shell parent,TreeMap<String, Boolean> libMap){
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.ruleMap = libMap;
	}

	/**
	 * Create contents of the dialog
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		//�Y���ܵ����j�p
		container.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent e) {
				//Resize
				//243��tempText�w�]�e�סA342��container�w�]�e��
				tempText.setSize(243 + container.getSize().x - 342, tempText.getSize().y);
				//243��displayTable�w�]�e�סA342��container�w�]�e�סF150��displayTable�w�]���סA247��container�w�]����
				displayTable.setSize(243 + container.getSize().x - 342, 150 + container.getSize().y - 247);
				//10��displayTable�w�]��m�A6��displayTable�MbtnComposite�������ťժ���
				btnComposite.setLocation(10 + tempText.getSize().x + 6, btnComposite.getLocation().y);
			}
		});
		container.setLayout(null);
		
		//���Table
		displayTable = new Table(container, SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI | SWT.BORDER);
		displayTable.setFont(new Font(this.getShell().getDisplay(),"Arial", 11,SWT.NONE));
		final GridData gd_testList = new GridData(SWT.FILL, SWT.FILL, true, true);
		displayTable.setBounds(10, 66, 243, 150);
		displayTable.setLayoutData(gd_testList);
		//�p�G��ܪ�displayTable��Item���W����ܦbText�W
		displayTable.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event e){
				int selectionIndex = displayTable.getSelectionIndex();
				//����@�}Dialog�N���Ŀ�checkbox,�X�{index=-1�����p
				if(selectionIndex >= 0){
					//���ܪ�Item��Library�W����ܦbText�W
					editBtn.setEnabled(true);
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});
		//�Y�bdisplayTable��Item�W�I���U�A���X�ק����
		displayTable.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					String temp = displayTable.getItem(selectionIndex).getText();
					//�I�s�ק�Dialog
					EditRuleDialog dialog = new EditRuleDialog(new Shell(),temp,displayTable);
					dialog.open();
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});

		//��mButton��composite
		btnComposite = new Composite(container, SWT.NONE);

		//ĵ�i�ϥܩM��r
		final Label picLabel = new Label(container, SWT.NONE);
		picLabel.setBounds(10, 222, 16, 15);
		picLabel.setVisible(false);
		picLabel.setImage(ImageManager.getInstance().get("warning"));
		final Label warningLabel = new Label(container, SWT.NONE);
		warningLabel.setText(resource.getString("lib.exist"));
		warningLabel.setVisible(false);
		warningLabel.setBounds(32, 222, 85, 12);

		//�ϥγ���JText
		tempText = new Text(container, SWT.BORDER);
		tempText.setFont(new Font(parent.getDisplay(), "Courier New",12,SWT.NORMAL));
		tempText.setBounds(10, 38, 243, 22);
		tempText.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				//�YText�@���ܴN��ĵ�i�T������
				picLabel.setVisible(false);
				warningLabel.setVisible(false);
			}
		});
		
		Label nameLabel = new Label(container, SWT.NONE);
		nameLabel.setBounds(10, 10, 97, 22);
		//�̬O�_��library��statement�ӧ��ܤ��P���d��
		nameLabel.setText(resource.getString("detect.rule"));

		//���������s
		final Button clearBtn = new Button(btnComposite, SWT.NONE);
		clearBtn.setBounds(0, 112, 68, 22);
		clearBtn.setText(resource.getString("deselect.all"));
		clearBtn.pack();
		clearBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//����������Item
				for (int i=0;i<displayTable.getItemCount();i++) {
					TableItem item = displayTable.getItem(i);
					item.setChecked(false);
				}
			}
		});
		int maxButtonWidth = clearBtn.getBounds().width;

		//������s
		final Button selectBtn = new Button(btnComposite, SWT.NONE);
		selectBtn.setBounds(0, 84, maxButtonWidth, 22);
		selectBtn.setText(resource.getString("select.all"));
		selectBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//��ܥ�����Item
				for (int i=0;i<displayTable.getItemCount();i++) {
					TableItem item = displayTable.getItem(i);
					item.setChecked(true);
				}
			}
		});
		
		//�s�W���s
		Button addBtn = new Button(btnComposite, SWT.NONE);
		addBtn.setBounds(0, 0, maxButtonWidth, 22);
		addBtn.setText(resource.getString("add"));
		addBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				boolean isWarning = addRule();
				//�Y���ƴN���ĵ�i�T��
				if (isWarning){
					picLabel.setVisible(true);
					warningLabel.setVisible(true);
				}
			}
		});

		//�R�����s
		final Button removeButton = new Button(btnComposite, SWT.NONE);
		removeButton.setBounds(0, 28, maxButtonWidth, 22);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e)
			{
				//Table�����Ū��B�����Library �N���ܪ�Library���R��
				if (displayTable.getItemCount() != 0 && displayTable.getSelectionIndex()!=-1) {
					displayTable.remove(displayTable.getSelectionIndices());
					//�R���ɧ�Text�M��
					tempText.setText("");
				}
			}
		});
		removeButton.setText(resource.getString("remove"));

		//�ק諸���s
		editBtn = new Button(btnComposite, SWT.NONE);
		editBtn.setBounds(0, 56, maxButtonWidth, 22);
		editBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int selectionIndex = displayTable.getSelectionIndex();
				if (selectionIndex >= 0) {
					String temp = displayTable.getItem(selectionIndex).getText();
					//�I�s�ק�Dialog
					EditRuleDialog dialog = new EditRuleDialog(new Shell(),temp,displayTable);
					dialog.open();
					tempText.setText(displayTable.getItem(selectionIndex).getText());
				}
			}
		});
		editBtn.setText(resource.getString("edit"));
		editBtn.setEnabled(false);

		//��������
		final Button explainBtn = new Button(btnComposite, SWT.NONE);
		explainBtn.setBounds(0, 140, maxButtonWidth, 22);
		explainBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//���X������Dialog
				MessageDialog.openInformation(
						new Shell(),
						resource.getString("caption"),
						resource.getString("help.description"));
			}
		});
		explainBtn.setText(resource.getString("help"));
		explainBtn.setImage(ImageManager.getInstance().get("help"));
		
		btnComposite.setBounds(259, 38, maxButtonWidth, 199);

		//�N�өҦ�������Library�����ܦbList
		setInput();

		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
	}
		
	@Override
	protected void okPressed() {
		//�W�[Rule
		addRule();
		//�M�����쥻��Rule
		ruleMap.clear();
		//���N�C����item���X��
		TableItem[] temp = displayTable.getItems();
		//�htraverse���table��item��Text�M�O�_�Q�Ŀ��
		for(int i=0;i<temp.length;i++){
			ruleMap.put(temp[i].getText(),temp[i].getChecked());
		}
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(350, 325);
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		//���Dialog���D
		newShell.setText(resource.getString("extra.rules.dialog.title"));
	}

	/**
	 * �W�[Rule���
	 */
	private boolean addRule() {
		boolean isWarning = false;
		//�R��Text�e��Ů泡��
		String temp = tempText.getText().trim();

		if (tempText.getText().length() != 0) {			
			//�Y�S��"."���ܬ�Method�A�ۦ����ϥΪ̥["*."
			if (!temp.contains("."))
				temp = "*." + temp;

			boolean isExist = false;
			//��Library��Name���S������
			for(int i=0;i<displayTable.getItemCount();i++) {
				if(temp.equals(displayTable.getItem(i).getText()))
					isExist = true;
			}
			//�S�����ƴN�[�J�s��Library
			if (!isExist) {
				TableItem item = new TableItem(displayTable,SWT.NONE);
				item.setText(temp);
				item.setChecked(true);
				tempText.setText("");
			}else {
				tempText.setText(temp);
				isWarning = true;
			}
		}
		return isWarning;
	}
	
	/**
	 * ���o�]�w������Library���
	 */
	public TreeMap<String, Boolean> getLibMap() {
		return ruleMap;
	}
	
	/**
	 * �N�Ҧ������ר���ܦbtable��
	 */
	private void setInput() {
		Iterator<String> libIt = ruleMap.keySet().iterator();
		while(libIt.hasNext()){
			String temp = libIt.next();
			TableItem item = new TableItem(displayTable,SWT.NONE);
			item.setText(temp);
			item.setChecked(ruleMap.get(temp));
		}
	}
}