package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.ModeCommunication;

/**
 * Classe de transtypage pour Hibernate : ModeCommunication <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class ModeCommunicationUserType extends EnumUserType<ModeCommunication> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public ModeCommunicationUserType() {
		super(ModeCommunication.class);
	}

}
