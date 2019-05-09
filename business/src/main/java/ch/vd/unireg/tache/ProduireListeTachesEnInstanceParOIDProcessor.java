package ch.vd.unireg.tache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeTache;

public class ProduireListeTachesEnInstanceParOIDProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProduireListeTachesEnInstanceParOIDProcessor.class);

	private final HibernateTemplate hibernateTemplate;

	private final TiersService tiersService;
	private final AdresseService adresseService;

	private static final Map<String, TypeTache> TYPES_TACHE = buildTypesTacheMapping();

	protected static Map<String, TypeTache> buildTypesTacheMapping() {
		final Map<String, TypeTache> mapping = new HashMap<>();
		mapping.put("NOUVEAU_DOSSIER", TypeTache.TacheNouveauDossier);
		mapping.put("TRANS_DOSSIER", TypeTache.TacheTransmissionDossier);
		mapping.put("CTRL_DOSSIER", TypeTache.TacheControleDossier);
		mapping.put("ENVOI_DI_PP", TypeTache.TacheEnvoiDeclarationImpotPP);
		mapping.put("ENVOI_DI_PM", TypeTache.TacheEnvoiDeclarationImpotPM);
		mapping.put("ANNUL_DI", TypeTache.TacheAnnulationDeclarationImpot);
		mapping.put("ENVOI_QSNC", TypeTache.TacheEnvoiQuestionnaireSNC);
		mapping.put("ANNUL_QSNC", TypeTache.TacheAnnulationQuestionnaireSNC);
		return mapping;
	}

	public ProduireListeTachesEnInstanceParOIDProcessor(HibernateTemplate hibernateTemplate,
	                                                    TiersService tiersService,
	                                                    AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public ListeTachesEnInstanceParOID run(final RegDate dateTraitement, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final ListeTachesEnInstanceParOID rapportFinal = new ListeTachesEnInstanceParOID(dateTraitement, tiersService, adresseService);

		status.setMessage("Récupération des OID à vérifier...");

		final String sql = "SELECT CA.NUMERO_CA, TA.TACHE_TYPE, COUNT(*)"
				+ " FROM TACHE TA"
				+ " JOIN TIERS CA ON CA.NUMERO=TA.CA_ID"
				+ " WHERE TA.ETAT='EN_INSTANCE'"
				+ " AND TA.ANNULATION_DATE IS NULL"
				+ " AND TA.DATE_ECHEANCE <= " + dateTraitement.index()
				+ " GROUP BY CA.NUMERO_CA, TA.TACHE_TYPE"
				+ " ORDER BY CA.NUMERO_CA, TA.TACHE_TYPE";

		final List<Object[]> tachesTrouvees = hibernateTemplate.execute(session -> {
			final Query query = session.createNativeQuery(sql);
			//noinspection unchecked
			return (List<Object[]>) query.list();
		});

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


		double nombreTotalTaches = DataAccessUtils.intResult(hibernateTemplate.find(queryNombreTache, null));
		double nombreTotalContribuable = DataAccessUtils.intResult(hibernateTemplate.find(queryNombreContribuable, null));
		double moyenne = 0;
		if (nombreTotalContribuable>0) {

			moyenne = nombreTotalTaches/nombreTotalContribuable;
		}

		rapportFinal.setNombreTacheMoyen(moyenne);



		rapportFinal.interrompu = status.isInterrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterTaches(List<Object[]> tachesTrouvees, ListeTachesEnInstanceParOID rapport, RegDate dateTraitement) {
		for (Object[] objects : tachesTrouvees) {
			final int numeroOID = ((Number) objects[0]).intValue();
			final TypeTache typeTache = translateTypeTache((String) objects[1]);
			final long nombre = ((Number) objects[2]).longValue();
			final String nameType = typeTache.name();
			final String nomCollectivite = tiersService.getNomCollectiviteAdministrative(numeroOID);
			rapport.addTypeDeTacheEnInstance(numeroOID, nomCollectivite, nameType, nombre);
		}
	}

	private TypeTache translateTypeTache(String code){
		return TYPES_TACHE.get(code);
	}
}
