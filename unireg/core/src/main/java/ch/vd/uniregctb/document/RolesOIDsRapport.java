package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des rôles pour les OID vaudois
 */
@Entity
@DiscriminatorValue("RolesOIDsRapport")
public class RolesOIDsRapport extends Document {

	public RolesOIDsRapport() {
	}

	public RolesOIDsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}