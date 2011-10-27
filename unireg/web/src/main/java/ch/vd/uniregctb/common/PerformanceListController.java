/**
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
package ch.vd.uniregctb.common;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import ch.vd.uniregctb.performance.PerformanceLog;
import ch.vd.uniregctb.performance.PerformanceLogsRepository;

/**
 * Retourne vers la vue sp�cifi� dans ParameterizableViewController.viewName et
 * ajoute dans le model les valeurs de requestURI et de labelCode. Ce controller
 * permet de n'utiliser qu'une seule vue (JSP) pour l'affichage des
 * performances, en sp�cifiant l'URL de request utilis�e par displayTag et le
 * code de libell� utilis� pour afficher le titre dans la page.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 *
 */
public class PerformanceListController extends ParameterizableViewController {

	private static final Logger LOGGER = Logger.getLogger(PerformanceListController.class);

	public static final String PERFORMANCE_LOGS_KEY = "performanceLogs";

	/**
	 * URI de r�f�rence utilis�e dans displaytag pour la navigation dans les
	 * pages.
	 */
	private String requestURI;

	/**
	 * la couche dont il faut afficher les performances
	 */
	private String layer;

	/**
	 * @see org.springframework.web.servlet.mvc.ParameterizableViewController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mav = super.handleRequestInternal(request, response);

		layer = request.getParameter("layer");
		if (layer == null || layer.equals("")) {
			layer = "all"; // Default
		}

		PerformanceLogsRepository repo = PerformanceLogsRepository.getInstance();

		Map<String, PerformanceLog> logs;
		if (layer.equals("all")) {

			logs = new HashMap<String, PerformanceLog>();

			Map<String, Map<String, PerformanceLog>> layers = repo.getLayers();
			for (String uri : layers.keySet()) {
				Map<String, PerformanceLog> log = layers.get(uri);
				for (String key : log.keySet()) {
					logs.put(key, log.get(key));
				}
			}
		}
		else {
			logs = repo.getLogs(getLayer());
		}

		Map<String, Object> model = mav.getModel();
		LOGGER.debug("Model: layer=" + getLayer() + " requestURI=" + getRequestURI());
		model.put(PERFORMANCE_LOGS_KEY, logs);
		model.put("requestURI", getRequestURI());
		model.put("layer", getLayer());
		return mav;
	}

	/**
	 * @return Returns the requestURI.
	 */
	public String getRequestURI() {
		return requestURI;
	}

	/**
	 * @param requestURI
	 *            The requestURI to set.
	 */
	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	/**
	 * @return Returns the layer.
	 */
	public String getLayer() {
		return layer;
	}

}
