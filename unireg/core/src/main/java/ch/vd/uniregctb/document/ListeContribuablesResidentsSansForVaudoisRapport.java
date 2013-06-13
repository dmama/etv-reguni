package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de génération de la liste des contribuables suisses ou permis C
 * résidents sans for vaudois
 */
@Entity
@DiscriminatorValue("ListeCtbsResSansForVD")
public class ListeContribuablesResidentsSansForVaudoisRapport extends Document {

	private static final long serialVersionUID = -1962749557441433107L;

	public ListeContribuablesResidentsSansForVaudoisRapport() {
	}

	public ListeContribuablesResidentsSansForVaudoisRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
