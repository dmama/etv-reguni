package ch.vd.uniregctb.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport des statistiques des contribuables assujettis.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Entity
@DiscriminatorValue("StatistiquesCtbsRapport")
public class StatistiquesCtbsRapport extends Document {

	private static final long serialVersionUID = 8554086128587760866L;

	public StatistiquesCtbsRapport() {
	}

	public StatistiquesCtbsRapport(String nom, String fileExtension, String description, String fileName, String subPath, long fileSize) {
		super(nom, fileExtension, description, fileName, subPath, fileSize);
	}
}
