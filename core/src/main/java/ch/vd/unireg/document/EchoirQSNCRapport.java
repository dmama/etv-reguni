package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information stockées dans la DB Unireg sur les fichiers PDFs générés par {@link PdfEchoirQSNCRapport}.
 */
@Entity
@DiscriminatorValue("EchoirQSNCRapport")
public class EchoirQSNCRapport extends Document {

	public EchoirQSNCRapport() {
	}

	public EchoirQSNCRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
