package ch.vd.uniregctb.tache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeTache;

public class ProduireListeTachesEnInstanceParOIDProcessor {

	private static final int BATCH_SIZE = 100;

	final Logger LOGGER = Logger.getLogger(ProduireListeTachesEnInstanceParOIDProcessor.class);

	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;

	private final ServiceInfrastructureService serviceInfrastructureService;




	public ProduireListeTachesEnInstanceParOIDProcessor(HibernateTemplate hibernateTemplate,
			ServiceInfrastructureService serviceInfrastructureService, PlatformTransactionManager transactionManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;

		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public ListeTachesEnInstanceParOID run(final RegDate dateTraitement, StatusManager s) throws Exception {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final ListeTachesEnInstanceParOID rapportFinal = new ListeTachesEnInstanceParOID(dateTraitement);

		status.setMessage("Récupération des OID à vérifier...");

		final List<Long> ids = getOidIds();
		final String query = "SELECT tache.contribuable.officeImpotId," +
				"tache.class," +
				"count(*)" +
				" FROM Tache tache " +
				" WHERE" +
				" tache.etat = 'EN_INSTANCE'" +
				" AND tache.annulationDate is null " +
				" AND tache.dateEcheance <="+dateTraitement.index()+
				" AND tache.contribuable.annulationDate is null" +
				" AND tache.contribuable.officeImpotId is not null" +
				" ORDER BY tache.contribuable.officeImpotId,tache.class" +
				" GROUP BY tache.contribuable.officeImpotId,tache.class";


		final List<Object[]> tachesTrouvees = hibernateTemplate.find(query);

		traiterTaches(tachesTrouvees, rapportFinal, dateTraitement);

		final String queryNombreTache = "SELECT count(*) FROM Tache tache" +
										" WHERE " +
										" tache.etat = 'EN_INSTANCE'" +
										" AND tache.annulationDate is null " +
										" AND tache.dateEcheance <="+dateTraitement.index();

		final String queryNombreContribuable = "SELECT count(tiers.numero) FROM Tiers tiers" +
												" WHERE tiers.annulationDate is null " +
												" AND tiers.numero in (select tache.contribuable.numero" +
												"						from Tache tache" +
												"						 WHERE " +
												"						  tache.etat = 'EN_INSTANCE'" +
												" AND tache.annulationDate is null " +
												" AND tache.dateEcheance <="+dateTraitement.index()+ ')';


		double nombreTotalTaches = DataAccessUtils.intResult(hibernateTemplate.find(queryNombreTache));
		double nombreTotalContribuable = DataAccessUtils.intResult(hibernateTemplate.find(queryNombreContribuable));
		double moyenne = 0;
		if (nombreTotalContribuable>0) {

			moyenne = nombreTotalTaches/nombreTotalContribuable;
		}

		rapportFinal.setNombreTacheMoyen(moyenne);



		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	private List<Long> getOidIds() throws ServiceInfrastructureException {
		List<EnumTypeCollectivite> typesCollectivite = new ArrayList<EnumTypeCollectivite>();
		typesCollectivite.add(EnumTypeCollectivite.SIGLE_CIR);
		List<CollectiviteAdministrative> collectivites = serviceInfrastructureService
				.getCollectivitesAdministratives(typesCollectivite);
		List<Long> listId = new ArrayList<Long>();
		for (CollectiviteAdministrative collectiviteAdministrative : collectivites) {
			listId.add((long) collectiviteAdministrative.getNoColAdm());
		}
		Collections.sort(listId);
		return listId;
	}




	private void traiterTaches(List<Object[]> tachesTrouvees, ListeTachesEnInstanceParOID rapport, RegDate dateTraitement) throws Exception {



		for (Object[] objects : tachesTrouvees) {
			int numeroOID = (Integer) objects[0];
			TypeTache typeTache = translateTypeTache((String) objects[1]);
			long  nombre = (Long) objects[2];
			String nameType = typeTache.name();



			rapport.addTypeDeTacheEnInstance(numeroOID, nameType, nombre);
		}

	}
	private TypeTache translateTypeTache(String code){

		if ("NOUVEAU_DOSSIER".equals(code)) {
			return TypeTache.TacheNouveauDossier;

		}
		else if("TRANS_DOSSIER".equals(code)){
			return TypeTache.TacheTransmissionDossier;
		}
		else if("CTRL_DOSSIER".equals(code)){
			return TypeTache.TacheControleDossier;
		}
		else if("ENVOI_DI".equals(code)){
			return TypeTache.TacheEnvoiDeclarationImpot;
		}
		else if("ANNUL_DI".equals(code)){
			return TypeTache.TacheAnnulationDeclarationImpot;
		}
		return null;

	}
}
