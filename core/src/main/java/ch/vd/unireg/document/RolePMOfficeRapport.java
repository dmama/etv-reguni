package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production du rôle PM global pour l'OIPM.
 */
@Entity
@DiscriminatorValue("RolePMOfficeRapport")
public class RolePMOfficeRapport extends Document {

	public RolePMOfficeRapport() {
	}

	public RolePMOfficeRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
