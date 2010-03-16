/**
 * 
 */
package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;

/**
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 * 
 */
public class URLListController extends AbstractSimpleFormController {

	private static final String SIMPLE_URL_MAPPING_BEAN_NAME = "simpleUrlMapping";

	public static final String URL_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map controlModel)
		throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, controlModel);
		ApplicationContext context = getApplicationContext();
		SimpleUrlHandlerMapping urlMapping = (SimpleUrlHandlerMapping) context.getBean(SIMPLE_URL_MAPPING_BEAN_NAME);

		/*
		 * Copie la liste des urls mapp�es, leur controlleur et l'URL d'appel
		 * dans une liste de URLBean .
		 */
		List<URLBean> urls = new ArrayList<URLBean>();
		Map model = mav.getModel();
		Map urlMap = urlMapping.getUrlMap();
		Iterator<String> iterator = urlMap.keySet().iterator();
		while (iterator.hasNext()) {
			URLBean bean = new URLBean();
			String mappedUrl = (String) iterator.next();
			bean.setMappedUrl(mappedUrl);
			bean.setController((Controller) urlMap.get(mappedUrl));
			bean.setUrl("<a href=\"" + mappedUrl.substring(1) + "\">" + mappedUrl.substring(1) + "</a>");
			urls.add(bean);
		}

		model.put(URL_LIST_ATTRIBUTE_NAME, urls);
		return mav;
	}
}
