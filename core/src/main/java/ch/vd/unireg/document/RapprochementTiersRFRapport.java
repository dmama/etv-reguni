package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de tentative de rapprochement des tiers RF
 */
@Entity
@DiscriminatorValue("RapprochementTiersRFRapport")
public class RapprochementTiersRFRapport extends Document {

	public RapprochementTiersRFRapport() {
	}

	public RapprochementTiersRFRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}