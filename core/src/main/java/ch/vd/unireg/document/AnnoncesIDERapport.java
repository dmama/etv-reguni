package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport des annonces à l'IDE
 */
@Entity
@DiscriminatorValue("AnnoncesIDERapport")
public class AnnoncesIDERapport extends Document {

	public AnnoncesIDERapport() {
	}

	public AnnoncesIDERapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}