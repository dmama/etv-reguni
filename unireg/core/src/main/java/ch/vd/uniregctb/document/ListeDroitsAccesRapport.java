package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information du rapport de dossiers protégés
 */
@Entity
@DiscriminatorValue("ListeDroitsAccesRapport")
public class ListeDroitsAccesRapport extends Document {

	public ListeDroitsAccesRapport() {
	}

	public ListeDroitsAccesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
