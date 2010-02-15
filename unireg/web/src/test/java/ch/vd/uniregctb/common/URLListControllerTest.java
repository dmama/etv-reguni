package ch.vd.uniregctb.common;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

/**
 * Test case du controlleur spring du m�me nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class URLListControllerTest extends WebTest {

	/**
	 * Le nom du controller � tester.
	 */
	private final static String CONTROLLER_NAME = "URLListController";

	/**
	 * @throws Exception
	 */
	@Test
	public void testShowForm() throws Exception {
		URLListController controller = getBean(URLListController.class, CONTROLLER_NAME);
		request.setMethod("GET");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assert (model != null);
		List<?> list = (List<?>) model.get(URLListController.URL_LIST_ATTRIBUTE_NAME);
		assert (list != null);
		assert (!list.isEmpty());
		assert (list.get(0) instanceof URLBean);
	}
}
