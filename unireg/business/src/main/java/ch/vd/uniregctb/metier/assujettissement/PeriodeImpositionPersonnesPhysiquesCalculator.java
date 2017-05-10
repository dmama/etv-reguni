package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;

public class PeriodeImpositionPersonnesPhysiquesCalculator implements PeriodeImpositionCalculator<ContribuableImpositionPersonnesPhysiques> {

	private final ParametreAppService parametreService;

	public PeriodeImpositionPersonnesPhysiquesCalculator(ParametreAppService parametreService) {
		this.parametreService = parametreService;
	}

	/**
	 * Point d'entrée du calcul des périodes d'imposition pour les contribuables assimilés "personne physique"
	 * @param contribuable contribuable cible
	 * @param assujettissements les assujettissements du contribuable, calculés par ailleurs
	 * @return la liste des périodes d'imposition du contribuable
	 */
	@NotNull
	@Override
	public List<PeriodeImposition> determine(ContribuableImpositionPersonnesPhysiques contribuable, List<Assujettissement> assujettissements) {

		// si pas d'assujettissement, pas de période d'imposition
		if (assujettissements == null || assujettissements.isEmpty()) {
			return Collections.emptyList();
		}

		// construisons les données utilisées plus loin
		final DonneesAssujettissement data = new DonneesAssujettissement(assujettissements);
		final int premiereAnnee = Math.max(data.getPremiereAnnee(), parametreService.getPremierePeriodeFiscalePersonnesPhysiques());
		final int derniereAnnee = data.getDerniereAnnee();

		// Détermination des périodes d'imposition sur toutes les années considérées
		final List<PeriodeImposition> list = new ArrayList<>();
		for (int annee = premiereAnnee; annee <= derniereAnnee; ++annee) {
			final DecompositionForsAnneeComplete fors = new DecompositionForsAnneeComplete(contribuable, annee);
			final List<PeriodeImpositionPersonnesPhysiques> l = determine(fors, data);
			if (l != null) {
				list.addAll(l);
			}
		}

		return list;
	}

	/**
	 * Container intelligent qui conserve l'intégralité des assujetissements d'une personne physique
	 * et est capable d'en sortir des extraits annuels (calculés une fois seulement)
	 */
	private static final class DonneesAssujettissement {

		/**
		 * La liste de tous les assujettissements (= données d'entrée), forcément non-vide, triée
		 */
		private final List<Assujettissement> tous;

		/**
		 * Les listes des assujettissements annuels (indexés par année, construites à la demande)
		 */
		private final Map<Integer, List<Assujettissement>> parAnnee = new HashMap<>();

		/**
		 * @param tous la liste de tous les assujettissements (= données d'entrée), triée si non-vide
		 */
		public DonneesAssujettissement(List<Assujettissement> tous) {
			this.tous = (tous == null ? Collections.emptyList() : tous);
		}

		/**
		 * @param annee une année de PF
		 * @return la liste des assujettissements relatifs à cette année (calculée une seule fois, puis conservée)
		 */
		public List<Assujettissement> forYear(int annee) {
			// déjà calculé ?
			if (parAnnee.containsKey(annee)) {
				return parAnnee.get(annee);
			}

			// pas encore calculé, c'est le moment
			final List<Assujettissement> extracted = AssujettissementHelper.extractYear(tous, annee);
			final List<Assujettissement> storedAndReturned = extracted.isEmpty() ? null : extracted;
			parAnnee.put(annee, storedAndReturned);     // pour l'éventuelle prochaine fois
			return storedAndReturned;
		}

		/**
		 * @return la première année (= la plus vieille) représentée dans les assujettissements
		 */
		public int getPremiereAnnee() {
			// tout assujettissement à toujours une date de début
			return tous.get(0).getDateDebut().year();
		}

		/**
		 * @return la dernière année (= la plus récente) représentée (ou l'année courante si l'assujettissement n'est pas fermé) dans les assujettissements
		 */
		public int getDerniereAnnee() {
			// un assujettissement n'a pas toujours une date de fin
			final Assujettissement dernier = tous.get(tous.size() - 1);
			return dernier.getDateFin() != null ? dernier.getDateFin().year() : RegDate.get().year();
		}
	}

	/**
	 * @param fors les fors d'une année fiscale
	 * @param data les données d'assujettissement complètes du contribuable
	 * @return les périodes d'imposition pour l'année des fors donnés
	 */
	@Nullable
	private List<PeriodeImpositionPersonnesPhysiques> determine(DecompositionForsAnneeComplete fors, DonneesAssujettissement data) {

		final List<Assujettissement> assujettissements = data.forYear(fors.annee);

		// le contribuable n'est pas assujetti cette année-là
		if (assujettissements == null || assujettissements.isEmpty()) {
			return null;
		}

		// On retourne tous les ranges qui ne sont pas associés avec une déclaration
		final List<PeriodeImpositionPersonnesPhysiques> periodes = new ArrayList<>();
		for (Assujettissement a : assujettissements) {
			final PeriodeImpositionPersonnesPhysiques periode = determinePeriodeImposition(fors, a, data);
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
	 * [SIFISC-23095] Depuis l'intronisation de la "nouvelle entité", les décédés doivent avoir par défaut une adresse de retour en CEDI (= nouvelle entité)
	 * @param a un assujettissement
	 * @return l'adresse de retour correspondante.
	 */
	private static TypeAdresseRetour determineAdresseRetour(Assujettissement a) {
		if (a.getMotifFractFin() == MotifAssujettissement.VEUVAGE_DECES) {
			return TypeAdresseRetour.CEDI;
		}
		if (a instanceof VaudoisDepense) {
			return TypeAdresseRetour.OID;
		}
		if (a instanceof HorsCanton && !a.getFors().secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
			return TypeAdresseRetour.OID;
		}
		return TypeAdresseRetour.CEDI;
	}

	@Nullable
	private static PeriodeImposition.CauseFermeture getCauseFermeture(MotifAssujettissement motifFractionnement, boolean isAssujettissementHS) {
		final PeriodeImposition.CauseFermeture cause;
		if (motifFractionnement == null) {
			cause = null;
		}
		else {
			switch (motifFractionnement) {
			case VEUVAGE_DECES:
				cause = PeriodeImposition.CauseFermeture.VEUVAGE_DECES;
				break;

			case VENTE_IMMOBILIER:
			case FIN_EXPLOITATION:
				cause = isAssujettissementHS ? PeriodeImposition.CauseFermeture.FIN_ASSUJETTISSEMENT_HS : PeriodeImposition.CauseFermeture.AUTRE;
				break;

			default:
				cause = PeriodeImposition.CauseFermeture.AUTRE;
				break;
			}
		}
		return cause;
	}

	/**
	 * Point d'entrée un peu moins officiel, pour pouvoir assigner une période d'imposition à un assujettissement particulier sur une année donnée
	 * @param fors les fors d'une année
	 * @param assujettissement un assujettissement (qui couvre l'année au moins partiellement...)
	 * @param tous les assujettissements complets du contribuable
	 * @return la période d'imposition trouvée (ou <code>null</code> s'il n'y en a pas)
	 */
	@Nullable
	public PeriodeImpositionPersonnesPhysiques determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement, List<Assujettissement> tous) {
		final DonneesAssujettissement data = new DonneesAssujettissement(tous);
		return determinePeriodeImposition(fors, assujettissement, data);
	}

	/**
	 * Coeur du calcul, identification d'une période d'imposition qui correspond à l'assujettissement donnée dans l'année donnée
	 * @param fors les fors d'une année
	 * @param assujettissement un assujettissement (qui couvre l'année au moins partiellement...)
	 * @param data les données d'assujettissement complètes du contribuable (<code>null</code> implique qu'on n'aura accès ici à aucun autre assujettissement)
	 * @return la période d'imposition trouvée (ou <code>null</code> s'il n'y en a pas)
	 */
	@Nullable
	private PeriodeImpositionPersonnesPhysiques determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement, DonneesAssujettissement data) {

		final ContribuableImpositionPersonnesPhysiques contribuable = (ContribuableImpositionPersonnesPhysiques) assujettissement.getContribuable();
		final RegDate debutAssujettissement = assujettissement.getDateDebut();
		final RegDate finAssujettissement = assujettissement.getDateFin();
		final int annee = finAssujettissement.year();
		final PeriodeImpositionPersonnesPhysiques.CauseFermeture causeFermeture = getCauseFermeture(assujettissement.getMotifFractFin(), assujettissement instanceof HorsSuisse);

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
				final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.DIPLOMATE_SUISSE, contribuable, annee, data);
				final boolean optionnelle = (assujettissement.getMotifFractFin() != MotifAssujettissement.VENTE_IMMOBILIER && assujettissement.getMotifFractFin() != MotifAssujettissement.VEUVAGE_DECES);
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, optionnelle, false, causeFermeture, codeSegment, categorie, adresseRetour);
			}
			else {
				// Les diplomates Suisses basés à l'étranger ne reçoivent pas de déclaration, mais la période d'imposition existe bel et bien.
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, false, causeFermeture, null, CategorieEnvoiDIPP.DIPLOMATE_SUISSE, null);
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

			if (mixte.getMotifFractFin() == MotifAssujettissement.DEPART_HC && !is31Decembre(mixte.getDateFin())) {
				// [UNIREG-1742] Cas des contribuables imposés selon le mode mixte, partis dans un autre canton durant l’année et n’ayant aucun
				// rattachement économique -> bien qu’ils soient assujettis de manière illimitée jusqu'au dernier jour du mois de leur départ,
				// leur déclaration d’impôt est remplacée (= elle est optionnelle, en fait, voir exemples à la fin de la spécification) par une
				// note à l’administration fiscale cantonale de leur domicile.
				// [UNIREG-2328] A noter que cela ne s'applique pas aux arrivées de hors-canton : dans ce cas l'administration fiscale responsable est l'administration vaudoise.
				// [SIFISC-62] Dorénavant, cette règle s'applique aussi au sourciers mixte 137 Al.1
				optionnelle = true;
				remplaceeParNote = true;
			}

			if (mixte.getTypeAutoriteFiscalePrincipale() == TypeAutoriteFiscale.COMMUNE_HC) {
				if (forsPeriode.secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
					// Sourcier mixte hc avec activité indépendante => déclaration ordinaire
					final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_CANTON, contribuable, annee, data);
					return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, optionnelle, remplaceeParNote, causeFermeture, codeSegment, categorie, adresseRetour);
				}
				else {
					// Sourciers mixtes hors-canton avec immeuble => déclaration HC immeuble
					return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, false, causeFermeture, codeSegment, CategorieEnvoiDIPP.HC_IMMEUBLE, TypeAdresseRetour.ACI);
				}
			}
			else {
				// Sourcier mixte vaudois => déclaration ordinaire
				final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.VAUDOIS_ORDINAIRE, contribuable, annee, data);
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, optionnelle, remplaceeParNote, causeFermeture, codeSegment, categorie, adresseRetour);
			}
		}

		/*
		 * Vaudois à la dépense
		 */
		if (assujettissement instanceof VaudoisDepense) {
			return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, false, causeFermeture, codeSegment, CategorieEnvoiDIPP.VAUDOIS_DEPENSE, adresseRetour);
		}

		/*
		 * Vaudois ordinaire (ou indigent, pas de différence de traitement ici : c'est lors de l'envoi des DIs qu'ils seron traités différement)
		 */
		if (assujettissement instanceof VaudoisOrdinaire || assujettissement instanceof Indigent) {
			// Vaudois ordinaire => déclaration ordinaire
			final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.VAUDOIS_ORDINAIRE, contribuable, annee, data);
			return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, false, causeFermeture, codeSegment, categorie, adresseRetour);
		}

		/*
		 * Hors canton
		 */
		if (assujettissement instanceof HorsCanton) {

			boolean remplaceeParNote = false;

			if (assujettissement.getMotifFractFin() == MotifAssujettissement.VENTE_IMMOBILIER || assujettissement.getMotifFractFin() == MotifAssujettissement.FIN_EXPLOITATION) {
				// [UNIREG-1742] dans le cas des contribuables domiciliés dans un autre canton dont le rattachement économique (activité indépendante ou immeuble)
				// s’est terminé au cours de la période fiscale, la déclaration est remplacée par une note à l'administration fiscale de l'autre canton.
				remplaceeParNote = true;
			}

			if (causeFermeture == PeriodeImposition.CauseFermeture.VEUVAGE_DECES) {
				final ForFiscalSecondaire dernierForSecondaire = fors.secondairesDansLaPeriode.last();
				if (dernierForSecondaire != null && dernierForSecondaire.getDateFin() != null && dernierForSecondaire.getDateFin().isBefore(finAssujettissement)) {
					// [SIFISC-7636] en cas de décès, la déclaration est remplacée par une note uniquement si le dernier for secondaire a été fermé *avant* le décès
					remplaceeParNote = true;
				}
			}

			// [UNIREG-1360] Pour un contribuable hors canton, la période d'imposition est toujours égale à la période d'assujettissement
			// (qui elle-même est égale à l'année complète, sauf en cas de fractionnement de l'assujettissement). Pour autant qu'il ait des
			// fors secondaires (ce qui est forcément le cas, puisqu'il est assujetti).

			if (fors.secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
				// Activité indépendante dans le canton => déclaration ordinaires
				final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_CANTON, contribuable, annee, data);
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, remplaceeParNote, causeFermeture, codeSegment, categorie, adresseRetour);
			}
			else {
				// Immeuble dans le canton => déclaration HC immeuble
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, remplaceeParNote, causeFermeture, codeSegment, CategorieEnvoiDIPP.HC_IMMEUBLE, adresseRetour);
			}
		}

		/*
		 * Hors Suisse
		 */
		if (assujettissement instanceof HorsSuisse) {
			// [UNIREG-1742] pour les hors-Suisse, la période d'imposition corresponds simplement à la période d'assujettissement (confirmé par Thierry Declercq le 18 décembre 2009)

			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
				// Activité indépendante dans le canton => déclaration ordinaire
				final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_SUISSE, contribuable, annee, data);
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, false, causeFermeture, codeSegment, categorie, adresseRetour);
			}

			final ForFiscalPrincipal dernierPrincipal = assujettissement.getFors().principauxDansLaPeriode.last();
			if (dernierPrincipal.getMotifRattachement() == MotifRattachement.DIPLOMATE_ETRANGER) {
				// Fonctionnaire international ou diplomate étranger propriétaire d'immeuble dans le canton => déclaration ordinaire
				final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_SUISSE, contribuable, annee, data);
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, false, false, causeFermeture, codeSegment, categorie, adresseRetour);
			}

			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
				// [UNIREG-1742] Les contribuables domiciliées à l'étranger assujettis à raison d'une propriété d'immeuble [...] sont imposés
				// selon un mode forfaitaire et *peuvent* recevoir une déclaration d'impôt à leur demande (dès l’année d’acquisition du 1er immeuble),
				// mais n’en bénéficient *plus* l’année de la vente du dernier immeuble ou du décès.
				final boolean optionnelle = (assujettissement.getMotifFractFin() != MotifAssujettissement.VENTE_IMMOBILIER && assujettissement.getMotifFractFin() != MotifAssujettissement.VEUVAGE_DECES);
				final CategorieEnvoiDIPP categorie = determineCategorieEnvoiDIOrdinaire(TypeContribuable.HORS_SUISSE, contribuable, annee, data);
				return new PeriodeImpositionPersonnesPhysiques(debutAssujettissement, finAssujettissement, contribuable, optionnelle, false, causeFermeture, codeSegment, categorie, adresseRetour);
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
	 * @param data             les données d'assujettissement complètes du contribuable
	 * @return le type d'envoi de DI
	 */
	private CategorieEnvoiDIPP determineCategorieEnvoiDIOrdinaire(TypeContribuable typeContribuable, ContribuableImpositionPersonnesPhysiques contribuable, int annee, DonneesAssujettissement data) {
		final FormatDIOrdinaire formatDI = determineFormatDIOrdinaire(contribuable, annee, data);
		return CategorieEnvoiDIPP.ordinaireFor(typeContribuable, formatDI);
	}

	private static Qualification getQualification(ContribuableImpositionPersonnesPhysiques contribuable, int anneePrecedente) {
		Qualification qualification = null;
		DeclarationImpotOrdinairePP precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			qualification = precedente.getQualification();
		}
		return qualification;
	}

	private static Integer getCodeSegment(ContribuableImpositionPersonnesPhysiques contribuable, int anneePrecedente) {
		Integer codeSegment = null;
		final DeclarationImpotOrdinairePP precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			codeSegment = precedente.getCodeSegment();
		}
		return codeSegment;
	}

	private static List<DeclarationImpotOrdinairePP> getDeclarationsDansPeriode(ContribuableImpositionPersonnesPhysiques ctb, int... pf) {
		final List<DeclarationImpotOrdinairePP> declarations = ctb.getDeclarationsTriees(DeclarationImpotOrdinairePP.class, false);
		final List<DeclarationImpotOrdinairePP> dis = new ArrayList<>(declarations.size());
		if (pf != null && pf.length > 0) {
			for (DeclarationImpotOrdinairePP declaration : declarations) {
				if (ArrayUtils.contains(pf, declaration.getPeriode().getAnnee())) {
					dis.add(declaration);
				}
			}
		}
		return dis.isEmpty() ? Collections.emptyList() : dis;
	}

	private static DeclarationImpotOrdinairePP getDeclarationPrecedente(ContribuableImpositionPersonnesPhysiques contribuable, int anneePrecedente) {
		DeclarationImpotOrdinairePP precedenteDI = null;
		final List<DeclarationImpotOrdinairePP> declarations = getDeclarationsDansPeriode(contribuable, anneePrecedente);
		if (declarations != null && !declarations.isEmpty()) {
			precedenteDI = declarations.get(declarations.size() - 1);
		}
		return precedenteDI;
	}

	public static Qualification determineQualification(ContribuableImpositionPersonnesPhysiques contribuable, int annee) {

		Qualification qualification = getQualification(contribuable, annee - 1);
		if (qualification == null) {
			qualification = getQualification(contribuable, annee - 2);
		}

		return qualification;
	}

	public static Integer determineCodeSegment(ContribuableImpositionPersonnesPhysiques contribuable, int annee) {
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
	 * @param data         les données d'assujettissement complètes du contribuable
	 * @return le format de DI à utiliser
	 */
	protected FormatDIOrdinaire determineFormatDIOrdinaire(ContribuableImpositionPersonnesPhysiques contribuable, int annee, DonneesAssujettissement data) {

		final List<DeclarationImpotOrdinairePP> dis = getDeclarationsDansPeriode(contribuable, annee - 1, annee - 2);

		if (dis != null && !dis.isEmpty()) {
			// inspecte les déclarations des deux dernières années et retourne le type de la première déclaration ordinaire trouvée
			for (int i = dis.size() - 1; i >= 0; i--) {
				final DeclarationImpotOrdinairePP di = dis.get(i);
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
			final List<Assujettissement> assujetti = data.forYear(annee - 1);
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

	private static boolean is31Decembre(RegDate date) {
		return date.month() == 12 && date.day() == 31;
	}
}
