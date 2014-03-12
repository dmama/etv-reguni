package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@SuppressWarnings("UnusedDeclaration")
@Entity
@DiscriminatorValue("PassageNouveauxRentiersSourciersEnMixteRapport")
public class PassageNouveauxRentiersSourciersEnMixteRapport extends Document {

	public PassageNouveauxRentiersSourciersEnMixteRapport() {
	}

	public PassageNouveauxRentiersSourciersEnMixteRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}

}
