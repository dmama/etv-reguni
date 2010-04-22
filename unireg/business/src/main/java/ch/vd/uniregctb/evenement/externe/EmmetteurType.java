package ch.vd.uniregctb.evenement.externe;

/**
 * Contient tous les emmetteur pouvant envoyer un événement.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public enum EmmetteurType {

	ImpotSource,
	RegistreCivil
;



	public int getOrdinal() {
		return this.ordinal();
	}

	public String getName() {
		return this.name();
	}
}
