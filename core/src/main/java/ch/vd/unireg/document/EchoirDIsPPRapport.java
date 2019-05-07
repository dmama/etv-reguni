package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EchoirDIsPPRapport")
public class EchoirDIsPPRapport extends Document {

	public EchoirDIsPPRapport() {
	}

	public EchoirDIsPPRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
