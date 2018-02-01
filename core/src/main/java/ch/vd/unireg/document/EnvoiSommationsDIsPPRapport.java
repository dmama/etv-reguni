package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiSommationsDIsRapport")
public class EnvoiSommationsDIsPPRapport extends Document {

	public EnvoiSommationsDIsPPRapport() {
	}

	public EnvoiSommationsDIsPPRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
