package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport d'import des immeubles du registre foncier (RF).
 */
@Entity
@DiscriminatorValue("MutationsRFDetectorRapport")
public class MutationsRFDetectorRapport extends Document {

	public MutationsRFDetectorRapport() {
	}

	public MutationsRFDetectorRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}