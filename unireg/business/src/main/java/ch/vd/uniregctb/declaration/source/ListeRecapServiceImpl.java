package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.type.CategorieImpotSource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.editique.DelegateEditique;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapServiceImpl implements ListeRecapService, DelegateEditique {

	/** Le type de document à transmettre au service pour les LR */
	private static final String TYPE_DOCUMENT_LR = "03";

	private DelaisService delaisService;

	private EditiqueService editiqueService;

	private EvenementFiscalService evenementFiscalService;

	private TiersDAO tiersDAO;

	private PeriodeFiscaleDAO periodeDAO;

	private ListeRecapitulativeDAO listeRecapDAO;

	private ModeleDocumentDAO modeleDocumentDAO;

	private PlatformTransactionManager transactionManager;

	private HibernateTemplate hibernateTemplate;

	/**
	 * Creer un document dans l'editique pour l'impression locale
	 *
	 * @param lr
	 */
	public void envoieImpressionLocalLR(DeclarationImpotSource lr) throws EditiqueException {
		this.editiqueService.creerDocumentImmediatement(lr.getId().toString(), null, TypeFormat.PCL, lr, false);
	}

	/**
	 * Recoit un document de l'editique pour l'impression locale
	 *
	 * @param lr
	 * @return
	 * @throws Exception
	 */
	public byte[] recoitImpressionLocalLR(DeclarationImpotSource lr) throws Exception {
		EditiqueResultat resultat = this.editiqueService.getDocument(lr.getId().toString(), true);
		if (resultat.hasError()) {
			throw new Exception(resultat.getError());
		}
		return resultat.getDocument();
	}

	/**
	 * Recupere à l'éditique un document pour afficher une copie conforme (duplicata)
	 *
	 * @param lr
	 * @return le document pdf
	 * @throws EditiqueException
	 */
	public byte[] getCopieConformeLR(DeclarationImpotSource lr) throws EditiqueException {
		byte[] pdf = editiqueService.getPDFDocument(lr.getTiers().getNumero(), TYPE_DOCUMENT_LR, lr.getId().toString());
		return pdf;

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
	 * Somme toutes LR à sommer
	 *
	 * @param categorie
	 *@param dateTraitement  @throws Exception
	 */
	public EnvoiSommationLRsResults sommerAllLR(CategorieImpotSource categorie, RegDate dateTraitement, StatusManager status) throws Exception {
		EnvoiSommationLRsEnMasseProcessor processor = new EnvoiSommationLRsEnMasseProcessor(transactionManager, hibernateTemplate, this,
				delaisService);
		return processor.run(categorie, dateTraitement, status);
	}

	/**
	 * Impression d'une LR - Creation en base de donnée de la LR avec calcul de son nom de document - Alimentation de l'objet
	 * EditiqueListeRecap - Envoi des informations nécessaires à l'éditique
	 *
	 * @param dpi
	 * @param dateDebutPeriode
	 * @throws Exception
	 */
	public void imprimerLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode) throws Exception {
		DeclarationImpotSource lrSaved = saveLR(dpi, dateDebutPeriode);
		/*
		 * Set<Declaration> declarations = dpiSaved.getDeclarations(); Iterator<Declaration> itDec = declarations.iterator();
		 * DeclarationImpotSource lr = null; while (itDec.hasNext()) { Declaration declaration = itDec.next(); if (declaration instanceof
		 * DeclarationImpotSource) { DeclarationImpotSource lrCourante = (DeclarationImpotSource) declaration; if
		 * (lrCourante.getDateDebut().equals(dateDebutPeriode)) { lr = lrCourante; } } }
		 */

		editiqueService.imprimeLRForBatch(lrSaved, RegDate.get());
		evenementFiscalService.publierEvenementFiscalOuverturePeriodeDecompteLR(dpi, lrSaved, RegDate.get());
	}

	private DeclarationImpotSource saveLR(DebiteurPrestationImposable dpi, RegDate dateDebutPeriode) throws Exception {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(dateDebutPeriode);
		lr.setDateFin(dpi.getPeriodiciteDecompte().getFinPeriode(dateDebutPeriode));
		lr.setPeriodicite(dpi.getPeriodiciteDecompte());
		lr.setModeCommunication(dpi.getModeCommunication());

		PeriodeFiscale periodeFiscale = periodeDAO.getPeriodeFiscaleByYear(dateDebutPeriode.year());
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

		EtatDeclaration etat = new EtatDeclaration();
		etat.setDateObtention(RegDate.get());
		etat.setEtat(TypeEtatDeclaration.EMISE);
		lr.addEtat(etat);

		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(RegDate.get());

		// si la date de traitement est avant la fin de la période, alors le délai est 1 mois après la fin de la période
		// sinon, le délai est un mois après l'émission
		final RegDate dateDelai = delaisService.getDateFinDelaiRetourListeRecapitulative(delai.getDateTraitement(), lr.getDateFin());
		delai.setDelaiAccordeAu(dateDelai);
		lr.addDelai(delai);

		lr.setTiers(dpi);
		lr = listeRecapDAO.save(lr);
		dpi.addDeclaration(lr);
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
		EtatDeclaration etat = new EtatDeclaration();
		etat.setEtat(TypeEtatDeclaration.SOMMEE);
		RegDate dateExpedition = delaisService.getDateFinDelaiCadevImpressionListesRecapitulatives(dateTraitement);
		etat.setDateObtention(dateExpedition);
		lr.addEtat(etat);
		editiqueService.imprimeSommationLRForBatch(lr, RegDate.get());
		evenementFiscalService.publierEvenementFiscalSommationLR((DebiteurPrestationImposable) lr.getTiers(), lr, etat.getDateObtention());
	}


	/**
	 * Trouve toutes les LR manquantes d'un débiteur
	 *
	 * @param dpi
	 * @param dateFinPeriode
	 * @param lrTrouveesOut
	 * @return une liste de range de LR manquante
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
						lrManquantes.addAll(subtract(periodeActivite, lrTrouveesIn));
					}
				}

				// maintenant, à partir de cette liste de ranges où il devrait y avoir une LR mais il n'y en a pas
				// il faut extraire les périodes de LR
				if (lrManquantes.size() > 0) {
					final PeriodiciteDecompte periodicite = dpi.getPeriodiciteDecompte();
					lrPeriodiquesManquantes = extrairePeriodesDapresPeriodicite(periodicite, lrManquantes);
				}
			}
		}
		if (lrTrouveesIn != null) {
			lrTrouveesOut.addAll(lrTrouveesIn);
		}
		return lrPeriodiquesManquantes;
	}

	/**
	 *
	 * @param periodicite
	 * @param lrManquantes
	 * @return
	 */
	private static List<DateRange> extrairePeriodesDapresPeriodicite(PeriodiciteDecompte periodicite, List<DateRange> lrManquantes) {
		final List<DateRange> lr = new ArrayList<DateRange>();
		for (DateRange manquante : lrManquantes) {
			RegDate date = manquante.getDateDebut();

			// on fait des bonds de la bonne périodicité tant que l'on reste dans la période à couvrir
			do {
				final RegDate debut = periodicite.getDebutPeriode(date);
				final RegDate fin = periodicite.getFinPeriode(date);
				lr.add(new DateRangeHelper.Range(debut, fin));

				date = fin.addDays(1);
			}
			while (manquante.isValidAt(date));
		}
		return lr;
	}

	/**
	 * Renvoie les ranges compris dans "source" mais pas dans "toRemove"
	 * @param source
	 * @param toRemove supposé trié, composé de ranges non-adjacents
	 * @return
	 */
	private static List<DateRange> subtract(DateRange source, List<DateRange> toRemove) {
		final List<DateRange> result = new ArrayList<DateRange>(toRemove.size() + 1);
		RegDate debutProchain = source.getDateDebut();
		for (DateRange trou : toRemove) {
			if (DateRangeHelper.intersect(source, trou)) {
				if (trou.getDateDebut().isAfter(debutProchain)) {
					result.add(new DateRangeHelper.Range(debutProchain, trou.getDateDebut().addDays(-1)));
				}
				debutProchain = trou.getDateFin().addDays(1);
				if (trou.getDateFin().isAfterOrEqual(source.getDateFin())) {
					// le trou dépasse la zone complète, et les trous sont triés, donc c'est fini
					break;
				}
			}
		}
		if (debutProchain.equals(source.getDateDebut())) {
			Assert.isTrue(result.size() == 0);
			result.add(source);
		}
		else if (debutProchain.isBeforeOrEqual(source.getDateFin())) {
			result.add(new DateRangeHelper.Range(debutProchain, source.getDateFin()));
		}
		return result;
	}

	public void surDocumentRecu(EditiqueResultat resultat) {
	}

	public EditiqueService getEditiqueService() {
		return editiqueService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public EvenementFiscalService getEvenementFiscalService() {
		return evenementFiscalService;
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setPeriodeDAO(PeriodeFiscaleDAO periodeDAO) {
		this.periodeDAO = periodeDAO;
	}

	public DelaisService getDelaisService() {
		return delaisService;
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
}
