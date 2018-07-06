package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production du rôle SNC global pour l'OIPM.
 */
@Entity
@DiscriminatorValue("RoleSNCRapport")
public class RoleSNCRapport extends Document {

	public RoleSNCRapport() {
	}

	public RoleSNCRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
