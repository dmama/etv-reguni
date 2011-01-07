package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Rapport d'exécution du job CorrigerFlagHabitantJob
 */
@Entity
@DiscriminatorValue("CorrectionEtatDeclarationRapport")
public class CorrectionEtatDeclarationRapport extends Document {

	private static final long serialVersionUID = -190949074275648806L;

	public CorrectionEtatDeclarationRapport() {
	}

	public CorrectionEtatDeclarationRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
