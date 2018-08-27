package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport des populations pour l'échange automatique de renseignements EAR
 */
@Entity
@DiscriminatorValue("ListeEchangeRenseignementsRapport")
public class ListeEchangeRenseignementsRapport extends Document {

	public ListeEchangeRenseignementsRapport() {
	}

	public ListeEchangeRenseignementsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}