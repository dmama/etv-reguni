package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import org.apache.commons.lang.StringEscapeUtils;


public class WrapperCommuneOuPays {

	private String nomComplet = "???";
	private String numero ="???";

	public WrapperCommuneOuPays( Object row) {
		if (row instanceof Commune) {
			final Commune commune = (Commune) row;
			nomComplet = StringEscapeUtils.escapeXml(commune.getNomMinuscule());
			numero = String.valueOf(commune.getNoOFS());
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
