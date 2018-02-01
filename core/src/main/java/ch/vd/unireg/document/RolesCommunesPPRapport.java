package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des rôles PP (avant 2016) pour les communes vaudoises.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("RolesCommunesRapport")
public class RolesCommunesPPRapport extends Document {

	public RolesCommunesPPRapport() {
	}

	public RolesCommunesPPRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
