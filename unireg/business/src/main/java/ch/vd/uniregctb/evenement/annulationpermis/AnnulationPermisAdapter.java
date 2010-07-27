package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Adapter pour l'annulation de l'obtention de permis.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationPermisAdapter extends GenericEvenementAdapter implements AnnulationPermis {

	private static final Log LOGGER = LogFactory.getLog(AnnulationPermisHandler.class);
	
	/** Le permis obtenu. */
	private Permis permis;

	@Override
	public void init(EvenementCivilData evenementCivilData, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService, DataEventService dataEventService) throws EvenementAdapterException {
		super.init(evenementCivilData, serviceCivil, infrastructureService, dataEventService);

		try {
			final Collection<Permis> listePermis = super.getIndividu().getPermis();
			if (listePermis == null) {
				throw new EvenementAdapterException("Aucun permis trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (getDate().equals(permis.getDateDebutValidite()) && permis.getDateAnnulation() != null) {
					this.permis = permis;
					break;
				}
			}

			// si le permis n'a pas été trouvé, on lance une exception
			if ( this.permis == null ) {
				throw new EvenementAdapterException("Aucun permis trouvé dans le registre civil");
			}
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new EvenementAdapterException(e.getMessage(), e);
		}
	}
	
	public EnumTypePermis getTypePermis() {
		return permis.getTypePermis();
	}

	
}
