package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ExclCtbsEnvoiRapport")
public class ExclureContribuablesEnvoiRapport extends Document {

	public ExclureContribuablesEnvoiRapport() {
	}

	public ExclureContribuablesEnvoiRapport(String nom, String fileExtension, String description, String fileName, String subPath,
			long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
