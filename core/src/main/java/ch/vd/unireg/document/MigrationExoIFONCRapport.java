package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de migration exonérations IFONC de SIMPA-PM.
 */
@Entity
@DiscriminatorValue("MigrationExoIFONCRapport")
public class MigrationExoIFONCRapport extends Document {

	public MigrationExoIFONCRapport() {
	}

	public MigrationExoIFONCRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}