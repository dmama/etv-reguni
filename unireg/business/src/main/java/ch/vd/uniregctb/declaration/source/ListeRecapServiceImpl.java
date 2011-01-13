package ch.vd.uniregctb.declaration.source;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.editique.DelegateEditique;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.validation.ValidationInterceptor;

public class ListeRecapServiceImpl implements ListeRecapService, DelegateEditique, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(ListeRecapServiceImpl.class);

	/**
	 * Le type de document à transmettre au service pour les LR
	 */
	private static final String TYPE_DOCUMENT_LR = "03";

	private static final String CONTEXTE_COPIE_CONFORME_SOMMATION = "SommationLR";

	private static final String CONTEXTE_COPIE_CONFORME_LR = "LR";

	private DelaisService delaisService;

	private EditiqueCompositionService editiqueCompositionService;

	private EditiqueService editiqueService;

	private EvenementFiscalService evenementFiscalService;

	private TiersDAO tiersDAO;

	private PeriodeFiscaleDAO periodeDAO;

	private ListeRecapitulativeDAO listeRecapDAO;

	private ModeleDocumentDAO modeleDocumentDAO;

	private PlatformTransactionManager transactionManager;

	private HibernateTemplate hibernateTemplate;

	private ImpressionSommationLRHelper helperSommationLR;

	private TiersService tiersService;

	private ValidationInterceptor validationInterceptor;


	/**
	 * Recupere à l'éditique un document pour afficher une copie conforme (duplicata)
	 *
	 * @param lr
	 * @return le document pdf
	 * @throws EditiqueException
	 */
	public InputStream getCopieConformeLR(DeclarationImpotSource lr) throws EditiqueException {
		return editiqueService.getPDFDeDocumentDepuisArchive(lr.getTiers().getNumero(), TYPE_DOCUMENT_LR, lr.getId().toString(), CONTEXTE_COPIE_CONFORME_LR);
	}

	public InputStream getCopieConformeSommationLR(DeclarationImpotSource lr) throws EditiqueException {
		final String nomDocument = helperSommationLR.construitIdArchivageDocument(lr);
		return editiqueService.getPDFDeDocumentDepuisArchive(lr.getTiers().getNumero(), ImpressionSommationLRHelperImpl.TYPE_DOCUMENT_SOMMATION_LR, nomDocument, CONTEXTE_COPIE_CONFORME_SOMMATION);
	}

	/**
	 * Imprime toutes des LR a une date de fin de periode donnee
	 *
	 * @param dateFinPeriode
	 * @throws Exception
	 */
	public EnvoiLRsResults imprimerAllLR(RegDate dateFinPeriode, StatusManager status) throws Exception {
		final EnvoiLRsEnMasseProcessor processor = new EnvoiLRsEnMasseProcessor(transactionManager, hibernateTemplate, this);
		return processor.run(dateFinPeriode, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public EnvoiSommationLRsResults sommerAllLR(CategorieImpotSource categorie, RegDate dateFinPeriode, RegDate dateTraitement, StatusManager status) {
		final EnvoiSommationLRsEnMasseProcessor processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, this, delaisService);
		return processor.run(categorie, dateFinPeriode, dateTraitement, status);
	}

	/**
	 * Impression d'une LR - Creation en base de donnée de la LR avec calcul de son nom de document - Alimentation de l'objet EditiqueListeRecap - Envoi des informations nécessaires à l'éditique
	 *
	 *
	 * @param dpi
	 * @param dateDebutPeriode
	 * @param dateFinPeriode
	 * @throws Exception
	 */
	public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
		final DeclarationImpotSource lrSaved = saveLR(dpi, dateDebutPeriode,dateFinPeriode);
		/*
		 * Set<Declaration> declarations = dpiSaved.getDeclarations(); Iterator<Declaration> itDec = declarations.iterator();
		 * DeclarationImpotSource lr = null; while (itDec.hasNext()) { Declaration declaration = itDec.next(); if (declaration instanceof
		 * DeclarationImpotSource) { DeclarationImpotSource lrCourante = (DeclarationImpotSource) declaration; if
		 * (lrCourante.getDateDebut().equals(dateDebutPeriode)) { lr = lrCourante; } } }
		 */

		editiqueCompositionService.imprimeLRForBatch(lrSaved, RegDate.get());
		evenementFiscalService.publierEvenementFiscalOuverturePeriodeDecompteLR(dpi, lrSaved, RegDate.get());
	}

	private DeclarationImpotSource saveLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(dateDebutPeriode);
			//[UNIREG-3115] Periodicite non trouvé en debut de periode de lR on cherche à la fin.
		Periodicite periodiciteAt = dpi.findPeriodicite(dateDebutPeriode,dateFinPeriode);
		lr.setDateFin(periodiciteAt.getFinPeriode(dateDebutPeriode));
		lr.setPeriodicite(periodiciteAt.getPeriodiciteDecompte());
		lr.setModeCommunication(dpi.getModeCommunication());

		final PeriodeFiscale periodeFiscale = periodeDAO.getPeriodeFiscaleByYear(dateDebutPeriode.year());
		Assert.notNull(periodeFiscale);
		lr.setPeriode(periodeFiscale);

		ModeleDocument modDoc = modeleDocumentDAO.getModelePourDeclarationImpotSource(periodeFiscale);
		if (modDoc == null) {
			// modele de document a créer
			modDoc = new ModeleDocument();
			modDoc.setPeriodeFiscale(periodeFiscale);
			modDoc.setTypeDocument(TypeDocument.LISTE_RECAPITULATIVE);
			modDoc = modeleDocumentDAO.save(modDoc);
			periodeFiscale.addModeleDocument(modDoc);
		}

		lr.setModeleDocument(modDoc);

		final EtatDeclaration etat = new EtatDeclarationEmise();
		etat.setDateObtention(RegDate.get());
		lr.addEtat(etat);

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());

		// si la date de traitement est avant la fin de la période, alors le délai est 1 mois après la fin de la période
		// sinon, le délai est un mois après l'émission
		final RegDate dateDelai = delaisService.getDateFinDelaiRetourListeRecapitulative(delai.getDateTraitement(), lr.getDateFin());
		delai.setDelaiAccordeAu(dateDelai);
		lr.addDelai(delai);

		lr.setTiers(dpi);
		lr = listeRecapDAO.save(lr);

		// [UNIREG-2164] cet appel provoque la réindexation du débiteur pour rien : dpi.addDeclaration(lr);
		// DebiteurPrestationImposable dpiSaved = (DebiteurPrestationImposable) tiersDAO.save(dpi);
		return lr;
	}

	/**
	 * Impression d'une sommation LR - Alimentation de l'objet EditiqueListeRecap - Envoi des informations nécessaires à l'éditique
	 *
	 * @param dpi
	 * @throws Exception
	 */
	public void imprimerSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws Exception {

		final RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionListesRecapitulatives(dateTraitement);
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateTraitement,dateExpedition);
		lr.addEtat(etat);
		editiqueCompositionService.imprimeSommationLRForBatch(lr, RegDate.get());
		evenementFiscalService.publierEvenementFiscalSommationLR((DebiteurPrestationImposable) lr.getTiers(), lr, etat.getDateObtention());
	}


	/**
	 * {@inheritDoc}
	 */
	public List<DateRange> findLRsManquantes(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, List<DateRange> lrTrouveesOut) {
		// l'algo est le suivant :
		// 1. détermination des périodes d'activité globales du débiteur
		// 2. détermination des périodes sur lesquelles on a des LRs
		// 3. soustraction à l'ensemble vu en 1 de l'ensemble vu en 2
		// 4. s'il reste quelque chose, c'est qu'il y a des LRs à générer

		List<DateRange> lrPeriodiquesManquantes = null;
		List<DateRange> lrTrouveesIn = null;
		DateRange periodeInteressante = new DateRangeHelper.Range(null, dateFinPeriode);
		final List<ForFiscal> fors = dpi.getForsFiscauxNonAnnules(true);
		if (fors != null && fors.size() > 0) {

			// d'abord on cherche les périodes d'activité d'après les fors non-annulés (seulement
			// la partie d'entre elles qui tient jusqu'à la fin de la période donnée en paramètre)
			final List<DateRange> periodesBruttesActivite = DateRangeHelper.collateRange(fors);
			final List<DateRange> periodesActivite = new ArrayList<DateRange>(periodesBruttesActivite.size());
			for (DateRange periodeBrutte : periodesBruttesActivite) {
				if (periodeInteressante.isValidAt(periodeBrutte.getDateDebut())) {
					if (periodeInteressante.isValidAt(periodeBrutte.getDateFin())) {
						periodesActivite.add(periodeBrutte);
					}
					else {
						periodesActivite.add(new DateRangeHelper.Range(periodeBrutte.getDateDebut(), periodeInteressante.getDateFin()));
					}
				}
			}

			// si aucune période d'activité dans la période intéressante (avant la date de fin),
			// pas la peine d'aller plus loin
			if (periodesActivite.size() > 0) {

				// ici, on va prendre la période max (pour limiter l'appel à la méthode du DAO)
				final List<RegDate> boundaries = DateRangeHelper.extractBoundaries(periodesActivite);
				Assert.isTrue(boundaries.size() >= 1);
				final RegDate toutDebut = boundaries.get(0) != null ? boundaries.get(0) : RegDate.getEarlyDate();
				final RegDate finUltime;
				{
					final RegDate fin = boundaries.get(boundaries.size() - 1);
					if (fin == null || fin.isAfter(dateFinPeriode)) {
						finUltime = dateFinPeriode;
					}
					else {
						finUltime = fin;
					}
				}
				final DateRange periodeLrARegarder = new DateRangeHelper.Range(toutDebut, finUltime);

				// ensuite, on va chercher les LR dans cette période
				lrTrouveesIn = DateRangeHelper.collateRange(listeRecapDAO.findIntersection(dpi.getNumero(), periodeLrARegarder));

				// idéalement, tous les ranges, une fois les "collate" effectués, devraient être les mêmes
				final List<DateRange> lrManquantes;
				if (lrTrouveesIn.size() == 0) {
					lrManquantes = periodesActivite;
				}
				else {
					lrManquantes = new ArrayList<DateRange>();
					for (DateRange periodeActivite : periodesActivite) {
						lrManquantes.addAll(DateRangeHelper.subtract(periodeActivite, lrTrouveesIn));
					}
				}

				// maintenant, à partir de cette liste de ranges où il devrait y avoir une LR mais il n'y en a pas
				// il faut extraire les périodes de LR
				if (lrManquantes.size() > 0) {
					final List<DateRange>lrManquantesAjustees = ajusterSelonPeriodeFiscale(lrManquantes);
					lrPeriodiquesManquantes = extrairePeriodesAvecPeriodicites(dpi, lrManquantesAjustees);
				}
			}
		}
		if (lrTrouveesIn != null) {
			lrTrouveesOut.addAll(lrTrouveesIn);
		}
		return lrPeriodiquesManquantes;
	}

	/**
	 * Permet de confiner les ranges dans une période fiscale
	 * si on a un range qui couvre n périodes fiscales, il devra étre divisé en n ranges
	 * @param source liste de ranges qui peuvent chevaucher les frontières d'années
	 * @return liste de ranges dont la couverture est équivalente à celle de la source en ayant introduit des scissions à la Saint Sylvestre de chaque année
	 */
	protected static List<DateRange> ajusterSelonPeriodeFiscale(List<DateRange> source) {
		final List<DateRange> result = new ArrayList<DateRange>();
		for (DateRange manquante : source) {
			final RegDate debut = manquante.getDateDebut();
			final RegDate fin = manquante.getDateFin();
			final int periodeDebut = debut.year();
			final int periodeFin = fin.year();
			if (periodeDebut < periodeFin) {
				result.add(new DateRangeHelper.Range(debut, RegDate.get(periodeDebut, 12, 31)));
				for (int i = periodeDebut + 1; i < periodeFin ; ++ i) {
					result.add(new DateRangeHelper.Range(RegDate.get(i, 1, 1), RegDate.get(i, 12, 31)));
				}
				result.add(new DateRangeHelper.Range(RegDate.get(periodeFin, 1, 1), fin));
			}
			else {
				 result.add(manquante);
			}
		}
		return result;
	}

	/**
	 * Pour chacun des débiteurs à qui on a envoyé toutes les LR de la période fiscale donnée, et pour lesquels il existe au moins une LR échue (dont le délai de retour de la sommation a été bien dépassé
	 * à la date de traitement), envoie un événement fiscal "liste récapitulative manquante"
	 *
	 * @param periodeFiscale période fiscale sur laquelle les LR sont inspectées
	 * @param dateTraitement date déterminante pour savoir si un délai a été dépassé
	 */
	public DeterminerLRsEchuesResults determineLRsEchues(int periodeFiscale, RegDate dateTraitement, StatusManager status) throws Exception {
		final DeterminerLRsEchuesProcessor processor = new DeterminerLRsEchuesProcessor(transactionManager, hibernateTemplate, this, delaisService, tiersDAO, listeRecapDAO, evenementFiscalService, tiersService);
		return processor.run(periodeFiscale, dateTraitement, status);
	}

	/**
	 * @param periodicite
	 * @param lrManquantes
	 * @return
	 */
	protected static List<DateRange> extrairePeriodesAvecPeriodicites(DebiteurPrestationImposable debiteur, List<DateRange> lrManquantes) {
		final List<DateRange> lr = new ArrayList<DateRange>();
		for (DateRange manquante : lrManquantes) {
			RegDate date = manquante.getDateDebut();

			// on fait des bonds de la bonne périodicité tant que l'on reste dans la période à couvrir
			do {
				final Periodicite periodiciteCourante = debiteur.getPeriodiciteAt(date);
				if (periodiciteCourante == null) {
					throw new IllegalArgumentException("Incohérence des données : le débiteur n°" + debiteur.getNumero() +
							" ne possède pas de périodicité à la date [" + RegDateHelper.dateToDisplayString(date) +
							"] alors même qu'il existe un for fiscal. Problème de migration des périodicités ?");
				}

				if (periodiciteCourante.getPeriodiciteDecompte() == PeriodiciteDecompte.UNIQUE) {
					PeriodeDecompte periode = periodiciteCourante.getPeriodeDecompte();
					DateRange datePeriode =  periode.getPeriodeCourante(date);
					if (DateRangeHelper.intersect(datePeriode, manquante)) {
						lr.add(datePeriode);
					}
					date = datePeriode.getDateFin().addYears(1);
				}
				else {
					final RegDate debut = periodiciteCourante.getDebutPeriode(date);
					final RegDate fin = periodiciteCourante.getFinPeriode(date);
					lr.add(new DateRangeHelper.Range(debut, fin));
					date = fin.addDays(1);
				}
			}
			while (manquante.isValidAt(date));
		}
		return lr;
	}

	

	public void surDocumentRecu(EditiqueResultat resultat) {
	}

	/**
	 * Permet de reconstruire l'historique des periodicités a partir des LR de chaque debiteurs.
	 */
	public void afterPropertiesSet() throws Exception {

		// on désactive la validation des tiers pendant le calcul des périodicités des débiteurs qui n'ont
		// encore aucun historique
		final boolean enabled = validationInterceptor.isEnabled();
		validationInterceptor.setEnabled(false);
		try {
			createAllPeriodicites();
		}
		finally {
			validationInterceptor.setEnabled(enabled);
		}
	}

	@SuppressWarnings({"unchecked"})
	private void createAllPeriodicites() {

		final TransactionTemplate t = new TransactionTemplate(transactionManager);
		final List<Long> ids = (List<Long>) t.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return tiersDAO.getListeDebiteursSansPeriodicites();
			}
		});

		if (!ids.isEmpty()) {

			LOGGER.warn("--- Début de la création de l'historique des périodicités---");
			MigrationResults rapportFinal = new MigrationResults();

			AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);

			try {

				BatchTransactionTemplate<Long, MigrationResults> template =
						new BatchTransactionTemplate<Long, MigrationResults>(ids, 100, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, hibernateTemplate);
				template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, MigrationResults>() {

					@Override
					public MigrationResults createSubRapport() {
						return new MigrationResults();
					}

					@Override
					public boolean doInTransaction(List<Long> batch, MigrationResults rapport) throws Exception {
						createHistoriquePeriodicite(batch);
						return true;
					}
				});

			}
			finally {

				AuthenticationHelper.popPrincipal();
			}

			for (Map.Entry<Long, String> s : rapportFinal.erreurs.entrySet()) {
				LOGGER.error("Impossible de crééer l'historique des périodicités pour le debiteur =" + s.getKey() + " Erreur=" + s.getValue());
			}

			LOGGER.warn("--- Fin de la création des Périodicités (Nombre de débiteurs impactés=" + (ids.size() - rapportFinal.erreurs.size()) + ", en erreur=" + rapportFinal.erreurs.size() + ") ---");
		}
	}

	private void createHistoriquePeriodicite(List<Long> batch) {
		for (Long debiteurId : batch) {
			DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(debiteurId);
			List<Periodicite> listePeriodiciteACreer = construirePeriodiciteFromLR(debiteur);
			if (listePeriodiciteACreer.isEmpty()) {
				tiersService.addPeriodicite(debiteur, debiteur.getPeriodiciteDecompteAvantMigration(), debiteur.getPeriodeDecompteAvantMigration(), RegDate.get(debiteur.getLogCreationDate()), null);
			}
			else {
				for (Periodicite periodicite : listePeriodiciteACreer) {
					tiersService.addPeriodicite(debiteur, periodicite.getPeriodiciteDecompte(), periodicite.getPeriodeDecompte(), periodicite.getDateDebut(), periodicite.getDateFin());
				}
			}


		}
	}

	private List<Periodicite> construirePeriodiciteFromLR(DebiteurPrestationImposable debiteur) {
		List<Declaration> listeLR = debiteur.getDeclarationsSorted();
		List<Periodicite> listePeriodiciteIntermediaire = new ArrayList<Periodicite>();


		//Transformation en lilste de periodicite
		for (Declaration declaration : listeLR) {
			PeriodeDecompte periodeDecompte = null;
			if (!declaration.isAnnule()) {
				DeclarationImpotSource lr = (DeclarationImpotSource) declaration;
				//Pour la periodicite de type UNIQUE, on doit egalement creer la periode decompte associé,
				//Cette dernière est une propriété du tiers et non de la LR

				if (PeriodiciteDecompte.UNIQUE == lr.getPeriodicite()) {
					periodeDecompte = debiteur.getPeriodeDecompteAvantMigration();
				}

				Periodicite periodicite = new Periodicite(lr.getPeriodicite(), periodeDecompte, lr.getDateDebut(), lr.getDateFin());
				listePeriodiciteIntermediaire.add(periodicite);
			}

		}
		listePeriodiciteIntermediaire = Periodicite.comblerVidesPeriodicites(listePeriodiciteIntermediaire);


		return DateRangeHelper.collate(listePeriodiciteIntermediaire);  //To change body of created methods use File | Settings | File Templates.
	}


	private static class MigrationResults implements BatchResults<Long, MigrationResults> {

		public final Map<Long, String> erreurs = new HashMap<Long, String>();

		public void addErrorException(Long element, Exception e) {
			erreurs.put(element, e.getMessage());
		}

		public void addAll(MigrationResults right) {
			erreurs.putAll(right.erreurs);
		}
	}

	public void setEditiqueCompositionService(EditiqueCompositionService editiqueCompositionService) {
		this.editiqueCompositionService = editiqueCompositionService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setPeriodeDAO(PeriodeFiscaleDAO periodeDAO) {
		this.periodeDAO = periodeDAO;
	}

	public void setDelaisService(DelaisService delaisService) {
		this.delaisService = delaisService;
	}

	public void setListeRecapDAO(ListeRecapitulativeDAO listeRecapDAO) {
		this.listeRecapDAO = listeRecapDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setModeleDocumentDAO(ModeleDocumentDAO modeleDocumentDAO) {
		this.modeleDocumentDAO = modeleDocumentDAO;
	}

	public void setHelperSommationLR(ImpressionSommationLRHelper helperSommationLR) {
		this.helperSommationLR = helperSommationLR;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setValidationInterceptor(ValidationInterceptor validationInterceptor) {
		this.validationInterceptor = validationInterceptor;
	}
}
