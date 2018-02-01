package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @deprecated ancien rapport de l'import des données RF de l'extraction Michot (SIFISC-26536).
 */
@Entity
@DiscriminatorValue("ImportImmeublesRapport")
@Deprecated
public class ImportImmeublesRapport extends Document {

	public ImportImmeublesRapport() {
	}

	public ImportImmeublesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
