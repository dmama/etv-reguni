package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TarifImpotSource;

/**
 * Classe de transtypage pour Hibernate : TarifImpotSource <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TarifImpotSourceUserType extends EnumUserType<TarifImpotSource> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TarifImpotSourceUserType() {
		super(TarifImpotSource.class);
	}

}
