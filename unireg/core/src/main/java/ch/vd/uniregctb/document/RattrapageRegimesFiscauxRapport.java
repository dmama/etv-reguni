package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport du rattrapage des régimes fiscaux
 */
@Entity
@DiscriminatorValue("RattrapageRegimesFiscauxRapport")
public class RattrapageRegimesFiscauxRapport extends Document {

	public RattrapageRegimesFiscauxRapport() {
	}

	public RattrapageRegimesFiscauxRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}