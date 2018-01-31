package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de migration des demandes de dégrèvement de SIMPA-PM.
 */
@Entity
@DiscriminatorValue("MigrationDDCsvLoaderRapport")
public class MigrationDDCsvLoaderRapport extends Document {

	public MigrationDDCsvLoaderRapport() {
	}

	public MigrationDDCsvLoaderRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}