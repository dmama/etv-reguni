package ch.vd.uniregctb.evenement.fin.permis;

import java.util.Collection;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Adapter pour la fin obtention d'un permis.
 *
 * @author Pavel BLANCO
 *
 */
public class FinPermisAdapter extends GenericEvenementAdapter implements FinPermis {

	private final static Logger LOGGER = Logger.getLogger(FinPermisAdapter.class);

	/** Le permis arrivant échéance. */
	private Permis permis;

	@Override
	public void init(EvenementCivilData evenementCivilData, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivilData, serviceCivil, infrastructureService);

		try {
			// on récupère le permis à partir de sa date de fin (= à la date d'événement)
			final int anneeCourante = evenementCivilData.getDateEvenement().year();
			final Collection<Permis> listePermis = serviceCivil.getPermis(super.getIndividu().getNoTechnique(), anneeCourante);
			if (listePermis == null) {
				throw new EvenementAdapterException("Le permis n'a pas été trouvé dans le registre civil");
			}
			for (Permis permis : listePermis) {
				if (RegDateHelper.equals(permis.getDateFinValidite(), evenementCivilData.getDateEvenement())) {
					this.permis = permis;
					break;
				}
			}

			// si le permis n'a pas été trouvé, on lance une exception
			if ( this.permis == null ) {
				throw new EvenementAdapterException("Le permis n'a pas été trouvé dans le registre civil");
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
