package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiSommationsDIsPMRapport")
public class EnvoiSommationsDIsPMRapport extends Document {

	public EnvoiSommationsDIsPMRapport() {
	}

	public EnvoiSommationsDIsPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
