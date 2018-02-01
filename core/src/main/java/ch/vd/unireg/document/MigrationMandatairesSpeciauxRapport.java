package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de migration des mandataires spéciaux de TAO-PP/SIMPA
 */
@Entity
@DiscriminatorValue("MigrationMandatairesSpeciauxRapport")
public class MigrationMandatairesSpeciauxRapport extends Document {

	public MigrationMandatairesSpeciauxRapport() {
	}

	public MigrationMandatairesSpeciauxRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
