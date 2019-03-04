package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("AjouterDelaiPourMandataireRapport")
public class AjouterDelaiPourMandataireRapport extends Document {

	public AjouterDelaiPourMandataireRapport() {
	}

	public AjouterDelaiPourMandataireRapport(String nom, String fileExtension, String description, String fileName, String subPath,
	                                         long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
