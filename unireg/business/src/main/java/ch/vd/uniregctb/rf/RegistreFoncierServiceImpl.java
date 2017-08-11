package ch.vd.uniregctb.rf;

import java.io.InputStream;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	private TiersDAO tiersDAO;
	private ServiceCivilService serviceCivilService;
	private AdresseService adresseService;
	private TiersService tiersService;
	private ImmeubleDAO immeubleDAO;
	private PlatformTransactionManager transactionManager;

	@Override
	public RapprocherCtbResults rapprocherCtbRegistreFoncier(List<ProprietaireFoncier> listeProprietaireFoncier, StatusManager s, RegDate dateTraitement, int nbThreads) {
		final RapprocherCtbProcessor processor = new RapprocherCtbProcessor(transactionManager, tiersDAO, adresseService, tiersService, serviceCivilService);
		return processor.run(listeProprietaireFoncier, s, dateTraitement, nbThreads);
	}

	@Override
	public ImportImmeublesResults importImmeubles(InputStream csvStream, String encoding, StatusManager status) {
		final ImportImmeublesProcessor processor = new ImportImmeublesProcessor(immeubleDAO, transactionManager, tiersDAO, tiersService, adresseService);
		return processor.run(csvStream, encoding, status);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setImmeubleDAO(ImmeubleDAO immeubleDAO) {
		this.immeubleDAO = immeubleDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}
}
