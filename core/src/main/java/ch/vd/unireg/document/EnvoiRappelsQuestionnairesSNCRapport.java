package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("RappelQSNCRapport")
public class EnvoiRappelsQuestionnairesSNCRapport extends Document {

	public EnvoiRappelsQuestionnairesSNCRapport() {
	}

	public EnvoiRappelsQuestionnairesSNCRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
