package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EnvoiDIsRapport")
public class EchoirDIsRapport extends Document {

	private static final long serialVersionUID = 752743137533620814L;

	public EchoirDIsRapport() {
	}

	public EchoirDIsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
