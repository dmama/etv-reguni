package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DetQSNCRapport")
public class DeterminationQuestionnairesSNCRapport extends Document {

	public DeterminationQuestionnairesSNCRapport() {
	}

	public DeterminationQuestionnairesSNCRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
