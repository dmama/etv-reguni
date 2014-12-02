package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport de calcul des assujettissements par substitution
 */
@Entity
@DiscriminatorValue("AssujettiParSubstitutionRapport")
public class AssujettiParSubstitutionRapport extends Document {

	public AssujettiParSubstitutionRapport() {
	}

	public AssujettiParSubstitutionRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}