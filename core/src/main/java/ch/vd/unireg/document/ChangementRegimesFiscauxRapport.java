package ch.vd.unireg.document;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Méta-information sur le rapport du job de changement de régimes fiscaux.
 */
@Entity
@DiscriminatorValue("ChangementRegimesFiscauxRapport")
public class ChangementRegimesFiscauxRapport extends Document {

	public ChangementRegimesFiscauxRapport() {
	}
}
