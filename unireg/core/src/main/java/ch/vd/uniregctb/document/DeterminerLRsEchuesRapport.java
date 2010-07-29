package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des LR échues
 */
@Entity
@DiscriminatorValue("DeterminerLRsEchuesRapport")
public class DeterminerLRsEchuesRapport extends Document {

	public DeterminerLRsEchuesRapport() {
	}

	public DeterminerLRsEchuesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
