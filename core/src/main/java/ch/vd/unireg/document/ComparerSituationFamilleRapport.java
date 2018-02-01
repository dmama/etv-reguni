package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ComparerSituationFamilleRapport")
public class ComparerSituationFamilleRapport extends Document{

	public ComparerSituationFamilleRapport() {
	}

	public ComparerSituationFamilleRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
