package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de calcul des dates de fin de droits (RF).
 */
@Entity
@DiscriminatorValue("FinsDeDroitRFRapport")
public class FinsDeDroitRFRapport extends Document {

	public FinsDeDroitRFRapport() {
	}

	public FinsDeDroitRFRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}