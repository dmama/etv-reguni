package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de récupération des noms des parents des anciens habitants
 */
@Entity
@DiscriminatorValue("RecupDonneesAnciensHabitantsRapport")
@Deprecated
public class RecuperationDonneesAnciensHabitantsRapport extends Document {

	public RecuperationDonneesAnciensHabitantsRapport() {
	}

	public RecuperationDonneesAnciensHabitantsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}