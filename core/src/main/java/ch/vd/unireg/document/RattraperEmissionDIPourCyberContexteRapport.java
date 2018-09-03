package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("RattraperEmissionDIPourCyberContexteRapport")
public class RattraperEmissionDIPourCyberContexteRapport extends Document {

	public RattraperEmissionDIPourCyberContexteRapport() {
	}

	public RattraperEmissionDIPourCyberContexteRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
