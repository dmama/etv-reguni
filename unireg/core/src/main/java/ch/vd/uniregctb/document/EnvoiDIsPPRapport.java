package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiDIsRapport")
public class EnvoiDIsPPRapport extends Document {

	public EnvoiDIsPPRapport() {
	}

	public EnvoiDIsPPRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
