package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport des statistiques des déclarations d'impôt.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("StatistiquesDIsRapport")
public class StatistiquesDIsRapport extends Document {

	private static final long serialVersionUID = -2194082795005069066L;

	public StatistiquesDIsRapport() {
	}

	public StatistiquesDIsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
