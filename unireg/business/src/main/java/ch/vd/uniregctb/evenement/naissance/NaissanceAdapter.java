package ch.vd.uniregctb.evenement.naissance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

public class NaissanceAdapter extends GenericEvenementAdapter implements Naissance {

	private final List<Individu> parents = new ArrayList<Individu>();

	private NaissanceHandler handler;

	protected NaissanceAdapter(EvenementCivilData evenement, EvenementCivilContext context, NaissanceHandler handler) throws EvenementAdapterException {
		super(evenement, context);
		this.handler = handler;

		/* Récupération des parents du nouveau né */
		final Individu bebe = getIndividu();
		if (bebe != null) {
			if (bebe.getPere() != null) {
				parents.add(getIndividu().getPere());
			}
			if (bebe.getMere() != null) {
				parents.add(getIndividu().getMere());
			}
		}
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#isContribuablePresentBefore()
	 */
	@Override
	public boolean isContribuablePresentBefore() {
		/* Le contribuable n'existe pas à l'arrivée d'un événement naissance */
		return false;
	}

	/**
	 * Retourne les parents du nouveau né.
	 */
	public List<Individu> getParents() {
		return parents;
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PARENTS);
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
