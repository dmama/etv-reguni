package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport d'initialisation des données de filiations
 */
@Entity
@DiscriminatorValue("InitialisationFiliationsRapport")
public class InitialisationFilationsRapport extends Document {

	public InitialisationFilationsRapport() {
	}

	public InitialisationFilationsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}