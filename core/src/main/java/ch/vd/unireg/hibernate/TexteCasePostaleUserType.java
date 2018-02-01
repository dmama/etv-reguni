package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.TexteCasePostale;

/**
 * Classe de transtypage pour Hibernate : TexteCasePostale <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class TexteCasePostaleUserType extends EnumUserType<TexteCasePostale> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public TexteCasePostaleUserType() {
		super(TexteCasePostale.class);
	}

}
