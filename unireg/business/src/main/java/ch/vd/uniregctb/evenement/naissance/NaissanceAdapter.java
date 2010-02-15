package ch.vd.uniregctb.evenement.naissance;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class NaissanceAdapter extends GenericEvenementAdapter implements Naissance {

	private final List<Individu> parents = new ArrayList<Individu>();

	/**
	 * Constructeur
	 */
	public  NaissanceAdapter() {
		super();
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivilRegroupe, ch.vd.registre.civil.service.ServiceCivil, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilRegroupe evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService)
			throws EvenementAdapterException {
		super.init(evenement, serviceCivil, infrastructureService);

		/* Récupération des parents du nouveau né */
		if ( getIndividu().getPere() != null )
			parents.add(getIndividu().getPere());

		if ( getIndividu().getMere() != null )
			parents.add(getIndividu().getMere());

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



}
