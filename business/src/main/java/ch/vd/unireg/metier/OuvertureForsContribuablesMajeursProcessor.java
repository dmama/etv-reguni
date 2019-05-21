package ch.vd.unireg.metier;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.hibernate.query.Query;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.OuvertureForsResults.ErreurType;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersException;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Processor qui rechercher les habitants nouvellement majeurs et qui ouvre les fors principaux.
 */
public class OuvertureForsContribuablesMajeursProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(OuvertureForsContribuablesMajeursProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	protected final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final TiersDAO tiersDAO;
	private final AdresseService adresseService;
	private final ServiceInfrastructureService serviceInfra;
	private final ServiceCivilCacheWarmer serviceCivilCacheWarmer;
	private final ValidationService validationService;

	public OuvertureForsContribuablesMajeursProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, TiersDAO tiersDAO, TiersService tiersService,
	                                                  AdresseService adresseService, ServiceInfrastructureService serviceInfra, ServiceCivilCacheWarmer serviceCivilCacheWarmer,
	                                                  ValidationService validationService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersDAO = tiersDAO;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.serviceInfra = serviceInfra;
		this.serviceCivilCacheWarmer = serviceCivilCacheWarmer;
		this.validationService = validationService;
	}

	/**
	 * Exécute le traitement du processeur à la date de référence spécifiée.
	 */
	public OuvertureForsResults run(final RegDate dateReference, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final OuvertureForsResults rapportFinal = new OuvertureForsResults(dateReference, tiersService, adresseService);

		// boucle principale sur les habitants à traiter
		final List<Long> list = getListHabitantsSansForPrincipal(dateReference);

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, OuvertureForsResults> template = new BatchTransactionTemplateWithResults<>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, OuvertureForsResults>() {

			@Override
			public OuvertureForsResults createSubRapport() {
				return new OuvertureForsResults(dateReference, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, OuvertureForsResults r) throws Exception {
				traiteBatch(batch, dateReference, s, r);
				return !s.isInterrupted();
			}

			@Override
			public void afterTransactionCommit() {
				final int[] decomptes = new int[OuvertureForsResults.IgnoreType.values().length];
				rapportFinal.contribuablesIgnores.stream()
						.map(OuvertureForsResults.Ignore::getRaison)
						.map(Enum::ordinal)
						.forEach(ordinal -> ++decomptes[ordinal]);

				s.setMessage(String.format("%d habitants traités sur %d [fors ouverts=%d, mineurs=%d, décédés=%d, horsVD=%d, en erreur=%d]",
				                           rapportFinal.nbHabitantsTotal,
				                           list.size(),
				                           rapportFinal.habitantTraites.size(),
				                           decomptes[OuvertureForsResults.IgnoreType.MINEUR.ordinal()],
				                           decomptes[OuvertureForsResults.IgnoreType.DECEDE.ordinal()],
				                           decomptes[OuvertureForsResults.IgnoreType.HORS_VD.ordinal()],
				                           rapportFinal.habitantEnErrors.size()),
				             progressMonitor.getProgressInPercent());
			}
		}, progressMonitor);

		if (status.isInterrupted()) {
			status.setMessage("L'ouverture des fors des contribuables majeurs a été interrompue."
					+ " Nombre d'habitants traités au moment de l'interruption = " + rapportFinal.habitantTraites.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'ouverture des fors des contribuables majeurs est terminée." + " Nombre d'habitants traités = "
					+ rapportFinal.habitantTraites.size() + ". Nombre d'erreurs = " + rapportFinal.habitantEnErrors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<Long> batch, RegDate dateReference, StatusManager status, OuvertureForsResults r) {

		// On préchauffe le cache des individus, si possible
		if (serviceCivilCacheWarmer.isServiceWarmable()) {
			final Set<Long> numeroIndividus = tiersDAO.getNumerosIndividu(batch, false);
			if (!numeroIndividus.isEmpty()) {
				serviceCivilCacheWarmer.warmIndividus(numeroIndividus, dateReference, AttributeIndividu.PERMIS, AttributeIndividu.NATIONALITES, AttributeIndividu.PARENTS);
				serviceCivilCacheWarmer.warmIndividus(numeroIndividus, null, AttributeIndividu.ADRESSES);
			}
		}

		for (Long id : batch) {
			if (status.isInterrupted()) {
				break;
			}
			traiteHabitant(id, dateReference, r);
		}
	}

	protected void traiteHabitant(Long id, RegDate dateReference, OuvertureForsResults r) {

		PersonnePhysique habitant = hibernateTemplate.get(PersonnePhysique.class, id);

		++r.nbHabitantsTotal;

		// traitement de l'habitant
		try {
			traiteHabitant(habitant, dateReference, r);
		}
		catch (OuvertureForsIgnoreException e) {
			r.addContribuableIgnore(habitant, e.getRaison(), e.getDetails());
		}
		catch (OuvertureForsErreurException e) {
			r.addOuvertureForsException(e);
		}
		catch (Exception e) {
			LOGGER.error("Erreur inconnue en traitant l'habitant n° " + habitant.getNumero(), e);
			r.addUnknownException(habitant, e);
		}
	}

	private static class HabitantData {

		private Individu individu;

		private RegDate dateNaissance;
		private RegDate dateDeces;

		private boolean domicilieDansLeCanton;
		private Integer numeroOfsAutoriteFiscale;

		private TypeEtatCivil etatCivil;
		private boolean hasPermisC;
		private boolean hasNationaliteSuisse;

		public Individu getIndividu() {
			return individu;
		}

		public void setIndividu(Individu individu) {
			this.individu = individu;
		}

		public RegDate getDateNaissance() {
			return dateNaissance;
		}

		public void setDateNaissance(RegDate dateNaissance) {
			this.dateNaissance = dateNaissance;
		}

		public RegDate getDateDeces() {
			return dateDeces;
		}

		public void setDateDeces(RegDate dateDeces) {
			this.dateDeces = dateDeces;
		}

		public boolean isDomicilieDansLeCanton() {
			return domicilieDansLeCanton;
		}

		public void setDomicilieDansLeCanton(boolean domicilieDansLeCanton) {
			this.domicilieDansLeCanton = domicilieDansLeCanton;
		}

		public Integer getNumeroOfsAutoriteFiscale() {
			return numeroOfsAutoriteFiscale;
		}

		public void setNumeroOfsAutoriteFiscale(Integer numeroOfsAutoriteFiscale) {
			this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
		}

		public TypeEtatCivil getEtatCivil() {
			return etatCivil;
		}

		public void setEtatCivil(TypeEtatCivil etatCivil) {
			this.etatCivil = etatCivil;
		}

		public boolean isHasPermisC() {
			return hasPermisC;
		}

		public void setHasPermisC(boolean hasPermisC) {
			this.hasPermisC = hasPermisC;
		}

		public boolean isHasNationaliteSuisse() {
			return hasNationaliteSuisse;
		}

		public void setHasNationaliteSuisse(boolean hasNationaliteSuisse) {
			this.hasNationaliteSuisse = hasNationaliteSuisse;
		}
	}

	private void traiteHabitant(PersonnePhysique habitant, RegDate dateReference, OuvertureForsResults r) throws OuvertureForsErreurException, OuvertureForsIgnoreException {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Traitement de l'habitant n° " + habitant.getNumero());
		}

		final ValidationResults vr = validationService.validate(habitant);
		if (vr.hasErrors()) {
			throw new OuvertureForsErreurException(habitant, ErreurType.VALIDATION, vr.toString());
		}

		final HabitantData data = new HabitantData();

		fillDatesNaissanceEtDeces(habitant, data, dateReference);

		if (data.getDateNaissance() == null) {
			throw new OuvertureForsErreurException(habitant, ErreurType.DATE_NAISSANCE_NULLE);
		}

		// [UNIREG-1114] On cache la date de naissance calculée dans l'habitant, de manière à restreindre le nombre d'habitants traités la
		// prochaine fois que le batch est lancé.
		if (habitant.getDateNaissance() == null) {
			habitant.setDateNaissance(data.getDateNaissance());
		}

		// L’individu doit être majeur à la date du traitement, c’est-à-dire avoir 18 ans révolus
		if (!FiscalDateHelper.isMajeur(dateReference, data.getDateNaissance())) {
			// l'individu est mineur => rien à faire
			throw new OuvertureForsIgnoreException(habitant, OuvertureForsResults.IgnoreType.MINEUR, "Date de naissance : " + RegDateHelper.dateToDisplayString(data.getDateNaissance()));
		}

		if (data.getDateDeces() != null) {
			// l'individu est décédé => rien à faire
			habitant.setMajoriteTraitee(Boolean.TRUE); // à moins d'un résurrection, c'est fini avec celui-là.
			throw new OuvertureForsIgnoreException(habitant, OuvertureForsResults.IgnoreType.DECEDE, "Date de décès : " + RegDateHelper.dateToDisplayString(data.getDateDeces()));
		}

		final RegDate dateMajorite = FiscalDateHelper.getDateMajorite(data.getDateNaissance());
		fillInfoDomicile(habitant, data, dateMajorite);

		if (!data.isDomicilieDansLeCanton()) {
			// l'individu domicilié hors-Canton/hors-Suisse => rien à faire
			habitant.setMajoriteTraitee(Boolean.TRUE); // on ouvrira un for à son arrivée dans le canton, si nécessaire
			throw new OuvertureForsIgnoreException(habitant, OuvertureForsResults.IgnoreType.HORS_VD, null);
		}
		if (data.getNumeroOfsAutoriteFiscale() == null) {
			throw new IllegalArgumentException();
		}

		fillEtatCivilPermisEtNationalite(habitant, data, dateMajorite);

		// Vérification de cohérence sur l'état civil
		final TypeEtatCivil etatCivil = data.getEtatCivil();
		if (etatCivil != null && TypeEtatCivil.CELIBATAIRE != etatCivil) {
			String message = "L'habitant possède un état civil [" + etatCivil + "] différent de CELIBATAIRE :"
					+ " soit il est marié et son ménage commun manque,"
					+ " soit il est veuf/séparé/divorcé et son for principal n'a pas été rouvert correctement."
					+ " Traitement automatique impossible.";
			throw new OuvertureForsErreurException(habitant, ErreurType.INCOHERENCE_ETAT_CIVIL, message);
		}

		// Vérification de cohérence sur le for principal
		final ForFiscalPrincipal dernierForFiscalPrincipal = habitant.getDernierForFiscalPrincipal();
		if (dernierForFiscalPrincipal != null && (dernierForFiscalPrincipal.getDateFin() == null || dernierForFiscalPrincipal.getDateFin().isAfterOrEqual(dateMajorite))) {
			final String message = "L'habitant possède un for [" + dernierForFiscalPrincipal + "] actif après sa date de majorité [" + dateMajorite + "].";
			throw new OuvertureForsIgnoreException(habitant, OuvertureForsResults.IgnoreType.FOR_PRINCIPAL_EXISTANT, message);
		}

		/*
		 * Le for est ouvert le jour de la majorité sur la commune de domicile avec un motif de rattachement « Domicile », un motif
		 * d’ouverture « Majorité » et un mode d’imposition ordinaire si le contribuable est de nationalité suisse ou titulaire d’un permis
		 * d’établissement (permis C) et un mode d’imposition à la source dans le cas contraire
		 */
		final ModeImposition modeImposition;
		if (data.isHasPermisC() || data.isHasNationaliteSuisse()) {
			modeImposition = ModeImposition.ORDINAIRE;
		}
		else {
			modeImposition = ModeImposition.SOURCE;
		}

		// [SIFISC-19003] si l'individu est en fait arrivé de HS dans l'année de sa majorité c'est la date d'arrivée qui doit être utilisée
		// pour l'ouverture du for, et pas la date d'obtention de la majorité
		final MotifFor motifOuverture;
		final RegDate dateOuverture;
		final RegDate dateArriveeHorsSuisseAnneeMajorite = getPremiereDateArriveeHorsSuisseDansAnnee(habitant, dateMajorite.year());
		if (dateArriveeHorsSuisseAnneeMajorite != null) {
			dateOuverture = dateArriveeHorsSuisseAnneeMajorite;
			motifOuverture = MotifFor.ARRIVEE_HS;
		}
		else {
			dateOuverture = dateMajorite;
			motifOuverture = MotifFor.MAJORITE;
		}

		tiersService.openForFiscalPrincipal(habitant, dateOuverture, MotifRattachement.DOMICILE, data.getNumeroOfsAutoriteFiscale(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, modeImposition, motifOuverture);
		habitant.setMajoriteTraitee(Boolean.TRUE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("-> ouverture du for principal pour l'habitant n° " + habitant.getNumero());
		}

		// [UNIREG-1585] on s'assure que l'oid est bien renseigné
		final Integer oid = tiersService.calculateCurrentOfficeID(habitant);
		//On n'assert pas car les sourcier n'ont pour le moment pas de for de gestion
		//Assert.notNull(oid);

		r.addHabitantTraite(habitant, oid, dateOuverture, motifOuverture, modeImposition);
	}

	/**
	 * @param habitant habitant concerné
	 * @param annee année intéressante
	 * @return si elle existe (<code>null</code> sinon), la première date de début d'une adresse principale civile avec provenance hors-Suisse de l'année donnée
	 */
	@Nullable
	private RegDate getPremiereDateArriveeHorsSuisseDansAnnee(PersonnePhysique habitant, int annee) {
		final Individu individu = tiersService.getIndividu(habitant, RegDate.get(annee, 12, 31), AttributeIndividu.ADRESSES);
		if (individu == null) {
			if (habitant.isConnuAuCivil()) {
				throw new IndividuNotFoundException(habitant);
			}
			// même pas habitant civil, je n'ai aucune idée de quand (ni si) cette personne a pu arriver de HS
			return null;
		}

		return individu.getAdresses().stream()
				.filter(adresse -> adresse.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE)
				.filter(adresse -> adresse.getDateDebut() != null && adresse.getDateDebut().year() == annee)
				.filter(adresse -> adresse.getLocalisationPrecedente() != null && adresse.getLocalisationPrecedente().getType() == LocalisationType.HORS_SUISSE)
				.map(Adresse::getDateDebut)
				.min(Comparator.naturalOrder())
				.orElse(null);
	}

	private void fillDatesNaissanceEtDeces(PersonnePhysique habitant, final HabitantData data, RegDate dateReference) throws OuvertureForsErreurException {

		// on fait appel à host-interface
		Individu individu;
		try {
			individu = tiersService.getIndividu(habitant, dateReference, AttributeIndividu.PERMIS, AttributeIndividu.NATIONALITES);
		}
		catch (IndividuConnectorException e) {
			LOGGER.error("Impossible de récupérer l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsErreurException(habitant, ErreurType.CIVIL_EXCEPTION, e);
		}

		if (individu == null) {
			LOGGER.error("Impossible de récupérer l'individu n° " + habitant.getNumeroIndividu() + " associé à l'habitant n° "
					+ habitant.getNumero());
			throw new OuvertureForsErreurException(habitant, ErreurType.INDIVIDU_INCONNU, "individu associé n°" + habitant.getNumeroIndividu());
		}

		RegDate dateNaissance = individu.getDateNaissance();
		RegDate dateDeces = individu.getDateDeces();

		data.setIndividu(individu);
		data.setDateNaissance(dateNaissance);
		data.setDateDeces(dateDeces);
	}

	private void fillInfoDomicile(PersonnePhysique habitant, final HabitantData data, RegDate dateReference) throws OuvertureForsErreurException, OuvertureForsIgnoreException {

		final AdresseGenerique adresseDomicile;
		try {
			adresseDomicile = adresseService.getAdresseFiscale(habitant, TypeAdresseFiscale.DOMICILE, dateReference, false);
		}
		catch (AdresseException e) {
			LOGGER.error("Erreur dans les adresse de l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsErreurException(habitant, ErreurType.ADRESSE_EXCEPTION, e);
		}

		if (adresseDomicile == null) {
			throw new OuvertureForsIgnoreException(habitant, OuvertureForsResults.IgnoreType.AUCUNE_ADRESSE, null);
		}

		if (adresseDomicile.isDefault()) {
			// msi/tdq 3.6.09 : on ne doit pas tenir compte des adresses de domicile par défaut car elles n'ont pas de valeur pour
			// déterminer si un contribuable est dans le canton
			throw new OuvertureForsIgnoreException(habitant, OuvertureForsResults.IgnoreType.ADRESSE_DOMICILE_EST_DEFAUT, null);
		}

		boolean estDomicilieDansLeCanton;
		final Commune commune;
		try {
			estDomicilieDansLeCanton = serviceInfra.estDansLeCanton(adresseDomicile);
			if (estDomicilieDansLeCanton) {
				commune = serviceInfra.getCommuneByAdresse(adresseDomicile, dateReference);
			}
			else {
				commune = null;
			}
		}
		catch (ServiceInfrastructureException e) {
			LOGGER.error("Impossible de récupérer la commune de l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsErreurException(habitant, ErreurType.INFRA_EXCEPTION, e);
		}

		if (estDomicilieDansLeCanton) {
			if (commune == null) {
				final String message = "Impossibilité d'identifier le domicile du contribuable n° " + habitant.getNumero()
						+ " car la localité avec le numéro postal " + adresseDomicile.getNumeroOrdrePostal() + " est inconnue.";
				throw new OuvertureForsErreurException(habitant, ErreurType.DOMICILE_INCONNU, message);
			}
		}

		data.setDomicilieDansLeCanton(estDomicilieDansLeCanton);
		data.setNumeroOfsAutoriteFiscale(commune == null ? null : commune.getNoOFS());
	}

	private void fillEtatCivilPermisEtNationalite(PersonnePhysique habitant, final HabitantData data, RegDate dateReference) throws OuvertureForsErreurException {

		final Individu individu = data.getIndividu();
		if (individu == null) {
			LOGGER.error("Impossible de récupérer l'individu n° " + habitant.getNumeroIndividu() + " associé à l'habitant n° "
					+ habitant.getNumero());
			throw new OuvertureForsErreurException(habitant, ErreurType.INDIVIDU_INCONNU, "individu associé n°" + habitant.getNumeroIndividu());
		}

		final EtatCivil etatCivil = individu.getEtatCivil(dateReference);
		if (etatCivil != null) {
			data.setEtatCivil(etatCivil.getTypeEtatCivil());
		}

		final boolean hasPermisC = tiersService.isAvecPermisC(individu, dateReference);
		final boolean hasNationaliteSuisse;
		try {
			hasNationaliteSuisse = tiersService.isSuisse(individu, dateReference);
		}
		catch (TiersException e) {
			LOGGER.error("Impossible de récupérer la nationalité de l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsErreurException(habitant, ErreurType.CIVIL_EXCEPTION, e);
		}

		data.setHasPermisC(hasPermisC);
		data.setHasNationaliteSuisse(hasNationaliteSuisse);
	}

	private static final String queryHabitantWithoutFor = // --------------------------------
	"SELECT hab.id                                                                 "
			+ "FROM                                                                "
			+ "    PersonnePhysique AS hab                                         "
			+ "WHERE                                                               "
			+ "    hab.habitant = true                                             "
			+ "	   AND hab.annulationDate IS null                                  "
			+ "	   AND hab.dateDeces IS null                                       "
			+ "	   AND (hab.majoriteTraitee IS null OR hab.majoriteTraitee = false)"
			+ "	   AND (hab.dateNaissance IS null OR hab.dateNaissance <= :pivot)  "
			+ "    AND NOT EXISTS (                                                "
			+ "        SELECT                                                      "
			+ "            fors.id                                                 "
			+ "        FROM                                                        "
			+ "            ForFiscalPrincipalPP AS fors                              "
			+ "        WHERE                                                       "
			+ "            fors.annulationDate IS null                             "
			+ "            AND fors.tiers.id = hab.id                              "
			+ "            AND fors.dateDebut <= :date                             "
			+ "            AND (fors.dateFin Is null OR fors.dateFin >= :date)     "
			+ "        )                                                           "
			+ "    AND NOT EXISTS (                                                "
			+ "        SELECT                                                      "
			+ "            rap.id                                                  "
			+ "        FROM                                                        "
			+ "            RapportEntreTiers AS rap                                "
			+ "        WHERE                                                       "
			+ "            rap.annulationDate IS null                              "
			+ "            AND rap.sujetId = hab.id                                "
			+ "            AND type(rap) = AppartenanceMenage                      "
			+ "            AND rap.dateDebut <= :date                              "
			+ "            AND (rap.dateFin IS null OR rap.dateFin >= :date)       "
			+ "        )                                                           "
			+ "ORDER BY hab.id ASC ";

	/**
	 * Retourne la liste des ids des habitants :
	 * <ul>
	 * <li>n'ayant pas de for principal ouvert à la date spécifiée</li>
	 * <li>ne faisant pas partie d'un ménage-commun</li>
	 * <li>étant majeur ou dont la naissance de naissance est inconnue</li>
	 * </ul>
	 * ... à la date spécifiée.
	 *
	 * @param date
	 *            la date spécifiée
	 * @return une liste des ids des habitants trouvés
	 */
	protected List<Long> getListHabitantsSansForPrincipal(final RegDate date) {

		final RegDate datePivot = date.addYears(-FiscalDateHelper.AGE_MAJORITE);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(status -> hibernateTemplate.execute(session -> {
			final Query queryObject = session.createQuery(queryHabitantWithoutFor);
			queryObject.setParameter("pivot", datePivot);
			queryObject.setParameter("date", date);
			//noinspection unchecked
			return (List<Long>) queryObject.list();
		}));
	}
}
