package ch.vd.uniregctb.web.xt.handler;


import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;



public class WrapperLocaliteOuPays {
	private String nomComplet = "???";
	private String numero ="???";

	public WrapperLocaliteOuPays( Object row) {
		if (row instanceof Localite) {
			nomComplet = ((Localite)row).getNomAbregeMinuscule();
			numero = String.valueOf(((Localite)row).getNoOrdre());
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
