package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.type.TexteCasePostale;

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
