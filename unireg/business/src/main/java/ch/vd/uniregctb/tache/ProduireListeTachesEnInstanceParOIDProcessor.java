package ch.vd.uniregctb.tache;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeTache;

public class ProduireListeTachesEnInstanceParOIDProcessor {

	final Logger LOGGER = LoggerFactory.getLogger(ProduireListeTachesEnInstanceParOIDProcessor.class);

	private final HibernateTemplate hibernateTemplate;

	private final TiersService tiersService;
	private final AdresseService adresseService;


	public ProduireListeTachesEnInstanceParOIDProcessor(HibernateTemplate hibernateTemplate,
	                                                    TiersService tiersService,
	                                                    AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public ListeTachesEnInstanceParOID run(final RegDate dateTraitement, StatusManager s) throws Exception {

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

		@SuppressWarnings("unchecked")
		final List<Object[]> tachesTrouvees = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
			@Override
			public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createSQLQuery(sql);
				return query.list();
			}
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



		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterTaches(List<Object[]> tachesTrouvees, ListeTachesEnInstanceParOID rapport, RegDate dateTraitement) throws Exception {
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
		if ("NOUVEAU_DOSSIER".equals(code)) {
			return TypeTache.TacheNouveauDossier;
		}
		else if ("TRANS_DOSSIER".equals(code)) {
			return TypeTache.TacheTransmissionDossier;
		}
		else if ("CTRL_DOSSIER".equals(code)) {
			return TypeTache.TacheControleDossier;
		}
		else if ("ENVOI_DI_PP".equals(code)) {
			return TypeTache.TacheEnvoiDeclarationImpotPP;
		}
		else if ("ENVOI_DI_PM".equals(code)) {
			return TypeTache.TacheEnvoiDeclarationImpotPM;
		}
		else if ("ANNUL_DI".equals(code)) {
			return TypeTache.TacheAnnulationDeclarationImpot;
		}
		return null;
	}
}
