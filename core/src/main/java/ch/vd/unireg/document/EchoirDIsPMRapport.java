package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EchoirDIsPMRapport")
public class EchoirDIsPMRapport extends Document {

	public EchoirDIsPMRapport() {
	}

	public EchoirDIsPMRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
