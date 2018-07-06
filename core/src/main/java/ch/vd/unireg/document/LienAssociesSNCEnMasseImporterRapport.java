package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport d'import des liens entre associés et SNC importé du csv.
 */
@Entity
@DiscriminatorValue("LienAssociesSNCEnMasseImporterRapport")
public class LienAssociesSNCEnMasseImporterRapport extends Document {

	public LienAssociesSNCEnMasseImporterRapport() {
	}

	public LienAssociesSNCEnMasseImporterRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
