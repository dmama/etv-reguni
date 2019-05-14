package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;


/**
 * Rapport d'exécution du batch d'impression des chemises TO
 * @deprecated Cette classe reste dans le jar pour pouvoir relire des rapports sauvegardés en base, mais n'est plus jamais générée.
 */
@Deprecated
@Entity
@DiscriminatorValue("ChemisesTORapport")
public class ImpressionChemisesTORapport extends Document {
	public ImpressionChemisesTORapport() {
	}
}
