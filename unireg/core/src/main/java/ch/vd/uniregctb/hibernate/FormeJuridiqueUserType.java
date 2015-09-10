package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.FormeJuridique;

/**
 * Classe de transtypage pour Hibernate : FormeJuridique <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class FormeJuridiqueUserType extends EnumUserType<FormeJuridique> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public FormeJuridiqueUserType() {
		super(FormeJuridique.class);
	}

}
