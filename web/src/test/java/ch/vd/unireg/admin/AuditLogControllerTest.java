package ch.vd.unireg.admin;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.audit.AuditLine;
import ch.vd.unireg.audit.AuditLineDAO;
import ch.vd.unireg.common.WebTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test case du controlleur spring du meme nom.
 */
public class AuditLogControllerTest extends WebTest {

	// private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogControllerTest.class);

	private static final String DB_UNIT_FILE = "AuditLogControllerTest.xml";

	private AuditLineDAO auditLineDAO;

	/**
	 * Le nom du controller a tester.
	 */
	private static final String CONTROLLER_NAME = "auditLogController";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		auditLineDAO = getBean(AuditLineDAO.class, "auditLineDAO");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {

		loadDatabase(DB_UNIT_FILE);

		final List<AuditLine> listLines = auditLineDAO.getAll();
		int nbLines = listLines.size();

		AuditLogController controller = getBean(AuditLogController.class, CONTROLLER_NAME);

		final MockMvc m = MockMvcBuilders.standaloneSetup(controller).build();

		final ResultActions res = m.perform(get("/admin/audit.do"));
		res.andExpect(status().isOk());

		final MvcResult result = res.andReturn();
		assertNotNull(result);
		final Map<String, Object> model = result.getModelAndView().getModel();
		assertNotNull(model);

		AuditLogBean bean = (AuditLogBean) model.get("command");
		assertNotNull(bean);
		assertNotNull(bean.getList());
		assertEquals(nbLines, bean.getList().size());
	}

}
