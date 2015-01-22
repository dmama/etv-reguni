package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("IdentifierCtbRapport")
public class IdentifierContribuableRapport extends Document{

	private static final long serialVersionUID = 8809653870053422078L;

	public IdentifierContribuableRapport() {
	}

	public IdentifierContribuableRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
