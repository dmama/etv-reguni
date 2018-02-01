package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des rôles (avant 2016) pour l'OIPM
 */
@Entity
@DiscriminatorValue("RolesOIPMRapport")
public class RolesOIPMRapport extends Document {

	public RolesOIPMRapport() {
	}

	public RolesOIPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}