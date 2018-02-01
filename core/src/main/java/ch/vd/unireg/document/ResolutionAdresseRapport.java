package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ResolutionAdresseRapport")
public class ResolutionAdresseRapport extends Document{

	public ResolutionAdresseRapport() {
	}

	public ResolutionAdresseRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}