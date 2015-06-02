package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des rôles pour les communes vaudoises.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("RolesCommunesRapport")
public class RolesCommunesRapport extends Document {

	private static final long serialVersionUID = 6732981841355264805L;

	public RolesCommunesRapport() {
	}

	public RolesCommunesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
