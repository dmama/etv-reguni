package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production du rôle PP pour les communes vaudoises.
 */
@Entity
@DiscriminatorValue("RolePPCommunesRapport")
public class RolePPCommunesRapport extends Document {

	public RolePPCommunesRapport() {
	}

	public RolePPCommunesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
