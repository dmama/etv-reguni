package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de populations pour les bases acomptes
 */
@Entity
@DiscriminatorValue("AcomptesRapport")
public class AcomptesRapport extends Document {

	private static final long serialVersionUID = -9172726369448172743L;

	public AcomptesRapport() {
	}

	public AcomptesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}