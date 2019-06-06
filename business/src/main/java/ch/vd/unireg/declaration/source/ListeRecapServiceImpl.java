package ch.vd.unireg.declaration.source;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AddAndSaveHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ListeRecapitulativeDAO;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscalAddAndSaveAccessor;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueService;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeDelaiDeclaration;
import ch.vd.unireg.type.TypeDocument;

public class ListeRecapServiceImpl implements ListeRecapService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListeRecapServiceImpl.class);

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

	private AdresseService adresseService;

	private TicketService ticketService;

	@Override
	public EditiqueResultat getCopieConformeSommationLR(DeclarationImpotSource lr) throws EditiqueException {
		final String nomDocument = helperSommationLR.construitIdArchivageDocument(lr);
		return editiqueService.getPDFDeDocumentDepuisArchive(lr.getTiers().getNumero(), TypeDocumentEditique.SOMMATION_LR, nomDocument);
	}

	/**
	 * Imprime toutes des LR a une date de fin de periode donnee
	 *
	 * @param dateFinPeriode
	 * @throws Exception
	 */
	@Override
	public EnvoiLRsResults imprimerAllLR(RegDate dateFinPeriode, StatusManager status) throws Exception {
		final EnvoiLRsEnMasseProcessor processor = new EnvoiLRsEnMasseProcessor(transactionManager, hibernateTemplate, this, tiersService, adresseService, ticketService);
		return processor.run(dateFinPeriode, status);
	}

	@Override
	public EnvoiSommationLRsResults sommerAllLR(CategorieImpotSource categorie, RegDate dateFinPeriode, RegDate dateTraitement, StatusManager status) {
		final EnvoiSommationLRsEnMasseProcessor processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, this, delaisService, tiersService, adresseService);
		return processor.run(categorie, dateFinPeriode, dateTraitement, status);
	}

	/**
	 * Impression d'une LR <b>en mode batch</b> - Creation en base de donnée de la LR avec calcul de son nom de document - Alimentation de l'objet EditiqueListeRecap - Envoi des informations nécessaires à l'éditique
	 */
	@Override
	public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
		final DeclarationImpotSource lrSaved = saveLR(dpi, dateDebutPeriode,dateFinPeriode);
		editiqueCompositionService.imprimeLRForBatch(lrSaved);
		evenementFiscalService.publierEvenementFiscalEmissionListeRecapitulative(lrSaved, RegDate.get());
	}

	private DeclarationImpotSource saveLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode, RegDate dateFinPeriode) throws Exception {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(dateDebutPeriode);
		//[UNIREG-3115] Periodicite non trouvé en debut de periode de lR on cherche à la fin.
		final Periodicite periodiciteAt = dpi.findPeriodicite(dateDebutPeriode, dateFinPeriode);
		final PeriodiciteDecompte periodiciteDecompte = periodiciteAt.getPeriodiciteDecompte();
		if (periodiciteDecompte == PeriodiciteDecompte.UNIQUE) {
			// [SIFISC-14407] la fin de période est calculée différemment pour les périodicités uniques
			final PeriodeDecompte periode = periodiciteAt.getPeriodeDecompte();
			lr.setDateFin(periode.getPeriodeCourante(dateDebutPeriode).getDateFin());
		}
		else {
			lr.setDateFin(periodiciteAt.getFinPeriode(dateDebutPeriode));
		}
		lr.setPeriodicite(periodiciteDecompte);
		lr.setModeCommunication(dpi.getModeCommunication());

		final PeriodeFiscale periodeFiscale = periodeDAO.getPeriodeFiscaleByYear(dateDebutPeriode.year());
		if (periodeFiscale == null) {
			throw new IllegalArgumentException();
		}
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

		final RegDate today = RegDate.get();

		final EtatDeclaration etat = new EtatDeclarationEmise();
		etat.setDateObtention(today);
		lr.addEtat(etat);

		final DelaiDeclaration delai = new DelaiDeclaration();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateDemande(today);
		delai.setDateTraitement(today);
		delai.setTypeDelai(TypeDelaiDeclaration.IMPLICITE); // [FISCPROJ-873] Par définition, les délais des envois en masse sont implicites

		// si la date de traitement est avant la fin de la période, alors le délai est 1 mois après la fin de la période
		// sinon, le délai est un mois après l'émission
		final RegDate dateDelai = delaisService.getDateFinDelaiRetourListeRecapitulative(delai.getDateTraitement(), lr.getDateFin());
		delai.setDelaiAccordeAu(dateDelai);
		lr.addDelai(delai);

		lr.setTiers(dpi);
		lr = listeRecapDAO.save(lr);

		// [SIFISC-7979] force la re-validation du débiteur
		dpi.addDeclaration(lr);
		return lr;
	}

	/**
	 * Impression d'une sommation LR - Alimentation de l'objet EditiqueListeRecap - Envoi des informations nécessaires à l'éditique
	 * @throws Exception
	 */
	@Override
	public void imprimerSommationLR(DeclarationImpotSource lr, RegDate dateTraitement) throws Exception {

		final RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionListesRecapitulatives(dateTraitement);
		final EtatDeclarationSommee etat = new EtatDeclarationSommee(dateTraitement, dateExpedition, null);
		AddAndSaveHelper.addAndSave(lr, etat, listeRecapDAO::save, new EtatDocumentFiscalAddAndSaveAccessor<>());
		editiqueCompositionService.imprimeSommationLRForBatch(lr, RegDate.get());
		evenementFiscalService.publierEvenementFiscalSommationListeRecapitulative(lr, etat.getDateObtention());
	}

	@Override
	public List<DateRange> findLRsManquantes(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, List<DateRange> lrTrouveesOut) {
		// l'algo est le suivant :
		// 1. détermination des périodes d'activité globales du débiteur
		// 2. détermination des périodes sur lesquelles on a des LRs
		// 3. soustraction à l'ensemble vu en 1 de l'ensemble vu en 2
		// 4. s'il reste quelque chose, c'est qu'il y a des LRs à générer

		List<DateRange> lrPeriodiquesManquantes = null;
		List<DateRange> lrTrouveesIn = null;
		final DateRange periodeInteressante = new DateRangeHelper.Range(null, dateFinPeriode);
		final List<ForFiscal> fors = dpi.getForsFiscauxNonAnnules(true);
		if (fors != null && !fors.isEmpty()) {

			// d'abord on cherche les périodes d'activité d'après les fors non-annulés (seulement
			// la partie d'entre elles qui tient jusqu'à la fin de la période donnée en paramètre)
			final List<DateRange> periodesBruttesActivite = DateRangeHelper.collateRange(fors);
			final List<DateRange> periodesActivite = new ArrayList<>(periodesBruttesActivite.size());
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
			if (!periodesActivite.isEmpty()) {

				// ici, on va prendre la période max (pour limiter l'appel à la méthode du DAO)
				final List<RegDate> boundaries = DateRangeHelper.extractBoundaries(periodesActivite);
				if (boundaries.size() < 1) {
					throw new IllegalArgumentException();
				}
				final RegDate toutDebut = boundaries.get(0) != null ? boundaries.get(0) : RegDateHelper.getEarlyDate();
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
				if (lrTrouveesIn.isEmpty()) {
					lrManquantes = periodesActivite;
				}
				else {
					lrManquantes = new ArrayList<>();
					for (DateRange periodeActivite : periodesActivite) {
						lrManquantes.addAll(DateRangeHelper.subtract(periodeActivite, lrTrouveesIn));
					}
				}

				// maintenant, à partir de cette liste de ranges où il devrait y avoir une LR mais il n'y en a pas
				// il faut extraire les périodes de LR
				if (!lrManquantes.isEmpty()) {
					final List<DateRange> lrManquantesAjustees = ajusterSelonPeriodeFiscale(lrManquantes);
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
		final List<DateRange> result = new ArrayList<>();
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
	 * @param periodeFiscale période fiscale sur laquelle les LR sont inspectées (<code>null</code> si toutes doivent l'être)
	 * @param dateTraitement date déterminante pour savoir si un délai a été dépassé
	 */
	@Override
	public DeterminerLRsEchuesResults determineLRsEchues(Integer periodeFiscale, RegDate dateTraitement, StatusManager status) throws Exception {
		final DeterminerLRsEchuesProcessor processor = new DeterminerLRsEchuesProcessor(transactionManager, hibernateTemplate, this, delaisService, tiersDAO, listeRecapDAO, evenementFiscalService, tiersService, adresseService);
		return processor.run(periodeFiscale, dateTraitement, status);
	}

	protected static List<DateRange> extrairePeriodesAvecPeriodicites(DebiteurPrestationImposable debiteur, List<DateRange> lrManquantes) {
		final List<DateRange> lr = new ArrayList<>();
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
					date = fin.getOneDayAfter();
				}
			}
			while (manquante.isValidAt(date));
		}
		return lr;
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

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTicketService(TicketService ticketService) {
		this.ticketService = ticketService;
	}
}
