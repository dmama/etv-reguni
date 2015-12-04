package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ValidationJobRapport")
public class ValidationJobRapport extends Document {

	private static final long serialVersionUID = -6711906346556685174L;

	public ValidationJobRapport() {
	}

	public ValidationJobRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
