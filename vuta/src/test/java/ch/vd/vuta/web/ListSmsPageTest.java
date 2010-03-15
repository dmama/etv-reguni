package ch.vd.vuta.web;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.vd.vuta.AbstractSmsgwTestCase;

public class ListSmsPageTest extends AbstractSmsgwTestCase {

	public void testPage() throws Exception {
		
		MockHttpServletRequest req = new MockHttpServletRequest();
		MockHttpServletResponse resp = new MockHttpServletResponse();

		ListSmsPage page = new ListSmsPage(applicationContext);
		page.processPage(req, resp);
		
		String actual = resp.getContentAsString();
		assertTrue(actual.contains("Date de reception"));
	}

}
