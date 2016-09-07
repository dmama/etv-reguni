package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des rôles PM pour les communes vaudoises.
 */
@Entity
@DiscriminatorValue("RolesCommunesPMRapport")
public class RolesCommunesPMRapport extends Document {

	public RolesCommunesPMRapport() {
	}

	public RolesCommunesPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
