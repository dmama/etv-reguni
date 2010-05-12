package ch.vd.uniregctb.evenement.divorce;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.separation.SeparationAdapter;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Adapter pour le divorce.
 * 
 * @author Pavel BLANCO
 */
public class DivorceAdapter extends SeparationAdapter implements Divorce {

	protected static Logger LOGGER = Logger.getLogger(DivorceAdapter.class);
	
	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivilData, ch.vd.uniregctb.interfaces.service.ServiceCivilService, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilData evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenement, serviceCivil, infrastructureService);
	}

}
