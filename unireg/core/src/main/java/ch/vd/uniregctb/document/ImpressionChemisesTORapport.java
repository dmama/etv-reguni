package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;


/**
 * Rapport d'exécution du batch d'impression des chemises TO
 */
@Entity
@DiscriminatorValue("ChemisesTORapport")
public class ImpressionChemisesTORapport extends Document {

	private static final long serialVersionUID = 5582546081227737596L;

	public ImpressionChemisesTORapport() {
	}

	public ImpressionChemisesTORapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
