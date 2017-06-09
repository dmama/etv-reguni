package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("FusionDeCommunesRapport")
public class FusionDeCommunesRapport extends Document {

	public FusionDeCommunesRapport() {
	}

	public FusionDeCommunesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}