package ch.vd.uniregctb.evenement.obtentionpermis;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Adapter pour l'obtention de nationalité.
 *
 * @author <a href="mailto:ludovic.bertin@oosphere.com">Ludovic BERTIN</a>
 */
public class ObtentionNationaliteAdapter extends GenericEvenementAdapter implements ObtentionNationalite {

	/**
	 * LOGGER log4J
	 */
	protected static Logger LOGGER = Logger.getLogger(ObtentionNationaliteAdapter.class);

	/**
	 * le numero OFS étendu de la commune de l'adresse principale
	 */
	private Integer numeroOfsEtenduCommunePrincipale;
	
	/*
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivilRegroupe, ch.vd.registre.civil.service.ServiceCivil, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilRegroupe evenementCivilRegroupe, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivilRegroupe, serviceCivil, infrastructureService);
		
		try {
			// on récupère la commune de l'adresse principale en gérant les fractions 
			//à utiliser pour déterminer le numeroOFS si besoin d'ouvrir un nouveau for
			Commune communePrincipale = infrastructureService.getCommuneByAdresse(getAdressePrincipale());
			this.numeroOfsEtenduCommunePrincipale = communePrincipale == null ? 0 : communePrincipale.getNoOFSEtendu();
		}
		catch (InfrastructureException e) {
			throw new EvenementAdapterException(e);
		}
	}
	
	public Integer getNumeroOfsEtenduCommunePrincipale() {
		return numeroOfsEtenduCommunePrincipale;
	}
}
