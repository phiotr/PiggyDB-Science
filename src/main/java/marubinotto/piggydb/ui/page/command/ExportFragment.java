package marubinotto.piggydb.ui.page.command;

import javax.servlet.http.HttpServletResponse;

import marubinotto.piggydb.model.enums.Role;
import marubinotto.piggydb.ui.page.common.DatabaseSpecificBeans;
import marubinotto.util.RdbUtils;

public class ExportFragment extends AbstractCommand {

  @Override
  protected String[] getAuthorizedRoles() {
    return new String[]{Role.OWNER.getName()};
  }

  @Override
  protected void execute() throws Exception {
    HttpServletResponse response = getContext().getResponse();
    response.setContentType("text/xml");
    Integer rowId = Integer.parseInt(getContext().getRequestParameter("id"));
    RdbUtils.exportFragmentAsXml(new DatabaseSpecificBeans(getApplicationContext()).getJdbcConnection(), 
      response.getOutputStream(), rowId);
    
    response.flushBuffer();
  }

}
