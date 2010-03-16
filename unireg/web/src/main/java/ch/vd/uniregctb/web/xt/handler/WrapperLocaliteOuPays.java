package ch.vd.uniregctb.web.xt.handler;


import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import org.apache.commons.lang.StringEscapeUtils;


public class WrapperLocaliteOuPays {
	private String nomComplet = "???";
	private String numero ="???";

	public WrapperLocaliteOuPays( Object row) {
		if (row instanceof Localite) {
			final Localite localite = (Localite) row;
			nomComplet = StringEscapeUtils.escapeXml(localite.getNomAbregeMinuscule());
			numero = String.valueOf(localite.getNoOrdre());
		}
		if (row instanceof Pays) {
			final Pays pays = (Pays) row;
			nomComplet = StringEscapeUtils.escapeXml(pays.getNomMinuscule());
			numero = String.valueOf(pays.getNoOFS());
		}
	}

	/**
	 * @return the nom2
	 */
	public String getNomComplet() {
		return nomComplet;
	}

	/**
	 * @return the numero
	 */
	public String getNumero() {
		return numero;
	}
}
