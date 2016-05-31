package ch.vd.uniregctb.document;

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

	private static final long serialVersionUID = 8554086128587760866L;

	public ListeDIsNonEmisesRapport() {
	}

	public ListeDIsNonEmisesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
