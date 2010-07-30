package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport d'exécution du batch d'extraction des listes AFC
 */
@Entity
@DiscriminatorValue("ExtractionAfcRapport")
public class ExtractionAfcRapport extends Document {

	private static final long serialVersionUID = 451574987594516913L;

	public ExtractionAfcRapport() {
	}

	public ExtractionAfcRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
