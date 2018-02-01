package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiSommationLRsRapport")
public class EnvoiSommationLRsRapport extends Document {

	public EnvoiSommationLRsRapport() {
	}

	public EnvoiSommationLRsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
