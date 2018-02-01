package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de traitement des mutations des immeubles du registre foncier (RF).
 */
@Entity
@DiscriminatorValue("MutationsRFProcessorRapport")
public class MutationsRFProcessorRapport extends Document {

	public MutationsRFProcessorRapport() {
	}

	public MutationsRFProcessorRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}