package ch.vd.uniregctb.evenement;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EvenementFiscalSituationFamille")
public class EvenementFiscalSituationFamille extends EvenementFiscal{

	/**
	 *
	 */
	private static final long serialVersionUID = 2038644150813882372L;


}
