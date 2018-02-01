package ch.vd.unireg.hibernate;

import ch.vd.unireg.audit.AuditLevel;

/**
 * Classe de transtypage pour Hibernate : AuditLevel <--> varchar
 * 
 * @author Ludovic BERTIN
 */
public class AuditLevelUserType extends EnumUserType<AuditLevel> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public AuditLevelUserType() {
		super(AuditLevel.class);
	}

}
