package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de rattrapage des dates métier des droits RF.
 */
@Entity
@DiscriminatorValue("RattraperDatesMetierDroitProcessorRapport")
public class RattraperDatesMetierDroitProcessorRapport extends Document {

	public RattraperDatesMetierDroitProcessorRapport() {
	}

	public RattraperDatesMetierDroitProcessorRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}