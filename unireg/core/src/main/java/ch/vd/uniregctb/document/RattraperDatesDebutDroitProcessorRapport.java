package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de rattrapage des dates de début des droits RF.
 */
@Entity
@DiscriminatorValue("RattraperDatesDebutDroitProcessorRapport")
public class RattraperDatesDebutDroitProcessorRapport extends Document {

	public RattraperDatesDebutDroitProcessorRapport() {
	}

	public RattraperDatesDebutDroitProcessorRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}