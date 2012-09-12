package ch.vd.uniregctb.metier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.validation.ValidationService;

public class PassageNouveauxRentiersSourciersEnMixteProcessor {

	private final Logger LOGGER = Logger.getLogger(PassageNouveauxRentiersSourciersEnMixteProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final AdresseService adresseService;
	private final ServiceInfrastructureService serviceInfra;
	private final ServiceCivilService serviceCivil;
	private final ValidationService validationService;
	private final int ageRentierHomme;
	private final int ageRentierFemme;

	private PassageNouveauxRentiersSourciersEnMixteResults rapport;

	private HashSet <Long> conjointAIgnorerGlobal = new HashSet<Long>();

	public PassageNouveauxRentiersSourciersEnMixteProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersService tiersService, TiersDAO tiersDAO,
	                                                        AdresseService adresseService, ServiceInfrastructureService serviceInfra, ServiceCivilService serviceCivil,
	                                                        ValidationService validationService, ParametreAppService parametreAppService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.tiersDAO = tiersDAO;
		this.adresseService = adresseService;
		this.serviceInfra = serviceInfra;
		this.serviceCivil = serviceCivil;
		this.validationService = validationService;
		this.ageRentierFemme = parametreAppService.getAgeRentierFemme();
		this.ageRentierHomme = parametreAppService.getAgeRentierHomme();

	}

	public PassageNouveauxRentiersSourciersEnMixteResults run(final RegDate dateTraitement, StatusManager statusManager) {
		if (statusManager == null) {
			statusManager = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = statusManager;

		final PassageNouveauxRentiersSourciersEnMixteResults rapportFinal = new PassageNouveauxRentiersSourciersEnMixteResults(dateTraitement);

		final List<Long> list = getListPotentielsNouveauxRentiersSourciers(dateTraitement);

		final BatchTransactionTemplate<Long, PassageNouveauxRentiersSourciersEnMixteResults> template = new BatchTransactionTemplate<Long, PassageNouveauxRentiersSourciersEnMixteResults>(list, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, s, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, PassageNouveauxRentiersSourciersEnMixteResults>() {

			@Override
			public PassageNouveauxRentiersSourciersEnMixteResults createSubRapport() {
				return new PassageNouveauxRentiersSourciersEnMixteResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, PassageNouveauxRentiersSourciersEnMixteResults r) throws Exception {
				rapport = r;
				HashSet<Long> conjointAIgnorer = new HashSet<Long>();
				traiteBatch(batch, dateTraitement, s, conjointAIgnorer);
				conjointAIgnorerGlobal.addAll(conjointAIgnorer);
				return !s.interrupted();
			}

			@Override
			public void afterTransactionCommit() {
				int percent = (100 * rapportFinal.nbSourciersTotal) / list.size();
				s.setMessage(String.format(
						"%d sourciers traités sur %d (convertis = %s, erreurs = %s, conjoints ignorés = %s, hors-suisse = %s, trop jeune = %s)",
						rapportFinal.nbSourciersTotal, list.size(), rapportFinal.sourciersConvertis.size(), rapportFinal.sourciersEnErreurs.size(), rapportFinal.nbSourciersConjointsIgnores,
						rapportFinal.nbSourciersHorsSuisse, rapportFinal.nbSourciersTropJeunes), percent);
			}
		});

		if (statusManager.interrupted()) {
			statusManager.setMessage("Le passage des nouveaux rentiers sourciers en mixte 1 a été interrompue."
					+ " Nombre de sourciers traités au moment de l'interruption = " + rapportFinal.sourciersConvertis.size());
			rapportFinal.interrompu = true;
		}
		else {
			statusManager.setMessage("Le passage des nouveaux rentiers sourciers en mixte 1 est terminé." + " Nombre de sourciers traités = "
					+ rapportFinal.sourciersConvertis.size() + ". Nombre d'erreurs = " + rapportFinal.sourciersEnErreurs.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<Long> batch, RegDate dateReference, StatusManager status, HashSet<Long> conjointAIgnorer) {

		// On préchauffe le cache des individus, si possible
		if (serviceCivil.isWarmable()) {
			final Set<Long> numeroIndividus = tiersDAO.getNumerosIndividu(batch, false);
			if (!numeroIndividus.isEmpty()) {
				serviceCivil.getIndividus(numeroIndividus, dateReference, AttributeIndividu.ADRESSES);
			}
		}
		for (Long id : batch) {
			if (status.interrupted()) {
				break;
			}
			traiteSourcier(id, dateReference, conjointAIgnorer);
		}
	}

	private void traiteSourcier(Long id, RegDate dateReference, HashSet<Long> conjointAIgnorer) {
		PersonnePhysique sourcier = hibernateTemplate.get(PersonnePhysique.class, id);
		++rapport.nbSourciersTotal;
		// traitement du sourcier
		try {
			traiteSourcier(sourcier, dateReference, conjointAIgnorer);
		}
		catch (PassageNouveauxRentiersSourciersEnMixteException e) {
			rapport.addPassageNouveauxRentiersSourciersEnMixteException(e);
		}
		catch (Exception e) {
			LOGGER.error("Erreur inconnue en traitant le sourcier n° " + sourcier.getNumero(), e);
			rapport.addUnknownException(id,e);
		}
	}

	private void traiteSourcier(PersonnePhysique sourcier, RegDate dateReference, HashSet<Long> conjointAIgnorer) throws PassageNouveauxRentiersSourciersEnMixteException {
		LOGGER.debug("Traitement du sourcier n° " + sourcier.getNumero());

		if (conjointAIgnorer.contains(sourcier.getNumero()) || conjointAIgnorerGlobal.contains(sourcier.getNumero())) {
			// le for du sourcier à deja été modifié par l'intermediaire de son conjoint --> Rien à faire
			rapport.nbSourciersConjointsIgnores++;
			LOGGER.info(String.format("Le sourcier [%s] fait parti d'un couple déjà mixte -> ignoré", sourcier.getNumero()));
			return;
		}

		final ValidationResults vr = validationService.validate(sourcier);

		if (vr.hasErrors()) {
			throw new PassageNouveauxRentiersSourciersEnMixteException(
					sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.VALIDATION, vr.toString());
		}

		final SourcierData data = new SourcierData();

		fillDatesNaissanceDecesEtSexe(sourcier, data);

		if (data.getDateNaissance() == null) {
			LOGGER.info(String.format("La date de naissance du sourcier [%s] n'est pas determinable, impossible de calculé son age de rentier", sourcier.getNumero()));
			throw new PassageNouveauxRentiersSourciersEnMixteException(sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.DATE_NAISSANCE_NULLE);
		}

		if (data.getSexe() == null) {
			LOGGER.info(String.format("Le sexe du sourcier [%s] n'est pas determinable, impossible de calculé son age de rentier", sourcier.getNumero()));
			throw new PassageNouveauxRentiersSourciersEnMixteException(sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.SEXE_NUL);
		}

		// On cache la date de naissance, et le sexe dans le fiscal de manière à restreindre le nombre d'habitants traités la
		// prochaine fois que le batch est lancé.
		// TODO FRED probleme si la donnée est corrigée dans le civil, cette donnée devient obsolète et n'est pas remise à zéro ou remise à jour...  meme cas de figure pour le batch de majoration
		if (sourcier.getDateNaissance() == null) {
			sourcier.setDateNaissance(data.getDateNaissance());
		}
		if (sourcier.getSexe() == null) {
			sourcier.setSexe(data.getSexe());
		}

		// L’individu doit avoir atteint l'âge être rentier à la date du traitement
		if (!isAgeRentier(dateReference, data.getDateNaissance(), data.getSexe())) {
			// l'individu n'a pas encore atteint l'age d'etre rentier
			rapport.nbSourciersTropJeunes++;
			LOGGER.info(String.format("Le sourcier [%s] est trop jeune -> ignoré", sourcier.getNumero()));
			return;
		}

		// On determine si on poursuit avec le contribuable PersonnePhysique ou son eventuel MenageCommun
		final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(sourcier, dateReference);
		final Contribuable contribuable;
		final PersonnePhysique conjoint;
		if (etc != null) {
			data.setMenage(true);
			contribuable = etc.getMenage();
			conjoint = etc.getConjoint(sourcier);
			if (conjoint != null) {
				data.setDateNaissanceConjoint(tiersService.getDateNaissance(conjoint));
				data.setSexeConjoint(tiersService.getSexe(conjoint));
			}
		} else {
			data.setMenage(false);
			conjoint = null;
			contribuable = sourcier;
		}
		fillInfoDomicile(contribuable, data, dateReference);

		if (!data.isDomicilieEnSuisse()) {
			rapport.nbSourciersHorsSuisse ++;
			LOGGER.info(String.format("Le contribuable [%s] n'est pas domicilié en suisse -> ignoré", contribuable.getNumero()));
			return;
		}
    	final RegDate dateRentier = data.getDateRentier();

		// Vérification de cohérence sur le for principal
		final ForFiscalPrincipal dernierForFiscalPrincipal = contribuable.getDernierForFiscalPrincipal();
		if (dernierForFiscalPrincipal == null || dernierForFiscalPrincipal.getDateFin() != null || dernierForFiscalPrincipal.getModeImposition() != ModeImposition.SOURCE) {
			if (data.isMenage()) {
				final String message = String.format(
						"Le ménage commun [%s] associé à la personne [%s] n'a pas de for sourcier ouvert. son dernier for est [%s]",
						contribuable.getNumero(), sourcier.getNumero(), dernierForFiscalPrincipal);
				LOGGER.info(message);
				throw new PassageNouveauxRentiersSourciersEnMixteException(
						sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.INCOHERENCE_FOR_FISCAL,
						message);
			} else {
				final String message = String.format(
						"Le contribuable [%s] n'a pas de for sourcier ouvert. son dernier for est [%s]",
						contribuable.getNumero(), dernierForFiscalPrincipal);
				LOGGER.info(message);
				throw new PassageNouveauxRentiersSourciersEnMixteException(
						sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.INCOHERENCE_FOR_FISCAL,
						message);
			}
		}

		if (dernierForFiscalPrincipal.getDateDebut().isAfterOrEqual(dateRentier)) {
			final String message = String.format(
					"Le contribuable [%s] a un for sourcier [%s] qui a été ouvert après sa date de rentier. Traitement automatique impossible",
					contribuable.getNumero(), dernierForFiscalPrincipal);
			LOGGER.info(message);
			throw new PassageNouveauxRentiersSourciersEnMixteException(
					sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.FOR_FISCAL_POSTERIEUR,
					message);
		}

		/*
		 * Ouverture du nouveau for mixte
		 */
		tiersService.closeForFiscalPrincipal(contribuable, dateRentier.getOneDayBefore(), MotifFor.CHGT_MODE_IMPOSITION);
		tiersService.openForFiscalPrincipal(contribuable, dateRentier, dernierForFiscalPrincipal.getMotifRattachement(), dernierForFiscalPrincipal.getNumeroOfsAutoriteFiscale(),
				dernierForFiscalPrincipal.getTypeAutoriteFiscale(), ModeImposition.MIXTE_137_1, MotifFor.CHGT_MODE_IMPOSITION, false);
		LOGGER.info("ouverture du for mixte pour le contribuable n° " + contribuable.getNumero());
		rapport.addSourcierConverti(contribuable.getNumero());
		if (data.isMenage() && conjoint != null) {
			conjointAIgnorer.add(conjoint.getNumero());
		}
	}

	private void fillDatesNaissanceDecesEtSexe(PersonnePhysique sourcier, final SourcierData data) throws PassageNouveauxRentiersSourciersEnMixteException {
		try {
			data.setSexe(tiersService.getSexe(sourcier));
			data.setDateNaissance(tiersService.getDateNaissance(sourcier));
		}
		catch (ServiceCivilException e) {
			LOGGER.error("Impossible de récupérer l'individu n° " + sourcier.getNumero(), e);
			throw new PassageNouveauxRentiersSourciersEnMixteException(sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.CIVIL_EXCEPTION, e);
		} catch (IndividuNotFoundException e ) {
			LOGGER.info("L'individu n° " + sourcier.getNumeroIndividu() + " associé à la personne n° "	+ sourcier.getNumero() + " n'existe pas");
			throw new PassageNouveauxRentiersSourciersEnMixteException(sourcier, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.INDIVIDU_INCONNU, "individu associé n°" + sourcier.getNumeroIndividu());
		}
	}

	private void fillInfoDomicile(Contribuable ctb, final SourcierData data, RegDate dateReference) throws PassageNouveauxRentiersSourciersEnMixteException {

		final AdresseGenerique adresseDomicile;
		try {
			adresseDomicile = adresseService.getAdresseFiscale(ctb, TypeAdresseFiscale.DOMICILE, dateReference, false);
		}
		catch (AdresseException e) {
			LOGGER.error("Erreur dans les adresse de la personne n° " + ctb.getNumero(), e);
			throw new PassageNouveauxRentiersSourciersEnMixteException(ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.ADRESSE_EXCEPTION, e);
		}

		if (adresseDomicile == null) {
			final String message = "Impossibilité d'identifier le domicile du contribuable n° " + ctb.getNumero() + " car il ne possède pas d'adresse.";
			LOGGER.info(message);
			throw new PassageNouveauxRentiersSourciersEnMixteException(ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.DOMICILE_INCONNU, message);
		}

		if (adresseDomicile.isDefault()) {
			// msi/tdq 3.6.09 : on ne doit pas tenir compte des adresses de domicile par défaut car elles n'ont pas de valeur pour
			// déterminer si un contribuable est dans le canton
			String message = "Impossibilité d'identifier le domicile du contribuable n° " + ctb.getNumero()
					+ " car son adresse de domicile est une valeur par défaut.";
			LOGGER.info(message);
			throw new PassageNouveauxRentiersSourciersEnMixteException(ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.DOMICILE_INCONNU, message);
		}

		boolean estDomicilieEnSuisse;
		final Commune commune;
		try {
			estDomicilieEnSuisse = serviceInfra.estEnSuisse(adresseDomicile);
			if (estDomicilieEnSuisse) {
				commune = serviceInfra.getCommuneByAdresse(adresseDomicile, dateReference);
			}
			else {
				commune = null;
			}
		}
		catch (ServiceInfrastructureException e) {
			LOGGER.error("Impossible de récupérer la commune de l n° " + ctb.getNumero(), e);
			throw new PassageNouveauxRentiersSourciersEnMixteException(ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.INFRA_EXCEPTION, e);
		}

		if (estDomicilieEnSuisse) {
			if (commune == null) {
				String message = "Impossibilité d'identifier le domicile du contribuable n° " + ctb.getNumero()
						+ " car la localité avec le numéro postal " + adresseDomicile.getNumeroOrdrePostal() + " est inconnue.";
				LOGGER.info(message);
				throw new PassageNouveauxRentiersSourciersEnMixteException(ctb, PassageNouveauxRentiersSourciersEnMixteResults.ErreurType.DOMICILE_INCONNU, message);
			}
		}
		data.setDomicilieEnSuisse(estDomicilieEnSuisse);
	}

	final private static String QUERY_SOURCIERS = // Requete retrouvant les personnes imposées à la source ou faisant parti d'un ménage imposé à la source
			"SELECT pp.id                                                                                 "
				+ "FROM                                                                                   "
				+ "    PersonnePhysique AS pp                                                             "
				+ "WHERE                                                                                  "
				+ "	   pp.annulationDate IS null                                                          "
				+ "	   AND pp.dateDeces IS null                                                           "
				+ "	   AND (pp.dateNaissance IS null                                                      "
				+ "      OR pp.dateNaissance <= :pivot AND pp.sexe IS null                                "
				+ "      OR pp.dateNaissance <= :pivotHomme AND pp.sexe = '" + Sexe.MASCULIN+"'           "
				+ "      OR pp.dateNaissance <= :pivotFemme AND pp.sexe = '" + Sexe.FEMININ +"')          "
				+ "    AND ( EXISTS (                                                                     "
				+ "        SELECT                                                                         "
				+ "            fors.id                                                                    "
				+ "        FROM                                                                           "
				+ "            ForFiscalPrincipal AS fors                                                 "
				+ "        WHERE                                                                          "
				+ "            fors.annulationDate IS null                                                "
				+ "            AND fors.tiers.id = pp.id                                                  "
				+ "            AND fors.dateDebut <= :date                                                "
				+ "            AND (fors.dateFin Is null OR fors.dateFin >= :date)                        "
				+ "            AND fors.modeImposition = '" + ModeImposition.SOURCE + "')                 "
				+ "    OR EXISTS (                                                                        "
				+ "        SELECT                                                                         "
				+ "            rap.id                                                                     "
				+ "        FROM                                                                           "
				+ "            RapportEntreTiers AS rap                                                   "
				+ "        WHERE                                                                          "
				+ "            rap.annulationDate IS null                                                 "
				+ "            AND rap.sujetId = pp.id                                                    "
				+ "            AND rap.class = AppartenanceMenage                                         "
				+ "            AND rap.dateDebut <= :date                                                 "
				+ "            AND (rap.dateFin IS null OR rap.dateFin >= :date)                          "
				+ "            AND EXISTS (                                                               "
				+ "                 SELECT                                                                "
				+ "                     fors2.id                                                          "
				+ "                 FROM                                                                  "
				+ "                     ForFiscalPrincipal AS fors2                                       "
				+ "                 WHERE                                                                 "
				+ "                     fors2.annulationDate IS null                                      "
				+ "                     AND fors2.tiers.id = rap.objetId                                  "
				+ "                     AND fors2.dateDebut <= :date                                      "
				+ "                     AND (fors2.dateFin Is null OR fors2.dateFin >= :date)             "
				+ "                     AND fors2.modeImposition = '" + ModeImposition.SOURCE + "')))     "
				+ "ORDER BY pp.id ASC                                                                     ";

	private List<Long> getListPotentielsNouveauxRentiersSourciers(final RegDate date) {
		final RegDate datePivotHomme = date.addYears(-ageRentierHomme);
		final RegDate datePivotFemme = date.addYears(-ageRentierFemme);
		final RegDate datePivotLaPlusAncienne = datePivotHomme.isBefore(datePivotFemme) ? datePivotHomme : datePivotFemme;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final Query queryObject = session.createQuery(QUERY_SOURCIERS);
						queryObject.setParameter("pivot", datePivotLaPlusAncienne.index());
						queryObject.setParameter("pivotHomme", datePivotHomme.index());
						queryObject.setParameter("pivotFemme", datePivotFemme.index());
						queryObject.setParameter("date", date.index());
						//noinspection unchecked
						return queryObject.list();
					}
				});
			}
		});
	}

	private boolean isAgeRentier(RegDate dateReference, RegDate dateNaissance, Sexe sexe) {
		return dateReference.isAfterOrEqual(calculeDateRentier(dateNaissance, sexe));
	}

	private RegDate calculeDateRentier(RegDate dateNaissance, Sexe sexe) {
		final int age;
		switch (sexe) {
			case MASCULIN: age = ageRentierHomme; break;
			case FEMININ:  age = ageRentierFemme; break;
		default: throw new RuntimeException(sexe + " unknown");
		}
		return dateNaissance.addYears(age);
	}


	private class SourcierData {

		private RegDate dateNaissance;
		private RegDate dateNaissanceConjoint;
		private Sexe sexe;
		private Sexe sexeConjoint;
		private boolean menage;
		private boolean domicilieEnSuisse;

		public RegDate getDateNaissance() {
			return dateNaissance;
		}

		public void setDateNaissance(RegDate dateNaissance) {
			this.dateNaissance = dateNaissance;
		}

		public RegDate getDateNaissanceConjoint() {
			return dateNaissanceConjoint;
		}

		public void setDateNaissanceConjoint(RegDate dateNaissance) {
			this.dateNaissanceConjoint = dateNaissance;
		}

		public Sexe getSexe() {
			return sexe;
		}

		public void setSexe(Sexe sexe) {
			this.sexe = sexe;
		}

		public Sexe getSexeConjoint() {
			return sexeConjoint;
		}


		public void setSexeConjoint(Sexe sexe) {
			this.sexeConjoint = sexe;
		}

		public boolean isDomicilieEnSuisse() {
			return domicilieEnSuisse;
		}

		public void setDomicilieEnSuisse(boolean estDomicilieEnSuisse) {
			this.domicilieEnSuisse = estDomicilieEnSuisse;
		}

		public void setMenage(boolean menage) {
			this.menage = menage;
		}

		public boolean  isMenage() {
			return menage;
		}

		public RegDate getDateRentier() {
			if (getDateNaissance() == null || getSexe() == null) {
				throw new IllegalStateException("Impossible d'appeler calculeDateRentier() si la date de naissance ou le sexe sont null");
			}
			RegDate dateRentier = calculeDateRentier(getDateNaissance(), getSexe());
			if (isMenage() && getDateNaissanceConjoint() != null && getSexeConjoint() != null) {
				RegDate dateRentierConjoint = calculeDateRentier(getDateNaissanceConjoint(), getSexeConjoint());
				dateRentier = dateRentier.isBefore(dateRentierConjoint) ? dateRentier : dateRentierConjoint;
			}
			return dateRentier;
		}
	}
}
