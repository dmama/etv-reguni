package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("TraiterEvenementExterneRapport")
public class TraiterEvenementExterneRapport extends Document{

	public TraiterEvenementExterneRapport() {
	}

	public TraiterEvenementExterneRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}