package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiAnnexeImmeubleRapport")
public class EnvoiAnnexeImmeubleRapport extends Document {

	public EnvoiAnnexeImmeubleRapport() {
	}

	public EnvoiAnnexeImmeubleRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
