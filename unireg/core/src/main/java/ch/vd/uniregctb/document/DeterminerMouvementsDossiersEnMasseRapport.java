package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Rapport d'exécution du job de détermination des mouvements de dossiers en masse
 */
@Entity
@DiscriminatorValue("DeterminerMouvementsDossiersEnMasseRapport")
public class DeterminerMouvementsDossiersEnMasseRapport extends Document {

	public DeterminerMouvementsDossiersEnMasseRapport() {
	}

	public DeterminerMouvementsDossiersEnMasseRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
