package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Rapport d'exécution du job CorrigerFlagHabitantJob
 */
@Entity
@DiscriminatorValue("CorrectionFlagHabitantRapport")
public class CorrectionFlagHabitantRapport extends Document {

	public CorrectionFlagHabitantRapport() {
	}

	public CorrectionFlagHabitantRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
