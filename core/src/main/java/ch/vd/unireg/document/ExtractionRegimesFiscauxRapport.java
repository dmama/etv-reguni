package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ExtractionRegimesFiscauxRapport")
public class ExtractionRegimesFiscauxRapport extends Document {

	public ExtractionRegimesFiscauxRapport() {
	}

	public ExtractionRegimesFiscauxRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
