package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("TraiterEvenementExterneRapport")
public class TraiterEvenementExterneRapport extends Document{

	private static final long serialVersionUID = 8809653873691334278L;

	public TraiterEvenementExterneRapport() {
	}

	public TraiterEvenementExterneRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}