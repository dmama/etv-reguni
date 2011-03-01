package ch.vd.uniregctb.evenement.separation;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class SeparationOuDivorceAdapter extends GenericEvenementAdapter implements Separation {

	/**
	 * L'ancien conjoint de l'individu concerné par la séparation.
	 */
	private Individu ancienConjoint;

	private SeparationOuDivorceHandler handler;

	protected SeparationOuDivorceAdapter(EvenementCivilData evenement, EvenementCivilContext context, SeparationOuDivorceHandler handler) throws EvenementAdapterException {
		super(evenement, context);
		this.handler = handler;

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

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
