package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("MigrCoquillesPM")
public class MigrationCoquillesPMRapport extends Document {

	private static final long serialVersionUID = -6902229047337062357L;

	public MigrationCoquillesPMRapport() {
	}

	public MigrationCoquillesPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
