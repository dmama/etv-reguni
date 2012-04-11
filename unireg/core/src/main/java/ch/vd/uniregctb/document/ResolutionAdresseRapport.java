package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("ResolutionAdresseRapport")
public class ResolutionAdresseRapport extends Document{

	private static final long serialVersionUID = 8809632870053483078L;

	public ResolutionAdresseRapport() {
	}

	public ResolutionAdresseRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}