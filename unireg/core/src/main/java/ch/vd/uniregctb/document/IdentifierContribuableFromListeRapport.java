package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("IdentifierCtbFromListeRapport")
public class IdentifierContribuableFromListeRapport extends Document{

	public IdentifierContribuableFromListeRapport() {
	}

	public IdentifierContribuableFromListeRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
