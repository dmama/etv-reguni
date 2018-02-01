package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("RappelsLettresBienvenueRapport")
public class RappelLettresBienvenueRapport extends Document {

	public RappelLettresBienvenueRapport() {
	}

	public RappelLettresBienvenueRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
