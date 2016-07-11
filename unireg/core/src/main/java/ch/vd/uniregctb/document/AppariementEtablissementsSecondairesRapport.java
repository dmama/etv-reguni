package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de tentative d'appariement des établissements secondaires
 */
@Entity
@DiscriminatorValue("AppariementEtablissementsSecondairesRapport")
public class AppariementEtablissementsSecondairesRapport extends Document {

	public AppariementEtablissementsSecondairesRapport() {
	}

	public AppariementEtablissementsSecondairesRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}