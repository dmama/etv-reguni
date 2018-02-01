package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de recalcul des tâches
 */
@Entity
@DiscriminatorValue("RecalculTachesRapport")
public class RecalculTachesRapport extends Document {

	public RecalculTachesRapport() {
	}

	public RecalculTachesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}