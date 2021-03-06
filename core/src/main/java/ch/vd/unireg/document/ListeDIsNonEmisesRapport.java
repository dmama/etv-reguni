package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de la liste des DIs non émises.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("ListeDIsNonEmisesRapport")
public class ListeDIsNonEmisesRapport extends Document {

	public ListeDIsNonEmisesRapport() {
	}

	public ListeDIsNonEmisesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
