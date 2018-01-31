package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production du rôle PM pour les communes vaudoises.
 */
@Entity
@DiscriminatorValue("RolePMCommunesRapport")
public class RolePMCommunesRapport extends Document {

	public RolePMCommunesRapport() {
	}

	public RolePMCommunesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
