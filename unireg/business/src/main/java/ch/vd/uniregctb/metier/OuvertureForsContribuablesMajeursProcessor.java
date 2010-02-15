package ch.vd.uniregctb.metier;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseException;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.OuvertureForsResults.ErreurType;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Processor qui rechercher les habitants nouvellement majeurs et qui ouvre les fors principaux.
 */
public class OuvertureForsContribuablesMajeursProcessor {

	private final Logger LOGGER = Logger.getLogger(OuvertureForsContribuablesMajeursProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final ServiceInfrastructureService serviceInfra;
	private final GlobalTiersSearcher searcher;

	protected OuvertureForsResults rapport;

	public OuvertureForsContribuablesMajeursProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
			TiersService tiersService, AdresseService adresseService, ServiceInfrastructureService serviceInfra,
			GlobalTiersSearcher searcher) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.serviceInfra = serviceInfra;
		this.searcher = searcher;
	}

	/**
	 * Exécute le traitement du processeur à la date de référence spécifiée.
	 */
	public OuvertureForsResults run(final RegDate dateReference, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final OuvertureForsResults rapportFinal = new OuvertureForsResults(dateReference);

		// boucle principale sur les habitants à traiter
		final List<Long> list = getListHabitantsSansForPrincipal(dateReference);

		final BatchTransactionTemplate<Long, OuvertureForsResults> template = new BatchTransactionTemplate<Long, OuvertureForsResults>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, s, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, OuvertureForsResults>() {

			@Override
			public OuvertureForsResults createSubRapport() {
				return new OuvertureForsResults(dateReference);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, OuvertureForsResults r) throws Exception {
				rapport = r;
				traiteBatch(batch, dateReference, s);
				return !s.interrupted();
			}

			@Override
			public void afterTransactionCommit() {
				int percent = (100 * rapportFinal.nbHabitantsTotal) / list.size();
				s.setMessage(String.format(
						"%d habitants traités sur %d [fors ouverts=%d, mineurs=%d, décédés=%d, horsVD=%d, en erreur=%d]",
						rapportFinal.nbHabitantsTotal, list.size(), rapportFinal.habitantTraites.size(), rapportFinal.nbHabitantsMineurs,
						rapportFinal.nbHabitantsDecedes, rapportFinal.nbHabitantsHorsVD, rapportFinal.habitantEnErrors.size()), percent);
			}
		});

		if (status.interrupted()) {
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

	private void traiteBatch(List<Long> batch, RegDate dateReference, StatusManager status) {
		for (Long id : batch) {
			if (status.interrupted()) {
				break;
			}
			traiteHabitant(id, dateReference);
		}
	}

	protected void traiteHabitant(Long id, RegDate dateReference) {

		PersonnePhysique habitant = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);

		++rapport.nbHabitantsTotal;

		// traitement de l'habitant
		try {
			traiteHabitant(habitant, dateReference);
		}
		catch (OuvertureForsException e) {
			rapport.addOuvertureForsException(e);
		}
		catch (Exception e) {
			LOGGER.error("Erreur inconnue en traitant l'habitant n° " + habitant.getNumero(), e);
			rapport.addUnknownException(habitant, e);
		}
	}

	private static class HabitantData {

		private TiersIndexedData indexerData;
		private Individu individu;

		private RegDate dateNaissance;
		private RegDate dateDeces;

		private boolean domicilieDansLeCanton;
		private Integer numeroOfsAutoriteFiscale;

		private EnumTypeEtatCivil etatCivil;
		private boolean hasPermisC;
		private boolean hasNationaliteSuisse;

		public TiersIndexedData getIndexerData() {
			return indexerData;
		}

		public void setIndexerData(TiersIndexedData indexerData) {
			this.indexerData = indexerData;
		}

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

		public EnumTypeEtatCivil getEtatCivil() {
			return etatCivil;
		}

		public void setEtatCivil(EnumTypeEtatCivil etatCivil) {
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

	private void traiteHabitant(PersonnePhysique habitant, RegDate dateReference) throws OuvertureForsException {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Traitement de l'habitant n° " + habitant.getNumero());
		}

		final ValidationResults vr = habitant.validate();
		if (vr.hasErrors()) {
			throw new OuvertureForsException(habitant, ErreurType.VALIDATION, vr.toString());
		}

		final HabitantData data = new HabitantData();

		fillDatesNaissanceEtDeces(habitant, data, dateReference);

		if (data.getDateNaissance() == null) {
			throw new OuvertureForsException(habitant, ErreurType.DATE_NAISSANCE_NULLE);
		}

		// [UNIREG-1114] On cache la date de naissance calculée dans l'habitant, de manière à restreindre le nombre d'habitants traités la
		// prochaine fois que le batch est lancé.
		if (habitant.getDateNaissance() == null) {
			habitant.setDateNaissance(data.getDateNaissance());
		}

		// L’individu doit être majeur à la date du traitement, c’est-à-dire avoir 18 ans révolus
		if (!FiscalDateHelper.isMajeur(dateReference, data.getDateNaissance())) {
			// l'individu est mineur => rien à faire
			rapport.nbHabitantsMineurs++;
			return;
		}

		if (data.getDateDeces() != null) {
			// l'individu est décédé => rien à faire
			habitant.setMajoriteTraitee(Boolean.TRUE); // à moins d'un résurrection, c'est fini avec celui-là.
			rapport.nbHabitantsDecedes++;
			return;
		}

		fillInfoDomicile(habitant, data, dateReference);

		if (!data.isDomicilieDansLeCanton()) {
			// l'individu domicilié hors-Canton/hors-Suisse => rien à faire
			habitant.setMajoriteTraitee(Boolean.TRUE); // on ouvrira un for à son arrivée dans le canton, si nécessaire
			rapport.nbHabitantsHorsVD++;
			return;
		}
		Assert.notNull(data.getNumeroOfsAutoriteFiscale());

		fillEtatCivilPermisEtNationalite(habitant, data, dateReference);

		// Vérification de cohérence sur l'état civil
		final EnumTypeEtatCivil etatCivil = data.getEtatCivil();
		if (etatCivil != null && !EnumTypeEtatCivil.CELIBATAIRE.equals(etatCivil)) {
			String message = "L'habitant possède un état civil [" + etatCivil.getName() + "] différent de CELIBATAIRE :"
					+ " soit il est marié et son ménage commun manque,"
					+ " soit il est veuf/séparé/divorcé et son for principal n'a pas été rouvert correctement."
					+ " Traitement automatique impossible.";
			throw new OuvertureForsException(habitant, ErreurType.INCOHERENCE_ETAT_CIVIL, message);
		}

		final RegDate dateMajorite = FiscalDateHelper.getDateMajorite(data.getDateNaissance());

		// Vérification de cohérence sur le for principal
		final ForFiscalPrincipal dernierForFiscalPrincipal = habitant.getDernierForFiscalPrincipal();
		if (dernierForFiscalPrincipal != null
				&& (dernierForFiscalPrincipal.getDateFin() == null || dernierForFiscalPrincipal.getDateFin().isAfterOrEqual(dateMajorite))) {
			String message = "L'habitant possède un for [" + dernierForFiscalPrincipal + "] actif après sa date de majorité ["
					+ dateMajorite + "]. Traitement automatique impossible.";
			throw new OuvertureForsException(habitant, ErreurType.INCOHERENCE_FOR_FISCAUX, message);
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

		tiersService.openForFiscalPrincipal(habitant, dateMajorite, MotifRattachement.DOMICILE, data.getNumeroOfsAutoriteFiscale(),
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, modeImposition, MotifFor.MAJORITE, true);
		habitant.setMajoriteTraitee(Boolean.TRUE);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("-> ouverture du for principal pour l'habitant n° " + habitant.getNumero());
		}
		rapport.addHabitantTraite(habitant, modeImposition);
	}

	private void fillDatesNaissanceEtDeces(PersonnePhysique habitant, final HabitantData data, RegDate dateReference) throws OuvertureForsException {

		final TiersIndexedData tiersData = searcher.get(habitant.getNumero());
		if (tiersData != null) {
			// si l'individu est indexé, on passe par les données indexées pour aller plus vite
			RegDate dateNaissance = RegDateHelper.indexStringToDate(tiersData.getDateNaissance());
			RegDate dateDeces = RegDateHelper.indexStringToDate(tiersData.getDateDeces());

			data.setIndexerData(tiersData);
			data.setDateNaissance(dateNaissance);
			data.setDateDeces(dateDeces);

			if (dateNaissance != null) {
				// on a toutes les infos
				return;
			}
		}

		// autrement, on doit faire un appel à host-interface
		Individu individu;
		try {
			individu = tiersService.getIndividu(habitant, dateReference.year(), EnumAttributeIndividu.TOUS);
		}
		catch (ServiceCivilException e) {
			LOGGER.error("Impossible de récupérer l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsException(habitant, ErreurType.CIVIL_EXCEPTION, e);
		}

		if (individu == null) {
			LOGGER.error("Impossible de récupérer l'individu n° " + habitant.getNumeroIndividu() + " associé à l'habitant n° "
					+ habitant.getNumero());
			throw new OuvertureForsException(habitant, ErreurType.INDIVIDU_INCONNU, "individu associé n°" + habitant.getNumeroIndividu());
		}

		RegDate dateNaissance = individu.getDateNaissance();
		RegDate dateDeces = individu.getDateDeces();

		data.setIndividu(individu);
		data.setDateNaissance(dateNaissance);
		data.setDateDeces(dateDeces);
	}

	private void fillInfoDomicile(PersonnePhysique habitant, final HabitantData data, RegDate dateReference) throws OuvertureForsException {

		final TiersIndexedData indexerData = data.getIndexerData();
		if (indexerData != null) {

			final Boolean domicilieDansLeCanton = indexerData.isDomicilieDansLeCanton();
			if (domicilieDansLeCanton != null) {
				data.setDomicilieDansLeCanton(domicilieDansLeCanton);
			}

			final Integer noOfsCommuneDomicile = indexerData.getNoOfsCommuneDomicile();
			if (noOfsCommuneDomicile != null) {
				data.setNumeroOfsAutoriteFiscale(noOfsCommuneDomicile);
			}

			if (domicilieDansLeCanton != null && (!domicilieDansLeCanton || noOfsCommuneDomicile != null)) {
				// on a toutes les infos
				return;
			}
		}

		final AdresseGenerique adresseDomicile;
		try {
			adresseDomicile = adresseService.getAdresseFiscale(habitant, TypeAdresseTiers.DOMICILE, dateReference, false);
		}
		catch (AdresseException e) {
			LOGGER.error("Erreur dans les adresse de l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsException(habitant, ErreurType.ADRESSE_EXCEPTION, e);
		}

		if (adresseDomicile.isDefault()) {
			// msi/tdq 3.6.09 : on ne doit pas tenir compte des adresses de domicile par défaut car elles n'ont pas de valeur pour
			// déterminer si un contribuable est dans le canton
			String message = "Impossibilité d'identifier le domicile du contribuable n° " + habitant.getNumero()
					+ " car son adresse de domicile est une valeur par défaut.";
			throw new OuvertureForsException(habitant, ErreurType.DOMICILE_INCONNU, message);
		}

		boolean estDomicilieDansLeCanton;
		Commune commune;
		try {
			estDomicilieDansLeCanton = serviceInfra.estDansLeCanton(adresseDomicile);
			if (estDomicilieDansLeCanton) {
				commune = serviceInfra.getCommuneByAdresse(adresseDomicile);
			}
			else {
				commune = null;
			}
		}
		catch (InfrastructureException e) {
			LOGGER.error("Impossible de récupérer la commune de l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsException(habitant, ErreurType.INFRA_EXCEPTION, e);
		}

		if (estDomicilieDansLeCanton) {
			if (commune == null) {
				String message = "Impossibilité d'identifier le domicile du contribuable n° " + habitant.getNumero()
						+ " car la localité avec le numéro postal " + adresseDomicile.getNumeroOrdrePostal() + " est inconnue.";
				throw new OuvertureForsException(habitant, ErreurType.DOMICILE_INCONNU, message);
			}
		}

		data.setDomicilieDansLeCanton(estDomicilieDansLeCanton);
		data.setNumeroOfsAutoriteFiscale(commune == null ? null : commune.getNoOFSEtendu());
	}

	private void fillEtatCivilPermisEtNationalite(PersonnePhysique habitant, final HabitantData data, RegDate dateReference) throws OuvertureForsException {

		final boolean hasPermisC;
		final boolean hasNationaliteSuisse;

		Individu individu = data.getIndividu();
		if (individu == null) {
			try {
				individu = tiersService.getIndividu(habitant, dateReference.year(), EnumAttributeIndividu.TOUS);
			}
			catch (ServiceCivilException e) {
				LOGGER.error("Impossible de récupérer l'habitant n° " + habitant.getNumero(), e);
				throw new OuvertureForsException(habitant, ErreurType.CIVIL_EXCEPTION, e);
			}
		}

		if (individu == null) {
			LOGGER.error("Impossible de récupérer l'individu n° " + habitant.getNumeroIndividu() + " associé à l'habitant n° "
					+ habitant.getNumero());
			throw new OuvertureForsException(habitant, ErreurType.INDIVIDU_INCONNU, "individu associé n°" + habitant.getNumeroIndividu());
		}

		final EtatCivil etatCivil = individu.getEtatCivil(dateReference);
		if (etatCivil != null) {
			data.setEtatCivil(etatCivil.getTypeEtatCivil());
		}

		hasPermisC = tiersService.isAvecPermisC(individu, dateReference);
		try {
			hasNationaliteSuisse = tiersService.isSuisse(individu, dateReference);
		}
		catch (TiersException e) {
			LOGGER.error("Impossible de récupérer la nationalité de l'habitant n° " + habitant.getNumero(), e);
			throw new OuvertureForsException(habitant, ErreurType.CIVIL_EXCEPTION, e);
		}

		data.setHasPermisC(hasPermisC);
		data.setHasNationaliteSuisse(hasNationaliteSuisse);
	}

	final private static String queryHabitantWithoutFor = // --------------------------------
	"SELECT hab.id                                                                 "
			+ "FROM                                                                "
			+ "    PersonnePhysique AS hab                                         "
			+ "WHERE                                                               "
			+ "    hab.habitant = true                                             "
			+ "	   AND hab.annulationDate IS null                                  "
			+ "	   AND hab.dateDeces IS null                                       "
			+ "	   AND (hab.majoriteTraitee IS null OR hab.majoriteTraitee = 0)    "
			+ "	   AND (hab.dateNaissance IS null OR hab.dateNaissance <= :pivot)  "
			+ "    AND NOT EXISTS (                                                "
			+ "        SELECT                                                      "
			+ "            fors.id                                                 "
			+ "        FROM                                                        "
			+ "            ForFiscalPrincipal AS fors                              "
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
			+ "            AND rap.sujet.id = hab.id                               "
			+ "            AND rap.class = AppartenanceMenage                      "
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
	@SuppressWarnings("unchecked")
	protected List<Long> getListHabitantsSansForPrincipal(final RegDate date) {

		final RegDate datePivot = date.addYears(-FiscalDateHelper.AGE_MAJORITE);

		final List<Long> i = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Query queryObject = session.createQuery(queryHabitantWithoutFor);
				queryObject.setParameter("pivot", datePivot.index());
				queryObject.setParameter("date", date.index());
				return queryObject.list();
			}
		});

		return i;
	}
}
