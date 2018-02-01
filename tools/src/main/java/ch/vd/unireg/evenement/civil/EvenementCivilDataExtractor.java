package ch.vd.unireg.evenement.civil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.rcpers.ServiceCivilRCPers;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.fidor.ServiceInfrastructureFidor;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.unireg.civil.ServiceInfraGetPaysSimpleCache;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;

public class EvenementCivilDataExtractor {

	private static final String RCPERS_URL = "http://rp-ws-pr.etat-de-vaud.ch/registres/rcpers/west/ws/v5";
	private static final String RCPERS_USER = "gvd0unireg";
	private static final String RCPERS_PWD = "Welc0me_";

	private static final String FIDOR_URL = "http://rp-ws-pr.etat-de-vaud.ch/fiscalite/fidor/ws/v5";
	private static final String FIDOR_USER = "gvd0unireg";
	private static final String FIDOR_PWD = "Welc0me_";

	private static final String sourceFilename = "evenements.csv";

	public static void main(String[] args) throws Exception {
		final ServiceCivilRaw serviceCivil = buildServiceCivil();

		// lecture du fichier d'entrée
		final List<Long> ids = new ArrayList<>();
		try (InputStream in = EvenementCivilDataExtractor.class.getResourceAsStream(sourceFilename);
		     InputStreamReader fis = new InputStreamReader(in);
		     BufferedReader reader = new BufferedReader(fis)) {

			String line = reader.readLine();
			while (line != null) {
				final Long id = Long.valueOf(line);
				ids.add(id);
				line = reader.readLine();
			}
		}

		// boucle sur tous les événements donnés
		for (Long id : ids) {
			final IndividuApresEvenement indAfterEvent = serviceCivil.getIndividuAfterEvent(id);
			System.out.println(String.format("%d;%s;%s;%s;%d;%d",
			                                 id, indAfterEvent.getTypeEvenement(), indAfterEvent.getActionEvenement(),
			                                 RegDateHelper.dateToDashString(indAfterEvent.getDateEvenement()), indAfterEvent.getIdEvenementRef(), indAfterEvent.getIndividu().getNoTechnique()));
		}
	}

	private static ServiceCivilRaw buildServiceCivil() throws Exception {

		final WebClientPool rcpersPool = new WebClientPool();
		rcpersPool.setBaseUrl(RCPERS_URL);
		rcpersPool.setUsername(RCPERS_USER);
		rcpersPool.setPassword(RCPERS_PWD);

		final RcPersClientImpl rcpersClient = new RcPersClientImpl();
		rcpersClient.setWcPool(rcpersPool);
		rcpersClient.setEventPath("event");
		rcpersClient.setPeopleByEventIdPath("persons/byevent");

		final WebClientPool fidorPool = new WebClientPool();
		fidorPool.setBaseUrl(FIDOR_URL);
		fidorPool.setUsername(FIDOR_USER);
		fidorPool.setPassword(FIDOR_PWD);

		final FidorClientImpl fidorClient = new FidorClientImpl();
		fidorClient.setWcPool(fidorPool);

		final ServiceInfrastructureFidor infraServiceFiDor = new ServiceInfrastructureFidor();
		infraServiceFiDor.setFidorClient(fidorClient);

		final ServiceInfrastructureRaw infraServiceCache = new ServiceInfraGetPaysSimpleCache(infraServiceFiDor);

		final ServiceCivilRCPers serviceCivilRCPers = new ServiceCivilRCPers();
		serviceCivilRCPers.setClient(rcpersClient);
		serviceCivilRCPers.setInfraService(infraServiceCache);

		return serviceCivilRCPers;
	}
}
