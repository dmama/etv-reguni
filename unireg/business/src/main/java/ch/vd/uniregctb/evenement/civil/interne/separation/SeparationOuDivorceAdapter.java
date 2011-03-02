package ch.vd.uniregctb.evenement.civil.interne.separation;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter pour la séparation.
 * 
 * @author Pavel BLANCO
 */
public class SeparationOuDivorceAdapter extends EvenementCivilInterneBase implements Separation {

	/**
	 * L'ancien conjoint de l'individu concerné par la séparation.
	 */
	private Individu ancienConjoint;

	private SeparationOuDivorceHandler handler;

	protected SeparationOuDivorceAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, SeparationOuDivorceHandler handler) throws EvenementCivilInterneException {
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
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
