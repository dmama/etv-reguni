package ch.vd.uniregctb.admin;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.audit.AuditLine;
import ch.vd.uniregctb.audit.AuditLineDAO;
import ch.vd.uniregctb.common.WebTest;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class AuditLogControllerTest extends WebTest {

	// private static final Logger LOGGER = Logger.getLogger(AuditLogControllerTest.class);

	private final static String DB_UNIT_FILE = "AuditLogControllerTest.xml";

	private AuditLineDAO auditLineDAO;

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "auditLogController";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		auditLineDAO = getBean(AuditLineDAO.class, "auditLineDAO");
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		List<AuditLine> listLines = auditLineDAO.getAll();
		int nbLines = listLines.size();

		AuditLogController controller = getBean(AuditLogController.class, CONTROLLER_NAME);
		request.setMethod("GET");

		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);

		AuditLogBean bean = (AuditLogBean) model.get(controller.getCommandName());
		assertNotNull(bean);
		assertNotNull(bean.getList());
		assertEquals(nbLines, bean.getList().size());
	}

}
