package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Classe de transtypage pour Hibernate : MotifRattachement <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class MotifRattachementUserType extends EnumUserType<MotifRattachement> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public MotifRattachementUserType() {
		super(MotifRattachement.class);
	}

}
