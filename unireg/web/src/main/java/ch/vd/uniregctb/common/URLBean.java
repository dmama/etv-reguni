/**
 * 
 */
package ch.vd.uniregctb.common;

import org.springframework.web.servlet.mvc.Controller;

/**
 * Rep�sente un bean URL.
 * 
 * @author Akram BEN AISSI <mailto:akram.ben-aissi@vd.ch>
 * 
 */
public class URLBean {

	/**
	 * Le controller
	 */
	private Controller controller;

	/**
	 * L'URL telle qu'elle est mapp�e dans l'URL mapping
	 */
	private String mappedUrl;

	/**
	 * L'URL r�elle sans le contexte.
	 */
	private String url;

	/**
	 * @return the controller
	 */
	public Controller getController() {
		return controller;
	}

	/**
	 * @param controller
	 *            the controller to set
	 */
	public void setController(Controller controller) {
		this.controller = controller;
	}

	/**
	 * @return the mappedUrl
	 */
	public String getMappedUrl() {
		return mappedUrl;
	}

	/**
	 * @param mappedUrl
	 *            the mappedUrl to set
	 */
	public void setMappedUrl(String mappedUrl) {
		this.mappedUrl = mappedUrl;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

}
