package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.PeriodeDecompte;

/**
 * Classe de transtypage pour Hibernate : Mois <--> varchar
 */
public class PeriodeDecompteUserType extends EnumUserType<PeriodeDecompte> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public PeriodeDecompteUserType() {
		super(PeriodeDecompte.class);
	}
}
