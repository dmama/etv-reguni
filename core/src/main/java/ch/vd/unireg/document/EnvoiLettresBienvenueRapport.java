package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiLettresBienvenueRapport")
public class EnvoiLettresBienvenueRapport extends Document {

	public EnvoiLettresBienvenueRapport() {
	}

	public EnvoiLettresBienvenueRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
