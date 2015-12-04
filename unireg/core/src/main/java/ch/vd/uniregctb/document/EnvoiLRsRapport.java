package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiLRsRapport")
public class EnvoiLRsRapport extends Document {

	private static final long serialVersionUID = 4288375085852879130L;

	public EnvoiLRsRapport() {
	}

	public EnvoiLRsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
