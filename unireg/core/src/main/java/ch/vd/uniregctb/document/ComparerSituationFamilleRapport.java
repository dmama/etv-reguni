package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ComparerSituationFamilleRapport")
public class ComparerSituationFamilleRapport extends Document{

	private static final long serialVersionUID = 8809653870053422078L;

	public ComparerSituationFamilleRapport() {
	}

	public ComparerSituationFamilleRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
