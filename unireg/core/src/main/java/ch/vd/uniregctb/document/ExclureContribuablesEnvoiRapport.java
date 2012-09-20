package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ExclCtbsEnvoiRapport")
public class ExclureContribuablesEnvoiRapport extends Document {

	private static final long serialVersionUID = 7664158889002088102L;

	public ExclureContribuablesEnvoiRapport() {
	}

	public ExclureContribuablesEnvoiRapport(String nom, String fileExtension, String description, String fileName, String subPath,
			long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
