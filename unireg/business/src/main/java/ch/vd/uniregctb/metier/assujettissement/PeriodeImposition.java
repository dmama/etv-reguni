package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.*;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.type.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe de base abstraite représentant une période d'imposition.
 * <p>
 * <b>Note:</b> la différence entre une période d'imposition et un assujettissement (voir {@link Assujettissement}) est subtile, mais bien
 * réelle.
 * <ul>
 * <li>Un <b>assujettissement</b> représente le type de contribuable tel que calculé en appliquant les règles fiscales au plus près de leurs
 * définition, sans tenir compte du contexte d'utilisation.</li>
 * <li>Une <b>période d'imposition</b> est une notion qui représente la période et le type de contribuable, mais limité à une période
 * fiscale et en tenant compte d'un context orienté "déclaration d'impôt".</li>
 * </ul>
 *
 * Dans la plupart des cas, les deux notions sont confondues; mais pas dans tous.
 * <p>
 * <b>Exemple où les deux notions sont différentes:</b> le cas du contribuable vaudois qui part hors-Suisse en gardant un immeuble dans le
 * canton. Dans ce cas, on a:
 * <ul>
 * <li><b>assujettissements:</b> un assujettissement VaudoisOrdinaire pour la période du 1er janvier à la date du départ; et un second
 * assujettissement HorsSuisse pour le reste de l'année.</li>
 * <li><b>période d'imposition:</b> une seule période couvrant toute l'année avec le type de document DECLARATION_IMPOT_COMPLETE car les
 * deux assujettissements VaudoisOrdinaire et HorsSuisse génèrent le même type de DI au final.</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PeriodeImposition implements CollatableDateRange {

	private final RegDate debut;
	private final RegDate fin;
	private final TypeContribuableDI typeContribuableDI;
	private final Contribuable contribuable;
	private final Qualification qualification;
	private final TypeAdresseRetour adresseRetour; // [UNIREG-1741]

	/**
	 * <code>vrai</code> si la période d'imposition est optionnelle (= une DI n'est émise que sur demande du contribuable).
	 */
	private final boolean optionnelle;

	/**
	 * <code>vrai</code> si la déclaration correspondante à la période d'imposition est remplacée par une note à l'administration fiscale d'une autre canton (= la DI n'est émise).
	 */
	private final boolean remplaceeParNote;

	/**
	 * Détermine la liste des périodes d'imposition durant l'année spécifiée.
	 * <p>
	 * Cette méthode appelle la méthode {@link Assujettissement#determine(Contribuable, int)} et applique les règles métier pour en déduire
	 * les périodes d'imposition.
	 *
	 * @param contribuable
	 *            le contribuable dont on veut déterminer l'assujettissement
	 * @param annee
	 *            l'année correspondant à la période fiscale considérée (du 1er janvier au 31 décembre)
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	public static List<PeriodeImposition> determine(Contribuable contribuable, int annee) throws AssujettissementException {
		return determine(new DecompositionForsAnneeComplete(contribuable, annee));
	}

	/**
	 * Détermine la liste des périodes d'imposition durant la période spécifiée.
	 * <p>
	 * Cette méthode fonctionne en calculant les périodes d'imposition année après année et en ajoutant les résultats l'un après l'autre.
	 * Elle n'est donc pas terriblement efficace, et dans la mesure du possible préférer la méthode {@link #determine(Contribuable, int)}.
	 *
	 * @param contribuable
	 *            le contribuable dont on veut déterminer l'assujettissement
	 * @param range
	 *            la période considérée
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	public static List<PeriodeImposition> determine(Contribuable contribuable, DateRange range) throws AssujettissementException {
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

	/**
	 * Détermine la liste des périodes d'imposition durant l'année spécifiée.
	 * <p>
	 * Cette méthode appelle la méthode {@link Assujettissement#determine(DecompositionForsAnneeComplete)} et applique les règles métier
	 * pour en déduire les périodes d'imposition.
	 *
	 * @param fors
	 *            la décomposition des fors précalculée par l'année considérée
	 * @return une liste de périodes d'imposition contenant 1 ou plusieurs entrées, ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException s'il n'est pas possible de détermine l'assujettissement.
	 */
	public static List<PeriodeImposition> determine(DecompositionForsAnneeComplete fors) throws AssujettissementException {

		// on calcul l'assujettissement complet du contribuable
		final List<Assujettissement> assujettissements = Assujettissement.determine(fors.contribuable, fors.annee);

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

	/**
	 * Détermine la période d'imposition pour un assujettissement particulier.
	 *
	 * @param fors
	 *            la décomposition des fors duquel est issu l'assujettissement
	 * @param assujettissement
	 *            l'assujettissement dont on veut déterminer la période d'imposition
	 * @return une période d'imposition; ou <b>null</b> si le contribuable ne reçoit pas de déclaration d'impôt.
	 */
	public static PeriodeImposition determinePeriodeImposition(DecompositionForsAnneeComplete fors, Assujettissement assujettissement) {

		final Contribuable contribuable = assujettissement.getContribuable();

		/*
		 * Diplomate Suisse
		 */
		if (assujettissement instanceof DiplomateSuisse) {
			// Les diplomates Suisses basés à l'étranger ne reçoivent pas de déclaration, mais la période d'imposition existe bel et bien.
			return new PeriodeImposition(assujettissement.getDateDebut(), assujettissement.getDateFin(), TypeContribuableDI.DIPLOMATE_SUISSE, assujettissement.getContribuable(), null, null, false,
					false);
		}

		/*
		 * Sourcier pur
		 */
		if (assujettissement instanceof SourcierPur) {
			// Les sourciers purs sont perçus à la source uniquement et ne reçoivent donc pas de déclaration
			return null;
		}

		final Qualification qualification = determineQualification(contribuable,  assujettissement.getDateFin().year());
		final TypeAdresseRetour adresseRetour = determineAdresseRetour(assujettissement);

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

			if (mixte.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC && !forsPeriode.secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
				// Cas des sourciers mixtes hors-canton avec immeuble -> ils doivent recevoir une déclaration HC immeuble (pour autant qu'il n'aient pas d'activité indépendante)
				return new PeriodeImposition(assujettissement.getDateDebut(), assujettissement.getDateFin(), TypeContribuableDI.HORS_CANTON, assujettissement.getContribuable(), qualification,
						TypeAdresseRetour.ACI);
			}

			// [UNIREG-1824] faut déterminer aussi le type de DI dans le cas des contribuables mixte
			final TypeContribuableDI type = determineTypeDI(contribuable, assujettissement.getDateFin().year());

			return new PeriodeImposition(assujettissement.getDateDebut(), assujettissement.getDateFin(), type, assujettissement.getContribuable(), qualification, adresseRetour, optionnelle, remplaceeParNote);
		}

		/*
		 * Vaudois à la dépense
		 */
		if (assujettissement instanceof VaudoisDepense) {
			return new PeriodeImposition(assujettissement.getDateDebut(), assujettissement.getDateFin(), TypeContribuableDI.VAUDOIS_DEPENSE, assujettissement.getContribuable(), qualification,
					adresseRetour);
		}

		/*
		 * Vaudois ordinaire (ou indigent, pas de différence de traitement ici : c'est lors de l'envoi des DIs qu'ils seron traités différement)
		 */
		if (assujettissement instanceof VaudoisOrdinaire || assujettissement instanceof Indigent) {

			/*
			 * On choisi entre ordinaire et VaudTax : si la dernière déclaration était une VaudTax, on continue en VaudTax. Dans tous les
			 * autres cas, on est en ordinaire
			 */

			TypeContribuableDI type = determineTypeDI(contribuable, assujettissement.getDateFin().year());

			return new PeriodeImposition(assujettissement.getDateDebut(), assujettissement.getDateFin(), type, assujettissement.getContribuable(), qualification, adresseRetour);
		}

		/*
		 * Hors canton
		 */
		if (assujettissement instanceof HorsCanton) {

			boolean remplaceeParNote = false;

			if (assujettissement.getMotifFractFin() == MotifFor.VENTE_IMMOBILIER || assujettissement.getMotifFractFin() == MotifFor.FIN_EXPLOITATION ||
					assujettissement.getMotifFractFin() == MotifFor.VEUVAGE_DECES) {
				// [UNIREG-1742] dans le cas des contribuables domiciliés dans un autre canton dont le rattachement économique (activité indépendante ou immeuble)
				// s’est terminé au cours de la période fiscale, la déclaration est remplacée par une note à l'administration fiscale de l'autre canton.
				remplaceeParNote = true;
			}

			// [UNIREG-1360] Pour un contribuable hors canton, la période d'imposition est toujours égale à la période d'assujettissement
			// (qui elle-même est égale à l'année complète, sauf en cas de fractionnement de l'assujettissement). Pour autant qu'il ait des
			// fors secondaires (ce qui est forcément le cas, puisqu'il est assujetti).
			final RegDate dateDebut = assujettissement.getDateDebut();
			final RegDate dateFin = assujettissement.getDateFin();

			if (fors.secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {

				// Activité indépendante dans le canton -> (plus d')imposition ordinaires dans tous les cas
				// [UNIREG-1824] les contribuables hors-canton ne seront plus vaudois ordinaires par défaut, il faut déterminer le type de DI en se basant sur les précédentes
				TypeContribuableDI type = determineTypeDI(contribuable, assujettissement.getDateFin().year());

				return new PeriodeImposition(dateDebut, dateFin, type, assujettissement.getContribuable(), qualification, adresseRetour, false, remplaceeParNote);
			}

			return new PeriodeImposition(dateDebut, dateFin, TypeContribuableDI.HORS_CANTON, assujettissement.getContribuable(), qualification, adresseRetour, false, remplaceeParNote);
		}

		/*
		 * Hors Suisse
		 */
		if (assujettissement instanceof HorsSuisse)
		{
			// [UNIREG-1742] pour les hors-Suisse, la période d'imposition corresponds simplement à la période d'assujettissement (confirmé par Thierry Declercq le 18 décembre 2009)
			final RegDate dateDebut = assujettissement.getDateDebut();
			final RegDate dateFin = assujettissement.getDateFin();

			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.ACTIVITE_INDEPENDANTE)) {
				// Activité indépendante dans le canton -> (plus d')imposition ordinaires dans tous les cas
				
				// [UNIREG-1824] les contribuables hors-suisse avec une activité independante ne seront plus vaudois ordinaires par défaut, 
				//               il faut déterminer le type de DI en se basant sur les précédentes
				TypeContribuableDI type = determineTypeDI(contribuable, assujettissement.getDateFin().year());
				
				return new PeriodeImposition(dateDebut, dateFin, type, assujettissement.getContribuable(), qualification, adresseRetour);
			}

			final ForFiscalPrincipal dernierPrincipal = assujettissement.getFors().principauxDansLaPeriode.last();
			if (dernierPrincipal.getMotifRattachement().equals(MotifRattachement.DIPLOMATE_ETRANGER)) {
				// Cas du fonctionnaire international ou diplomate étranger propriétaire d'immeuble dans le canton
				
				// [UNIREG-1824] les contribuables hors-canton diplomates étrangers, il faut déterminer le type de DI en se basant sur les précédentes
				TypeContribuableDI type = determineTypeDI(contribuable, assujettissement.getDateFin().year());
				
				return new PeriodeImposition(dateDebut, dateFin, type, assujettissement.getContribuable(), qualification, adresseRetour);
			}

			if (assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
				// [UNIREG-1742] Les contribuables domiciliées à l'étranger assujettis à raison d'une propriété d'immeuble [...] sont imposés
				// selon un mode forfaitaire et *peuvent* recevoir une déclaration d'impôt à leur demande (dès l’année d’acquisition du 1er immeuble),
				// mais n’en bénéficient *plus* l’année de la vente du dernier immeuble ou du décès.
				final boolean optionnelle = (assujettissement.getMotifFractFin() != MotifFor.VENTE_IMMOBILIER && assujettissement.getMotifFractFin() != MotifFor.VEUVAGE_DECES);
				return new PeriodeImposition(dateDebut, dateFin, TypeContribuableDI.HORS_SUISSE, assujettissement.getContribuable(), qualification, adresseRetour, optionnelle, false);
			}

			return null; // pas d'envoi de DI dans ce cas
		}

		throw new IllegalArgumentException("Type d'assujettissement inconnu = [" + assujettissement.getClass() + "].");
	}


	private static Qualification getQualification(Contribuable contribuable, int anneePrecedente) {
		Qualification qualification = null;
		DeclarationImpotOrdinaire precedente = getDeclarationPrecedente(contribuable, anneePrecedente);
		if (precedente != null) {
			qualification  = precedente.getQualification();
		}
		return qualification;
	}

	private static DeclarationImpotOrdinaire getDeclarationPrecedente(Contribuable contribuable, int anneePrecedente) {
		DeclarationImpotOrdinaire precedenteDI = null;
		final List<Declaration> declarations = contribuable.getDeclarationForPeriode(anneePrecedente);
		if (declarations != null && !declarations.isEmpty()) {
			Declaration precedenteDeclaration =  declarations.get(declarations.size() - 1);
			if (precedenteDeclaration instanceof DeclarationImpotOrdinaire) {
				precedenteDI = (DeclarationImpotOrdinaire) precedenteDeclaration;
			}
		}
		return precedenteDI;
	}


	private static TypeContribuableDI getTypeDI(Contribuable contribuable, int anneePrecedente) {

		TypeContribuableDI type = null;
		DeclarationImpotOrdinaire precedente = getDeclarationPrecedente(contribuable, anneePrecedente);

		if (precedente != null) {
			if (TypeDocument.DECLARATION_IMPOT_VAUDTAX.equals(precedente.getTypeDeclaration())) {
				type  = TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX;
			}
			else {
				type  = TypeContribuableDI.VAUDOIS_ORDINAIRE;
			}
		}
		else {
			List<Assujettissement> assujetti;
			try {
				assujetti = Assujettissement.determine(contribuable, anneePrecedente);
			}
			catch (AssujettissementException e) {
				assujetti = null; // tant pis, on aura au moins essayé...
			}
			if (assujetti == null || (assujetti.get(assujetti.size() - 1) instanceof SourcierPur)) {
				type  = TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX;
			}
		}

		return type;
	}

	public static Qualification determineQualification(Contribuable contribuable, int annee) {

		Qualification qualification = getQualification(contribuable, annee - 1);
		if (qualification == null) {
			qualification = getQualification(contribuable, annee - 2);
		}

		return qualification;
	}

	private static TypeContribuableDI determineTypeDI(Contribuable contribuable, int annee) {

		TypeContribuableDI type = getTypeDI(contribuable, annee - 1);
		if (type == null) {
			type = getTypeDI(contribuable, annee - 2);
			if (type == null) {
				type  = TypeContribuableDI.VAUDOIS_ORDINAIRE;
			}
		}

		return type;
	}

	private PeriodeImposition(RegDate dateDebut, RegDate dateFin, TypeContribuableDI typeCtbDoc, Contribuable contribuable, Qualification qualification, TypeAdresseRetour adresseRetour) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.debut = dateDebut;
		this.fin = dateFin;
		this.typeContribuableDI = typeCtbDoc;
		this.contribuable = contribuable;
		this.qualification = qualification;
		this.adresseRetour = adresseRetour;
		this.optionnelle = false;
		this.remplaceeParNote = false;
	}

	private PeriodeImposition(RegDate dateDebut, RegDate dateFin, TypeContribuableDI typeCtbDoc, Contribuable contribuable, Qualification qualification, TypeAdresseRetour adresseRetour,
	                          boolean optionnelle, boolean remplaceeParNote) {
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
		this.debut = dateDebut;
		this.fin = dateFin;
		this.typeContribuableDI = typeCtbDoc;
		this.contribuable = contribuable;
		this.qualification = qualification;
		this.adresseRetour = adresseRetour;
		this.optionnelle = optionnelle;
		this.remplaceeParNote = remplaceeParNote;
	}

	public RegDate getDateDebut() {
		return debut;
	}

	public RegDate getDateFin() {
		return fin;
	}

	public TypeContribuableDI getTypeContribuableDI() {
		return typeContribuableDI;
	}

	public TypeContribuable getTypeContribuable() {
		return typeContribuableDI.getTypeContribuable();
	}

	public TypeDocument getTypeDocument() {
		return typeContribuableDI.getTypeDocument();
	}

	public boolean isOptionnelle() {
		return optionnelle;
	}

	public boolean isRemplaceeParNote() {
		return remplaceeParNote;
	}

	public boolean isDiplomateSuisse() {
		return typeContribuableDI == TypeContribuableDI.DIPLOMATE_SUISSE;
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	public Qualification getQualification() {
		return qualification;
	}

	public TypeAdresseRetour getAdresseRetour() {
		return adresseRetour;
	}

	public DateRange collate(DateRange n) {
		final PeriodeImposition next = (PeriodeImposition) n;
		final TypeAdresseRetour adresseRetour = next.adresseRetour; // [UNIREG-1741] en prenant le second type, on est aussi correct en cas de décès. 
		Assert.isTrue(isCollatable(next));
		return new PeriodeImposition(debut, next.getDateFin(), collateTypeCtbDoc(typeContribuableDI, next.typeContribuableDI), contribuable, qualification, adresseRetour, optionnelle && next.optionnelle,
				remplaceeParNote && next.remplaceeParNote);
	}

	public boolean isCollatable(DateRange n) {
		final PeriodeImposition next = (PeriodeImposition) n;
		return isEquivalent(typeContribuableDI, next.typeContribuableDI) && isRangeCollatable(next);
	}

	private boolean isRangeCollatable(final PeriodeImposition next) {
		// on accepte les ranges qui se touchent *et* ceux qui se chevauchent, ceci parce que les périodes d'impositions peuvent être plus
		// larges que les assujettissement sous-jacents (cas des HorsCanton et HorsSuisse) et qu'il s'agit de pouvoir les
		// collater malgré tout.
		return this.getDateFin() != null && this.getDateFin().getOneDayAfter().isAfterOrEqual(next.getDateDebut());
	}

	/**
	 * @param left le type de gauche
	 * @param right le type de droite
	 * @return <b>vrai</b> si les deux types de documents sont égaux, en <b>ne faisant pas</b> de différence entre
	 *         DECLARATION_IMPOT_COMPLETE et DECLARATION_IMPOT_VAUDTAX.
	 */
	private boolean isEquivalent(TypeContribuableDI left, TypeContribuableDI right) {
		return (left == right) || (isCompleteOuVaudTax(left.getTypeDocument()) && isCompleteOuVaudTax(right.getTypeDocument()));
	}

	private boolean isCompleteOuVaudTax(TypeDocument doc) {
		return TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH.equals(doc) || TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL.equals(doc) || TypeDocument.DECLARATION_IMPOT_VAUDTAX.equals(doc);
	}

	private TypeContribuableDI collateTypeCtbDoc(TypeContribuableDI left, TypeContribuableDI right) {
		// Dans la plupart des cas, on prend le type de contribuable le plus à jour, c'est-à-dire la valeur 'right'.
		// Sauf lorsque on a le choix entre en déclaration d'impôt vaudtax et une complète; dans ce cas on préfère la vaudtax.
		if (left == TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX && right == TypeContribuableDI.VAUDOIS_ORDINAIRE) {
			return TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX;
		}
		else {
			return right;
		}
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, debut, fin, NullDateBehavior.LATEST);
	}
}
