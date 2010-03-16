package ch.vd.uniregctb.evenement.externe;

import java.util.Collection;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.RegDate;

public interface EvenementExterneService {

	public void sendEvenementExterne(IEvenementExterne evenement) throws Exception;

	public EvenementImpotSourceQuittanceType createEvenementQuittancement(TypeQuittance.Enum quitancement, Long numeroCtb, RegDate dateDebut,
			RegDate dateFin, RegDate dateQuittance);

	public Collection<EvenementExterne> getEvenementExternes(boolean ascending, EtatEvenementExterne... etatEvenementExternes);

}
