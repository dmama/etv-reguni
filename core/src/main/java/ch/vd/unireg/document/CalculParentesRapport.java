package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de calcul des données de parentés
 */
@Entity
@DiscriminatorValue("CalculParentesRapport")
public class CalculParentesRapport extends Document {

	public CalculParentesRapport() {
	}

	public CalculParentesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}