package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("IdentifierCtbRapport")
public class IdentifierContribuableRapport extends Document{

	public IdentifierContribuableRapport() {
	}

	public IdentifierContribuableRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
