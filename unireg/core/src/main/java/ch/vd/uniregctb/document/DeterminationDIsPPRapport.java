package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DetDIsRapport")
public class DeterminationDIsPPRapport extends Document {

	public DeterminationDIsPPRapport() {
	}

	public DeterminationDIsPPRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
