package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de production des listes nominatives
 */
@Entity
@DiscriminatorValue("ListesNominativesRapport")
public class ListesNominativesRapport extends Document {

	public ListesNominativesRapport() {
	}

	public ListesNominativesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
