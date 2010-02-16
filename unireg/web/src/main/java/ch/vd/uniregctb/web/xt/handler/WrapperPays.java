/**
 *
 */
package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Pays;
import org.apache.commons.lang.StringEscapeUtils;


/**
 * @author xcicfh
 *
 */
public class WrapperPays {
	private String nomMinuscule = "???";
	private String noOFS = "???";

	public WrapperPays( Pays pays) {
			this.nomMinuscule = StringEscapeUtils.escapeXml(pays.getNomMinuscule());
			this.noOFS = String.valueOf(pays.getNoOFS());
	}

	/**
	 * @return the nomMinuscule
	 */
	public String getNomMinuscule() {
		return nomMinuscule;
	}


	/**
	 * @return the noOFS
	 */
	public String getNoOFS() {
		return noOFS;
	}
}
