package ch.vd.uniregctb.evenement.separation;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

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
	
	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivilData, ch.vd.uniregctb.interfaces.service.ServiceCivilService, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilData evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenement, serviceCivil, infrastructureService);
		
		/*
		 * Calcul de l'année où a eu lieu l'événement
		 */
		int anneeEvenement = getDate().year();

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 */
		EnumAttributeIndividu[] attributs = { EnumAttributeIndividu.CONJOINT };
		final long noIndividu = getNoIndividu();
		Individu individuPrincipal = serviceCivil.getIndividu(noIndividu, anneeEvenement, attributs);
		this.ancienConjoint = serviceCivil.getConjoint(individuPrincipal.getNoTechnique(),getDate().getOneDayBefore());
		
	}

	public Individu getAncienConjoint() {
		return ancienConjoint;
	}

}
