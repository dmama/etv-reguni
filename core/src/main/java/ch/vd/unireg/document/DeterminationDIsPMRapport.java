package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DetDIsPMRapport")
public class DeterminationDIsPMRapport extends Document {

	public DeterminationDIsPMRapport() {
	}

	public DeterminationDIsPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
