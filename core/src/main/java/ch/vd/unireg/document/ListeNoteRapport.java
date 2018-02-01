package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de la liste des Notes.
 *
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 */
@Entity
@DiscriminatorValue("ListeNoteRapport")
public class ListeNoteRapport extends Document {

	public ListeNoteRapport() {
	}

	public ListeNoteRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
