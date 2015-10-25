package marubinotto.piggydb.ui.page;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;

import marubinotto.piggydb.impl.PigDump;
import marubinotto.piggydb.impl.db.DatabaseSchema;
import marubinotto.piggydb.impl.db.SequenceAdjusterList;
import marubinotto.piggydb.model.enums.Role;
import marubinotto.piggydb.ui.page.common.AbstractBorderPage;
import marubinotto.piggydb.ui.page.common.DatabaseSpecificBeans;
import marubinotto.util.RdbUtils;
import marubinotto.util.procedure.Procedure;
import marubinotto.util.web.WebUtils;
import net.sf.click.control.FileField;
import net.sf.click.control.Form;
import net.sf.click.control.Submit;

import org.apache.commons.fileupload.FileItem;
import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;

public class RestorePage extends AbstractBorderPage {

	private DatabaseSpecificBeans dbSpecificBeans;
	private SequenceAdjusterList sequenceAdjusterList;

	public SequenceAdjusterList getSequenceAdjusterList() {
		return this.sequenceAdjusterList;
	}

	public void setSequenceAdjusterList(SequenceAdjusterList sequenceAdjusterList) {
		this.sequenceAdjusterList = sequenceAdjusterList;
	}

	@Override
	protected String[] getAuthorizedRoles() {
		return new String[]{Role.OWNER.getName()};
	}

	//
	// Control
	//

	public Form restoreForm = new Form();
	public Form importForm = new Form();
	private FileField dataFileField = new FileField("dataFile", true);

	@Override
	public void onInit() {
		super.onInit();
		initControls();
		this.dbSpecificBeans = new DatabaseSpecificBeans(getApplicationContext());
	}

	private void initControls() {
		this.dataFileField.setLabel(getMessage("RestorePage-data-file") + " (*.xml/*.pig)");
		this.dataFileField.setSize(50);
		this.restoreForm.add(this.dataFileField);
		this.restoreForm.add(new Submit("restore", getMessage("RestorePage-restore"), this, "onRestoreClick"));
		this.importForm.add(this.dataFileField);
		this.importForm.add(new Submit("import", getMessage("RestorePage-import"), this, "onImportClick"));
	}

	public boolean onRestoreClick() throws Exception {
		if (!this.restoreForm.isValid()) {
			return true;
		}

		try {
			doRestore();
		}
		catch (DataSetException e) { // XML error
			this.dataFileField.setError(getMessage("RestorePage-invalid-file"));
			return true;
		}
		catch (InvalidPigDumpException e) {
			this.dataFileField.setError(getMessage("RestorePage-invalid-file"));
			return true;
		}

		setRedirectWithMessage(HomePage.class, getMessage("RestorePage-database-restored"));
		return false;
	}
	
	//TODO
	public boolean onImportClick() throws Exception {
	  /*if (!this.importForm.isValid()) {
	    return true;
	  }
	  
	  try {
	    doImport();
	  }
	  catch (DataSetException e) { // XML error
      this.dataFileField.setError(getMessage("RestorePage-invalid-file"));
      return true;
    }
	  */
	  setRedirectWithMessage(HomePage.class, getMessage("RestorePage-fragment-imported"));
	  return false;
	}
	/**
	 * The database version (global_setting/database.version) won't be changed
	 * since the version belongs to the current schema, not the data.
	 */
	private void doRestore() throws Exception {
		final DatabaseSchema schema = this.dbSpecificBeans.getDatabaseSchema();
		final FileItem fileItem = this.dataFileField.getFileItem();

		getDomain().getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				int currentVersion = schema.getVersion();
				getLogger().info("currentVersion : " + currentVersion);

				cleanTables();
				if (fileItem.getName().toLowerCase().endsWith(".xml")) {
					restoreWithXml();
				}
				else {
					File uploadedFile = WebUtils.forceGetFile(fileItem);
					getLogger().debug("uploadedFile: " + uploadedFile.getAbsolutePath());
					if (!getPigDump().checkDumpFile(uploadedFile)) {
						throw new InvalidPigDumpException();
					}
					getPigDump().restore(uploadedFile);
				}

				schema.setVersion(currentVersion); // preserve the version
				getSequenceAdjusterList().adjust();
				return null;
			}
		});
	}
	
	//TODO
	/*private void doImport() throws Exception {
	  final DatabaseSchema schema = this.dbSpecificBeans.getDatabaseSchema();
	  final FileItem fileItem = this.dataFileField.getFileItem();
	  
	  getDomain().getTransaction().execute(new Procedure() {
      public Object execute(Object input) throws Exception {
        int currentVersion = schema.getVersion();
        getLogger().info("currentVersion : " + currentVersion);
        if(fileItem.getName().toLowerCase().endsWith(".xml"))
          importXml();
        schema.setVersion(currentVersion++);
        getSequenceAdjusterList().adjust();
        return null;
      }
    });
	}*/

	private void cleanTables() throws DatabaseUnitException, SQLException {
		getLogger().info("Cleaning all tables ...");
		for (String tableName : PigDump.TABLES) {
			RdbUtils.deleteAll(this.dbSpecificBeans.getJdbcConnection(), tableName);
		}
	}

	private void restoreWithXml() throws Exception {
		getLogger().info("Restoring with xml ...");
		InputStream dataInput = null;
		try {
			dataInput = this.dataFileField.getFileItem().getInputStream();
			RdbUtils.cleanImportXml(this.dbSpecificBeans.getJdbcConnection(), dataInput);
		}
		finally {
			dataInput.close();
		}
		getDomain().getFileRepository().clear();
	}

	private PigDump getPigDump() {
		return (PigDump) getBean("pigDump");
	}

	private static class InvalidPigDumpException extends Exception {
	}
	
	//TODO
	/*private void importXml() 
	  throws IOException, SQLException, DatabaseUnitException {
	  
	  getLogger().info("Importing the fragment from xml ...");
	  InputStream dataInput = null;
	  dataInput = this.dataFileField.getFileItem().getInputStream();
	  RdbUtils.importXml(this.dbSpecificBeans.getJdbcConnection(), dataInput);
	  dataInput.close();
	}*/
}
