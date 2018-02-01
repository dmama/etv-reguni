package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiQSNCRapport")
public class EnvoiQuestionnairesSNCRapport extends Document {

	public EnvoiQuestionnairesSNCRapport() {
	}

	public EnvoiQuestionnairesSNCRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
