package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de récupération des noms des parents des anciens habitants
 */
@Entity
@DiscriminatorValue("RecupOriginesNonHabitantsRapport")
@Deprecated
public class RecuperationOriginesNonHabitantsRapport extends Document {

	public RecuperationOriginesNonHabitantsRapport() {
	}

	public RecuperationOriginesNonHabitantsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}