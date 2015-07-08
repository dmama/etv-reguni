package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de populations pour les assujettis d'une période fiscale
 */
@Entity
@DiscriminatorValue("ListeAssujettisRapport")
public class ListeAssujettisRapport extends Document {

	private static final long serialVersionUID = -7744852235309791284L;

	public ListeAssujettisRapport() {
	}

	public ListeAssujettisRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}