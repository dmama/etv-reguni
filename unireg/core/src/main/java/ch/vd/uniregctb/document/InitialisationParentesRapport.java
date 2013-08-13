package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport d'initialisation des données de parentés
 */
@Entity
@DiscriminatorValue("InitialisationParentesRapport")
public class InitialisationParentesRapport extends Document {

	public InitialisationParentesRapport() {
	}

	public InitialisationParentesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}