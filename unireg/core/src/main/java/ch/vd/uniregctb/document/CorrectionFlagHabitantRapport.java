package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Rapport d'exécution du job CorrigerFlagHabitantJob
 */
@Entity
@DiscriminatorValue("CorrectionFlagHabitantRapport")
public class CorrectionFlagHabitantRapport extends Document {

	private static final long serialVersionUID = -4013311793112167967L;

	public CorrectionFlagHabitantRapport() {
	}

	public CorrectionFlagHabitantRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
