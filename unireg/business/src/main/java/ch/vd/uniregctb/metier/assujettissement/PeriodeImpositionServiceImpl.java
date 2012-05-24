package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;

import static ch.vd.uniregctb.metier.assujettissement.PeriodeImposition.CauseFermeture;

/**
 * Implémentation du service de détermination des périodes d'imposition à partir de l'assujettissement d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PeriodeImpositionServiceImpl implements PeriodeImpositionService {

	private AssujettissementService assujettissementService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	@Override
	public List<PeriodeImposition> determine(Contribuable contribuable, int annee) throws AssujettissementException {
		return determine(new DecompositionForsAnneeComplete(contribuable, annee));
	}

	@Override
	public List<PeriodeImposition> determine(Contribuable contribuable, @Nullable DateRange range) throws AssujettissementException {
		if (range != null && isFullYear(range)) {
			return determine(contribuable, range.getDateDebut().year());
		}
		else {
			int anneeDebut;
			int anneeFin;

			if (range == null) {
				RegDate debut = contribuable.getDateDebutActivite();
				if (debut == null) {
					// aucun date de début d'activité, le contribuable ne possède pas de for et ne peut donc pas être assujetti
					return null;
				}
				anneeDebut = debut.year();

				RegDate fin = contribuable.getDateFinActivite();
				if (fin == null) {
					anneeFin = RegDate.get().year(); // annee courante
				}
				else {
					anneeFin = fin.year();
				}

			}
			else {
				anneeDebut = range.getDateDebut().year();
				anneeFin = range.getDateFin().year();
			}

			// Détermination des périodes d'imposition sur toutes les années considérées
			List<PeriodeImposition> list = new ArrayList<PeriodeImposition>();
			for (int annee = anneeDebut; annee <= anneeFin; annee++) {
				List<PeriodeImposition> l = determine(contribuable, annee);
				if (l != null) {
					list.addAll(l);
				}
			}

			// Réduction au range spécifié
			if (range != null) {
				List<PeriodeImposition> results = new ArrayList<PeriodeImposition>();
				for (PeriodeImposition a : list) {
					if (DateRangeHelper.intersect(a, range)) {
						results.add(a);
					}
				}
				list = results;
			}

			return list;
		}
	}

	private static boolean isFullYear(DateRange range) {
		final RegDate debut = range.getDateDebut();
		final RegDate fin = range.getDateFin();
		return debut.year() == fin.year() && debut.month() == 1 && debut.day() == 1 && fin.month() == 12 && fin.day() == 31;
	}

	@Override
	public List<PeriodeImposition> determine(DecompositionForsAnneeComplete fors) throws AssujettissementException {

		// on calcul l'assujettissement complet du contribuable
		final List<Assujettissement> assujettissements = assujettissementService.determine(fors.contribuable, fors.annee);

		// le contribuable n'est pas assujetti cette année-là
		if (assujettissements == null || assujettissements.isEmpty()) {
			return null;
		}

		// On retourne tous les ranges qui ne sont pas associés avec une déclaration
		final List<PeriodeImposition> periodes = new ArrayList<PeriodeImposition>();
		for (Assujettissement a : assujettissements) {
			final PeriodeImposition periode = determinePeriodeImposition(fors, a);
			//on calcule la qualification
			if (periode != null) {
				periodes.add(periode);
			}
		}

		// [UNIREG-1118] On fusionne les périodes qui provoqueraient des déclarations identiques contiguës.
		return DateRangeHelper.collate(periodes);
	}

	/**
	 * [UNIREG-1741] Détermine l'adresse de retour des déclarations d'impôt ordinaires en fonction de l'assujettissement spécifié.
	 *
	 * @param a un assujettissement
	 * @return l'adresse de retour correspondante.
	 */
	private static TypeAdresseRetour determineAdresseRetour(Assujettissement a) {
		if (a.getMotifFractFin() == MotifFor.VEUVAGE_DECES) {
			return TypeAdresseRetour.ACI;
		}
		if (a instanceof VaudoisDepense) {
			return TypeAdresseRetour.OID;
		}
		if (a instanceof HorsCanton && !a.getFors().secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
			return TypeAdresseRetour.OID;
		}
		return TypeAdresseRetour.CEDI;
	}

	private static PeriodeImposition.CauseFermeture getCauseFermeture(MotifFor motifFractionnement, boolean isAssujettissementHS) {
		final CauseFermeture cause;
		if (motifFractionnement == null) {
			cause = null;
		}
		else {
			switch (motifFractionnement) {
			case VEUVAGE_DECES:
				cause = CauseFermeture.VEUVAGE_DECES;
				break;

			case VENTE_IMMOBILIER:
			case FIN_EXPLOITATION:
				cause = isAssujettissementHS ? CauseFermeture.FIN_ASSUJETTISSEMENT_HS : CauseFermeture.AUTRE;
				break;

			default:
				cause = CauseFermeture.AUTRE;
				break;
			}
		}
		return cause;
	}

	@Override
	public PeriodeImposition determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement) {

		final Contribuable contribuable = assujettissement.getContribuable();
		final RegDate debutAssujettissement = assujettissement.getDateDebut();
		final RegDate finAssujettissement = assujettissement.getDateFin();
		final int annee = finAssujettissement.year();
		final CauseFermeture causeFermeture = getCauseFermeture(assujettissement.getMotifFractFin(), assujettissement instanceof HorsSuisse);

		/*
		 * Sourcier pur
		 */
		if (assujettissement instanceof SourcierPur) {
			// Les sourciers purs sont perçus à la source uniquement et ne reçoivent donc pas de déclaration
			return null;
		}

		final Qualification qualification = determineQualification(contribuable, annee);
		final Integer codeSegment = determineCodeSegment(contribuable, annee);
		final TypeAdresseRetour adresseRetour = determineAdresseRetour(assujettissement);

		/*
		 * Diplomate Suisse
		 */
		if (assujettissement instanceof DiplomateSuisse) {
			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
				// [UNIREG-1976] diplomates suisses basés à l'étranger et qui possèdent un ou plusieurs immeubles => déclaration ordinaire
				final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.DIPLOMATE_SUISSE, contribuable, annee);
				final boolean optionnelle = (assujettissement.getMotifFractFin() != MotifFor.VENTE_IMMOBILIER && assujettissement.getMotifFractFin() != MotifFor.VEUVAGE_DECES);
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, optionnelle, false, causeFermeture);
			}
			else {
				// Les diplomates Suisses basés à l'étranger ne reçoivent pas de déclaration, mais la période d'imposition existe bel et bien.
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, CategorieEnvoiDI.DIPLOMATE_SUISSE, contribuable, null, null, null, false, false, causeFermeture);
			}
		}

		/**
		 * Pour un contribuable vaudois (ordinaire, dépense, sourcier ou indigent), la période d'assujettissement est toujours égale à
		 * l'année complète sauf en cas de:
		 *
		 * <pre>
		 *  - décès
		 *  - veuvage
		 *  - d'arrivée de l'étranger
		 *  - départ à l'étranger (pour autant qu'il ne reste pas un for secondaire sur un immeuble)
		 *  - plus plein de cas spéciaux, voir l'implémentation de la classe Assujettissement pour les détails
		 * </pre>
		 */

		/*
		 * Sourcier mixte
		 */
		if (assujettissement instanceof SourcierMixte) {

			final SourcierMixte mixte = (SourcierMixte) assujettissement;
			final DecompositionFors forsPeriode = mixte.getFors();

			boolean optionnelle = false;
			boolean remplaceeParNote = false;

			if (forsPeriode.principal.getModeImposition() == ModeImposition.MIXTE_137_2 && mixte.getMotifFractFin() == MotifFor.DEPART_HC) {
				// [UNIREG-1742] Cas des contribuables imposés selon le mode mixte, partis dans un autre canton durant l’année et n’ayant aucun
				// rattachement économique -> bien qu’ils soient assujettis de manière illimitée jusqu'au dernier jour du mois de leur départ,
				// leur déclaration d’impôt est remplacée (= elle est optionnelle, en fait, voir exemples à la fin de la spécification) par une
				// note à l’administration fiscale cantonale de leur domicile.
				// [UNIREG-2328] A noter que cela ne s'applique pas aux arrivées de hors-canton : dans ce cas l'administration fiscale responsable est l'administration vaudoise.
				optionnelle = true;
				remplaceeParNote = true;
			}

			if (mixte.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
				if (forsPeriode.secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
					// Sourcier mixte hc avec activité indépendante => déclaration ordinaire
					final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_CANTON, contribuable, annee);
					return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, optionnelle, remplaceeParNote, causeFermeture);
				}
				else {
					// Sourciers mixtes hors-canton avec immeuble => déclaration HC immeuble
					return new PeriodeImposition(debutAssujettissement, finAssujettissement, CategorieEnvoiDI.HC_IMMEUBLE, contribuable, qualification, codeSegment, TypeAdresseRetour.ACI, causeFermeture);
				}
			}
			else {
				// Sourcier mixte vaudois => déclaration ordinaire
				final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.VAUDOIS_ORDINAIRE, contribuable, annee);
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, optionnelle, remplaceeParNote, causeFermeture);
			}
		}

		/*
		 * Vaudois à la dépense
		 */
		if (assujettissement instanceof VaudoisDepense) {
			return new PeriodeImposition(debutAssujettissement, finAssujettissement, CategorieEnvoiDI.VAUDOIS_DEPENSE, contribuable, qualification, codeSegment, adresseRetour, causeFermeture);
		}

		/*
		 * Vaudois ordinaire (ou indigent, pas de différence de traitement ici : c'est lors de l'envoi des DIs qu'ils seron traités différement)
		 */
		if (assujettissement instanceof VaudoisOrdinaire || assujettissement instanceof Indigent) {
			// Vaudois ordinaire => déclaration ordinaire
			final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.VAUDOIS_ORDINAIRE, contribuable, annee);
			return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, causeFermeture);
		}

		/*
		 * Hors canton
		 */
		if (assujettissement instanceof HorsCanton) {

			boolean remplaceeParNote = false;

			if (assujettissement.getMotifFractFin() == MotifFor.VENTE_IMMOBILIER || assujettissement.getMotifFractFin() == MotifFor.FIN_EXPLOITATION ||
					causeFermeture == CauseFermeture.VEUVAGE_DECES) {
				// [UNIREG-1742] dans le cas des contribuables domiciliés dans un autre canton dont le rattachement économique (activité indépendante ou immeuble)
				// s’est terminé au cours de la période fiscale, la déclaration est remplacée par une note à l'administration fiscale de l'autre canton.
				remplaceeParNote = true;
			}

			// [UNIREG-1360] Pour un contribuable hors canton, la période d'imposition est toujours égale à la période d'assujettissement
			// (qui elle-même est égale à l'année complète, sauf en cas de fractionnement de l'assujettissement). Pour autant qu'il ait des
			// fors secondaires (ce qui est forcément le cas, puisqu'il est assujetti).

			if (fors.secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
				// Activité indépendante dans le canton => déclaration ordinaires
				final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_CANTON, contribuable, annee);
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, false, remplaceeParNote, causeFermeture);
			}
			else {
				// Immeuble dans le canton => déclaration HC immeuble
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, CategorieEnvoiDI.HC_IMMEUBLE, contribuable, qualification, codeSegment, adresseRetour, false, remplaceeParNote, causeFermeture);
			}
		}

		/*
		 * Hors Suisse
		 */
		if (assujettissement instanceof HorsSuisse) {
			// [UNIREG-1742] pour les hors-Suisse, la période d'imposition corresponds simplement à la période d'assujettissement (confirmé par Thierry Declercq le 18 décembre 2009)

			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
				// Activité indépendante dans le canton => déclaration ordinaire
				final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_SUISSE, contribuable, annee);
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, causeFermeture);
			}

			final ForFiscalPrincipal dernierPrincipal = assujettissement.getFors().principauxDansLaPeriode.last();
			if (dernierPrincipal.getMotifRattachement() == MotifRattachement.DIPLOMATE_ETRANGER) {
				// Fonctionnaire international ou diplomate étranger propriétaire d'immeuble dans le canton => déclaration ordinaire
				final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.VAUDOIS_ORDINAIRE, contribuable, annee);
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, causeFermeture);
			}

			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
				// [UNIREG-1742] Les contribuables domiciliées à l'étranger assujettis à raison d'une propriété d'immeuble [...] sont imposés
				// selon un mode forfaitaire et *peuvent* recevoir une déclaration d'impôt à leur demande (dès l’année d’acquisition du 1er immeuble),
				// mais n’en bénéficient *plus* l’année de la vente du dernier immeuble ou du décès.
				final boolean optionnelle = (assujettissement.getMotifFractFin() != MotifFor.VENTE_IMMOBILIER && assujettissement.getMotifFractFin() != MotifFor.VEUVAGE_DECES);
				final CategorieEnvoiDI categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_SUISSE, contribuable, annee);
				return new PeriodeImposition(debutAssujettissement, finAssujettissement, categorie, contribuable, qualification, codeSegment, adresseRetour, optionnelle, false, causeFermeture);
			}

			return null; // pas d'envoi de DI dans ce cas
		}

		throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + assujettissement.getClass() + "].");
	}

	/**
	 * Détermine la catégorie d'envoi de DI associé à une période fiscale pour un contribuable imposé à l'ordinaire.
	 *
	 * @param typeContribuable le type de contribuable (imposition ordinaire)
	 * @param contribuable     le contribuable
	 * @param annee            la période fiscale considérée
	 * @return le type d'envoi de DI
	 */
	private CategorieEnvoiDI determineCategorieEnvoiDIOrdinaire(TypeContribuable typeContribuable, Contribuable contribuable, int annee) {
		final FormatDIOrdinaire formatDI = determineFormatDIOrdinaire(contribuable, annee);
		return CategorieEnvoiDI.ordinaireFor(typeContribuable, formatDI);
	}

	private static Qualification getQualification(Contribuable contribuable, int anneePrecedente) {
		Qualification qualification = null;
		DeclarationImpotOrdinaire precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			qualification = precedente.getQualification();
		}
		return qualification;
	}

	private static Integer getCodeSegment(Contribuable contribuable, int anneePrecedente) {
		Integer codeSegment = null;
		final DeclarationImpotOrdinaire precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			codeSegment = precedente.getCodeSegment();
		}
		return codeSegment;
	}

	private static DeclarationImpotOrdinaire getDeclarationPrecedente(Contribuable contribuable, int anneePrecedente) {
		DeclarationImpotOrdinaire precedenteDI = null;
		final List<Declaration> declarations = contribuable.getDeclarationsForPeriode(anneePrecedente, false);
		if (declarations != null && !declarations.isEmpty()) {
			Declaration precedenteDeclaration = declarations.get(declarations.size() - 1);
			if (precedenteDeclaration instanceof DeclarationImpotOrdinaire) {
				precedenteDI = (DeclarationImpotOrdinaire) precedenteDeclaration;
			}
		}
		return precedenteDI;
	}

	public static Qualification determineQualification(Contribuable contribuable, int annee) {

		Qualification qualification = getQualification(contribuable, annee - 1);
		if (qualification == null) {
			qualification = getQualification(contribuable, annee - 2);
		}

		return qualification;
	}

	public static Integer determineCodeSegment(Contribuable contribuable, int annee) {
		Integer codeSegment = getCodeSegment(contribuable, annee - 1);
		if (codeSegment == null) {
			codeSegment = getCodeSegment(contribuable, annee - 2);
		}
		return codeSegment;
	}

	/**
	 * [UNIREG-820] [UNIREG-1824] Détermine le format (VaudTax ou complète) pour une déclaration ordinaire
	 *
	 * @param contribuable un contribuable
	 * @param annee        l'année pour laquelle on veut déterminer le format de DI
	 * @return le format de DI à utiliser
	 */
	protected FormatDIOrdinaire determineFormatDIOrdinaire(Contribuable contribuable, int annee) {

		final List<DeclarationImpotOrdinaire> dis = getDeclarationsPourAnnees(contribuable, annee - 1, annee - 2);

		if (dis != null && !dis.isEmpty()) {
			// inspecte les déclarations des deux dernières années et retourne le type de la première déclaration ordinaire trouvée
			for (int i = dis.size() - 1; i >= 0; i--) {
				final DeclarationImpotOrdinaire di = dis.get(i);
				switch (di.getTypeDeclaration()) {
				case DECLARATION_IMPOT_VAUDTAX:
					return FormatDIOrdinaire.VAUDTAX;
				case DECLARATION_IMPOT_COMPLETE_LOCAL:
				case DECLARATION_IMPOT_COMPLETE_BATCH:
					return FormatDIOrdinaire.COMPLETE;
				}
			}

			// contribuable assujetti mais aucune information disponible sur les DIS -> déclaration complète par défaut
			return FormatDIOrdinaire.COMPLETE;
		}
		else {
			// s'il n'y a pas de déclaration, on retourne vaudtax sauf s'il était sourcier pur
			List<Assujettissement> assujetti;
			try {
				assujetti = assujettissementService.determine(contribuable, annee - 1);
			}
			catch (AssujettissementException e) {
				assujetti = null; // tant pis, on aura au moins essayé...
			}
			if (assujetti == null || (assujetti.get(assujetti.size() - 1) instanceof SourcierPur)) {
				// le contribuable est nouvellement assujetti -> VaudTax par défaut
				return FormatDIOrdinaire.VAUDTAX;
			}
			else {
				// le contribuable est déjà assujetti -> déclaration complète par défaut
				return FormatDIOrdinaire.COMPLETE;
			}
		}
	}

	/**
	 * Retourne les déclarations d'impôt ordinaires valides pour des années données.
	 *
	 * @param contribuable un contribuable
	 * @param annees       une ou plusieurs années
	 * @return les déclarations d'impôt ordinaires valides pour les années spécifiées, et triées par ordre croissant.
	 */
	private static List<DeclarationImpotOrdinaire> getDeclarationsPourAnnees(Contribuable contribuable, int... annees) {

		final List<DeclarationImpotOrdinaire> results = new ArrayList<DeclarationImpotOrdinaire>();

		final List<Declaration> declarations = contribuable.getDeclarationsSorted();
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				if (d.isAnnule()) {
					continue;
				}
				if (d instanceof DeclarationImpotOrdinaire) {
					final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) d;
					final int anneeDi = di.getPeriode().getAnnee();
					if (ArrayUtils.contains(annees, anneeDi)) {
						results.add(di);
					}
				}
			}
		}

		return results;
	}
}
