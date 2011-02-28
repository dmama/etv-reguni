package ch.vd.uniregctb.evenement.separation;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class SeparationAdapter extends GenericEvenementAdapter implements Separation {

	/**
	 * L'ancien conjoint de l'individu concerné par la séparation.
	 */
	private Individu ancienConjoint;

	protected SeparationAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);

		/*
		 * Calcul de l'année où a eu lieu l'événement
		 */
		int anneeEvenement = getDate().year();

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 */
		final long noIndividu = getNoIndividu();
		Individu individuPrincipal = context.getServiceCivil().getIndividu(noIndividu, anneeEvenement, AttributeIndividu.CONJOINT);
		this.ancienConjoint = context.getServiceCivil().getConjoint(individuPrincipal.getNoTechnique(), getDate().getOneDayBefore());
		
	}

	public Individu getAncienConjoint() {
		return ancienConjoint;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
