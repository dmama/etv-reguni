/**
 *
 */
package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;
import org.apache.commons.lang.StringEscapeUtils;


/**
 * @author xcicfh
 *
 */
public class WrapperRue {

	private final String designationCourrier;
	private final String noRue;
	private final String noLocalite;
	private final String nomLocalite;

	public WrapperRue( Rue rue, Localite localite) {
		this.designationCourrier = StringEscapeUtils.escapeXml(rue.getDesignationCourrier());
		this.noRue = String.valueOf(rue.getNoRue());
		this.noLocalite = rue.getNoLocalite().toString();
		this.nomLocalite = StringEscapeUtils.escapeXml(localite.getNomAbregeMinuscule());
	}

	/**
	 * @return the designationCourrier
	 */
	public String getDesignationCourrier() {
		return designationCourrier;
	}

	/**
	 * @return the noRue
	 */
	public String getNoRue() {
		return noRue;
	}

	/**
	 * @return the noLocalite
	 */
	public String getNoLocalite() {
		return noLocalite;
	}

	public String getNomLocalite()  {
		 return nomLocalite;
	}

}
