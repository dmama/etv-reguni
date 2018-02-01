package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("MigrCoquillesPM")
public class MigrationCoquillesPMRapport extends Document {

	public MigrationCoquillesPMRapport() {
	}

	public MigrationCoquillesPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
