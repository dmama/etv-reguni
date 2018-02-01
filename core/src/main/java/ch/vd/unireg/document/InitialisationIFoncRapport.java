package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("InitialisationIFoncRapport")
public class InitialisationIFoncRapport extends Document {

	public InitialisationIFoncRapport() {
	}

	public InitialisationIFoncRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
