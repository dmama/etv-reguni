package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production du rôle PP pour les OIDs vaudois.
 */
@Entity
@DiscriminatorValue("RolePPOfficesRapport")
public class RolePPOfficesRapport extends Document {

	public RolePPOfficesRapport() {
	}

	public RolePPOfficesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
