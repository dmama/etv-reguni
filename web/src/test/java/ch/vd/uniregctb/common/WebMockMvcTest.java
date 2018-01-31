package ch.vd.uniregctb.common;

import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public abstract class WebMockMvcTest extends WebTest {

	private MockMvc mvc;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		mvc = MockMvcBuilders.standaloneSetup(getControllers()).build();
	}

	protected abstract Object[] getControllers();

	protected ResultActions get(String uri, @Nullable Map<String, String> params, @Nullable MockHttpSession session) throws Exception {
		final MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.get(uri);
		return performHttp(params, reqBuilder, session).andExpect(MockMvcResultMatchers.status().isOk());
	}

	protected ResultActions post(String uri, @Nullable Map<String, String> params, @Nullable MockHttpSession session) throws Exception {
		final MockHttpServletRequestBuilder reqBuilder = MockMvcRequestBuilders.post(uri);
		return performHttp(params, reqBuilder, session);
	}

	private ResultActions performHttp(Map<String, String> params, MockHttpServletRequestBuilder reqBuilder, @Nullable MockHttpSession session) throws Exception {
		if (params != null) {
			for (Map.Entry<String, String> param : params.entrySet()) {
				reqBuilder.param(param.getKey(), param.getValue());
			}
		}
		if (session != null) {
			reqBuilder.session(session);
		}
		return mvc.perform(reqBuilder);
	}
}
