package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport d'exécution du batch d'extraction des listes AFC
 */
@Entity
@DiscriminatorValue("ExtractionAfcRapport")         // nom historique...
public class ExtractionDonneesRptRapport extends Document {

	public ExtractionDonneesRptRapport() {
	}

	public ExtractionDonneesRptRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
