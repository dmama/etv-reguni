package ch.vd.unireg.mouvement;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import ch.vd.unireg.type.Localisation;

@Entity
@DiscriminatorValue(value = "ReceptionClassementGeneral")
public class ReceptionDossierClassementGeneral extends ReceptionDossier {

	@Transient
	@Override
	public Localisation getLocalisation() {
		return Localisation.CLASSEMENT_GENERAL;
	}
}