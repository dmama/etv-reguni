/**
 *
 */
package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Rue;



/**
 * @author xcicfh
 *
 */
public class WrapperRue {
	final private String designationCourrier;
	final private String noRue;
	final private String noLocalite;
	final private Localite localite;

	public WrapperRue( Rue rue, Localite localite) {
			this.designationCourrier = rue.getDesignationCourrier();
			this.noRue = String.valueOf(rue.getNoRue());
			this.noLocalite = rue.getNoLocalite().toString();
			this.localite = localite;
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
		 return localite.getNomAbregeMinuscule();
	}

}
