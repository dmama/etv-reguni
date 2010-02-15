package ch.vd.uniregctb.web.xt.handler;

import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;



public class WrapperCommuneOuPays {

	private String nomComplet = "???";
	private String numero ="???";

	public WrapperCommuneOuPays( Object row) {
		if (row instanceof Commune) {
			nomComplet = ((Commune)row).getNomMinuscule();
			numero = String.valueOf(((Commune)row).getNoOFS());
		}
		if (row instanceof Pays) {
			nomComplet = ((Pays) row).getNomMinuscule();
			numero = String.valueOf(((Pays)row).getNoOFS());
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
