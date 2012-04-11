package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.Triplet;
import ch.vd.uniregctb.common.TripletIterator;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.ValidationService;

/**
 * Implémentation du service de détermination de l'assujettissement d'un contribuable à partir de ses fors fiscaux.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class AssujettissementServiceImpl implements AssujettissementService {

	private ValidationService validationService = null;

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	private static final Adapter adapter = new Adapter();

	@Override
	public List<Assujettissement> determine(Contribuable ctb) throws AssujettissementException {

		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			return null;
		}

		return determine(ctb, fpt, null);
	}

	@Override
	public List<Assujettissement> determinePourCommunes(Contribuable ctb, Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		if (ctb.isAnnule()) {
			// un contribuable annulé n'est évidemment pas assujetti
			return null;
		}

		final ForsParType fpt = ctb.getForsParType(true);
		if (fpt.isEmpty()) {
			return null;
		}

		// l'assujettissement ne considère que les fors principaux et secondaires pour le moment
		// (peut-être un jour y aura-t-il aussi les fors autres éléments imposables)

		return determine(ctb, fpt, noOfsCommunesVaudoises);
	}

	private List<Assujettissement> determine(Contribuable ctb, ForsParType fors, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		try {
			ajouteForsPrincipauxFictifs(fors.principaux);

			// Détermination des données d'assujettissement brutes
			final List<Fraction> fractionnements = determineFractionnements(fors.principaux);
			final CasParticuliers casParticuliers = determineCasParticuliers(ctb, fors.principaux);

			final DataList domicile = determineAssujettissementDomicile(fors.principaux, fractionnements, casParticuliers, noOfsCommunesVaudoises);
			domicile.compacterNonAssujettissements(noOfsCommunesVaudoises != null); // SIFISC-2939
			assertCoherenceRanges(domicile);

			final List<Data> economique = determineAssujettissementEconomique(fors.secondaires, fractionnements, noOfsCommunesVaudoises);
			fusionne(domicile, economique);

			// Création des assujettissements finaux
			List<Assujettissement> assujettissements = instanciate(ctb, domicile);
			assujettissements = DateRangeHelper.collate(assujettissements);
			adapteDatesDebutEtFin(assujettissements);

			assertCoherenceRanges(assujettissements);

			return assujettissements.isEmpty() ? null : assujettissements;
		}
		catch (AssujettissementException e) {
			if (validationService != null && !validationService.isInValidation()) { // on évite les appels récursifs
				final ValidationResults vr = validationService.validate(ctb);
				if (vr.hasErrors()) {
					// si le contribuable ne valide pas, on est un peu plus explicite
					throw new AssujettissementException("Une exception a été levée sur le contribuable n°" + ctb.getNumero() +
							" lors du calcul des assujettissements, mais en fait le contribuable ne valide pas: " + vr.toString(), e);
				}
			}

			// autrement, on propage simplement l'exception
			throw e;
		}
	}

	/**
	 * Cette méthode ajoute des fors fiscaux principaux fictifs, c'est-à-dire des fors qui devraient exister en fonction des caractéristiques des fors existants, mais qui manquent.
	 *
	 * @param forsPrincipaux les fors principaux (non-fictifs) du contribuable.
	 */
	private static void ajouteForsPrincipauxFictifs(List<ForFiscalPrincipal> forsPrincipaux) {

		List<ForFiscalPrincipal> forsFictifs = null;

		// [UNIREG-2444] si un for fiscal principal possède un motif d'ouverture d'obtention de permis C et qu'il n'y a pas de for fiscal principal précédent,
		// on suppose que le contribuable était sourcier et qu'il y a passage du rôle source pur au rôle ordinaire. Dans les faits pour ne pas casser l'algorithme général,
		// on ajoute artificiellement un for principal avec le mode d'imposition source.
		final TripletIterator<ForFiscalPrincipal> iter = new TripletIterator<ForFiscalPrincipal>(forsPrincipaux.iterator());
		while (iter.hasNext()) {
			final Triplet<ForFiscalPrincipal> triplet = iter.next();
			if (triplet.current.getMotifOuverture() == MotifFor.PERMIS_C_SUISSE && (triplet.previous == null || !DateRangeHelper.isCollatable(triplet.previous, triplet.current))) {
				// On ajoute un for principal source compris entre le début de l'année et la date d'ouverture du for principal courant. Cette période est raccourcie si nécessaire.
				final RegDate debut;
				final RegDate fin = triplet.current.getDateDebut().getOneDayBefore();
				if (triplet.previous == null) {
					debut = RegDate.get(triplet.current.getDateDebut().year(), 1, 1);
				}
				else {
					debut = RegDateHelper.maximum(RegDate.get(triplet.current.getDateDebut().year(), 1, 1), triplet.previous.getDateFin().getOneDayAfter(), NullDateBehavior.EARLIEST);
				}
				final ForFiscalPrincipal forFictif = new ForFiscalPrincipal(debut, fin, triplet.current.getNumeroOfsAutoriteFiscale(),
						triplet.current.getTypeAutoriteFiscale(), triplet.current.getMotifRattachement(), ModeImposition.SOURCE);
				//noinspection deprecation
				forFictif.setMotifOuverture(MotifFor.INDETERMINE);
				forFictif.setMotifFermeture(MotifFor.PERMIS_C_SUISSE);
				if (forsFictifs == null) {
					forsFictifs = new ArrayList<ForFiscalPrincipal>();
				}
				forsFictifs.add(forFictif);
			}
		}

		if (forsFictifs != null) {
			forsPrincipaux.addAll(forsFictifs);
			Collections.sort(forsPrincipaux, new DateRangeComparator<ForFiscalPrincipal>());
		}
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, int annee) throws AssujettissementException {

		List<Assujettissement> list = determine(contribuable);
		if (list == null) {
			return null;
		}

		list = DateRangeHelper.extract(list, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), adapter);
		if (list.isEmpty()) {
			list = null;
		}

		return list;
	}

	@Override
	public List<Assujettissement> determine(Contribuable contribuable, @Nullable final DateRange range, boolean collate) throws AssujettissementException {

		List<Assujettissement> list = determine(contribuable);
		if (list == null) {
			return null;
		}

		if (!collate) {
			if (range == null) {
				throw new IllegalArgumentException("Le range doit être spécifié si collate=false");
			}
			list = split(list, range.getDateDebut().year(), range.getDateFin().year());
		}

		if (range != null) {
			// Limitation des assujettissements au range demandé
			list = DateRangeHelper.extract(list, range.getDateDebut(), range.getDateFin(), adapter);
		}

		if (list.isEmpty()) {
			list = null;
		}
		return list;
	}

	/**
	 * Asserte que les ranges ne se chevauchent pas.
	 *
	 * @param ranges les ranges à tester
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static void assertCoherenceRanges(List<? extends DateRange> ranges) throws AssujettissementException {
		DateRange previous = null;
		for (DateRange current : ranges) {
			if (previous != null) {
				if (DateRangeHelper.intersect(previous, current)) {
					throw new AssujettissementException("Le range [" + previous + "] entre en collision avec le suivant [" + current + ']');
				}
			}
			previous = current;
		}
	}

	/**
	 * Contient la date et le motif de fractionnement d'un range de dates.
	 */
	private static class Fraction {
		/**
		 * La date de fractionnement <b>au matin</b>. Au matin, signifie que le fractionnement doit être appliqué à 00:00 le matin du jour spécifié.
		 * <p/>
		 * <b>Exemple:</b> Fractionnement du range 2005.01.01-2005.12.31 à la date 2005.05.12 => deux ranges 2005.01.01-2005.05.11 et 2005.05.12-2005.12.31.
		 */
		public final RegDate date;
		/**
		 * Le motif (= la raison) du fractionnement. Ce motif peut être nul.
		 */
		public final MotifFor motif;

		private Fraction(@NotNull RegDate date, MotifFor motif) {
			this.date = date;
			this.motif = motif;
		}
	}

	/**
	 * Analyse les fors fiscaux principaux et retourne les dates (et motifs) de fractionnement.
	 *
	 * @param principaux une liste de fors fiscaux principaux
	 * @return une liste de fractionnements (non-modifiable). Cette liste peut-être vide
	 */
	@NotNull
	private static List<Fraction> determineFractionnements(List<ForFiscalPrincipal> principaux) {

		// Détermine les assujettissements pour le rattachement de type domicile
		final Map<RegDate, Fraction> map = new HashMap<RegDate, Fraction>();
		final TripletIterator<ForFiscalPrincipal> iter = new TripletIterator<ForFiscalPrincipal>(principaux.iterator());
		while (iter.hasNext()) {
			final Triplet<ForFiscalPrincipal> triplet = iter.next();

			// on détermine les fors principaux qui précèdent et suivent immédiatement
			final ForFiscalPrincipalContext forPrincipal = new ForFiscalPrincipalContext(triplet);

			// on détecte une éventuelle date de fractionnement à l'ouverture
			if (isFractionOuverture(forPrincipal)) {
				final RegDate fraction = forPrincipal.current.getDateDebut();
				MotifFor motifFraction = forPrincipal.current.getMotifOuverture();

				if (forPrincipal.next != null && isArriveeHCApresDepartHSMemeAnnee(forPrincipal.current) && !roleSourcierPur(forPrincipal.current)) {
					// dans ce cas précis, on veut utiliser le motif d'ouverture du for suivant comme motif de fractionnement
					motifFraction = forPrincipal.next.getMotifOuverture();
				}

				if (!map.containsKey(fraction)) {
					map.put(fraction, new Fraction(fraction, motifFraction));
				}
			}

			// on détecte une éventuelle date de fractionnement à la fermeture
			if (isFractionFermeture(forPrincipal)) {
				final RegDate fraction = forPrincipal.current.getDateFin().getOneDayAfter();
				final MotifFor motifFraction = forPrincipal.current.getMotifFermeture();

				if (!map.containsKey(fraction)) {
					map.put(fraction, new Fraction(fraction, motifFraction));
				}
			}
		}

		// On trie par ordre croissant
		final List<Fraction> fractionnements = new ArrayList<Fraction>(map.values());
		Collections.sort(fractionnements, new Comparator<Fraction>() {
			@Override
			public int compare(Fraction o1, Fraction o2) {
				return o1.date.compareTo(o2.date);
			}
		});

		return Collections.unmodifiableList(fractionnements);
	}

	private static class CasParticuliers {

		private boolean menageCommun;
		private Map<Integer, Mutation> mariagesDivorces = new HashMap<Integer, Mutation>();

		private CasParticuliers(boolean menageCommun) {
			this.menageCommun = menageCommun;
		}

		public boolean isMenageCommun() {
			return menageCommun;
		}

		public boolean hasMariage(int annee) {
			final Mutation mutation = mariagesDivorces.get(annee);
			return mutation != null && mutation.type == Mutation.Type.MARIAGE;
		}

		public Mutation getMariage(int annee) {
			final Mutation mutation = mariagesDivorces.get(annee);
			if (mutation == null || mutation.type != Mutation.Type.MARIAGE) {
				return null;
			}
			return mutation;
		}

		public boolean hasDivorce(int annee) {
			final Mutation mutation = mariagesDivorces.get(annee);
			return mutation != null && mutation.type == Mutation.Type.DIVORCE;
		}

		public Mutation getDivorce(int annee) {
			final Mutation mutation = mariagesDivorces.get(annee);
			if (mutation == null || mutation.type != Mutation.Type.DIVORCE) {
				return null;
			}
			return mutation;
		}

		private void add(RegDate date, Mutation.Type type) {
			final Mutation current = new Mutation(date, type);
			final Mutation previous = mariagesDivorces.put(date.year(), current);
			if (previous != null && previous.date.isAfter(date)) {
				throw new IllegalArgumentException("Cas particuliers reçus dans le désordre : ancien [" + previous + "], nouveau [" + current + ']');
			}
		}

		public void addMariage(RegDate date) {
			add(date, Mutation.Type.MARIAGE);
		}

		public void addDivorce(RegDate date) {
			final Mutation previous = mariagesDivorces.get(date.year());
			if (previous == null || previous.type != Mutation.Type.MARIAGE) { // on supporte de détecter deux divorces à la suite
				add(date, Mutation.Type.DIVORCE);
			}
			else {
				// un mariage suivi d'un divorce s'annulent
				mariagesDivorces.remove(date.year());
			}
		}

		public boolean isEmpty() {
			return mariagesDivorces.isEmpty();
		}
	}

	private static class Mutation {

		public enum Type {
			MARIAGE,
			DIVORCE
		}

		private RegDate date;
		private Type type;

		private Mutation(RegDate date, Type type) {
			this.date = date;
			this.type = type;
		}

		@Override
		public String toString() {
			return "Mutation{" +
					"date=" + date +
					", type=" + type +
					'}';
		}
	}

	/**
	 * Détermine les cas où des règles particulières s'appliquent (par exemple, avec les mariages où malgré la présence d'un for principal vaudois durant l'année, l'assujettissement se termine le 31
	 * décembre de l'année précédente).
	 *
	 * @param ctb        un contribuable
	 * @param principaux les fors fiscaux principaux du contribuable
	 * @return les cas particulières détectés.
	 */
	private static CasParticuliers determineCasParticuliers(Contribuable ctb, List<ForFiscalPrincipal> principaux) {
		final boolean menageCommun = ctb instanceof MenageCommun;
		CasParticuliers cas = new CasParticuliers(menageCommun);
		for (ForFiscalPrincipal f : principaux) {
			// les mariages et divorces n'ont d'influence que sur les assujettissements à raison de domicile, c'est-à-dire en présence d'un contribuable sur sol vaudois.
			if (f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				if (f.getMotifOuverture() == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
					cas.addMariage(f.getDateDebut());
				}
				else if (f.getMotifOuverture() == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT) {
					cas.addDivorce(f.getDateDebut());
				}
				if (f.getMotifFermeture() == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
					cas.addMariage(f.getDateFin());
				}
				else if (f.getMotifFermeture() == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT) {
					cas.addDivorce(f.getDateFin());
				}

				final RegDate prochain31Decembre = getProchain31Decembre(f.getDateDebut());
				if (menageCommun) {
					final Mutation divorce = cas.getDivorce(f.getDateDebut().year());
					if (divorce != null && divorce.date.isBefore(prochain31Decembre) && f.isValidAt(prochain31Decembre)) {
						// pour un ménage commun, s'il y a une séparation/divorce dans l'année et qu'il y a un for fiscal vaudois valide au 31 décembre,
						// c'est qu'il y a eu une réconciliation/mariage mais que les motifs étaient faux -> on compense
						cas.addMariage(f.getDateDebut());
					}
				}
				else {
					final Mutation mariage = cas.getMariage(f.getDateDebut().year());
					if (mariage != null && mariage.date.isBefore(prochain31Decembre) && f.isValidAt(prochain31Decembre)) {
						// pour une personne physique, s'il y a un mariage/réconciliation dans l'année et qu'il y a un for fiscal vaudois valide au 31 décembre,
						// c'est qu'il y a eu une divorce/séparation mais que les motifs étaient faux -> on compense
						cas.addDivorce(f.getDateDebut());
					}
				}
			}
		}
		return cas;
	}

	/**
	 * Détermine les données d'assujettissements brutes pour les rattachements de type domicile.
	 *
	 * @param principaux             les fors principaux d'un contribuable
	 * @param fractionnements        une liste vide qui contiendra les fractionnements calculés après l'exécution de la méthode
	 * @param casParticuliers        les cas particuliers identifiés dans l'historique des fors fiscaux
	 * @param noOfsCommunesVaudoises si renseigné, détermine le assujettissements du point de vue des communes spécifiées; si null, détermine les assujettissements du point de vue cantonal.
	 * @return la liste des assujettissements brutes calculés
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static DataList determineAssujettissementDomicile(List<ForFiscalPrincipal> principaux, List<Fraction> fractionnements, CasParticuliers casParticuliers,
	                                                          @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final DataList domicile = new DataList();

		// Détermine les assujettissements pour le rattachement de type domicile
		TripletIterator<ForFiscalPrincipal> iter = new TripletIterator<ForFiscalPrincipal>(principaux.iterator());
		while (iter.hasNext()) {
			final Triplet<ForFiscalPrincipal> triplet = iter.next();

			// on détermine les fors principaux qui précèdent et suivent immédiatement
			final ForFiscalPrincipalContext forPrincipal = new ForFiscalPrincipalContext(triplet);

			// on détermine l'assujettissement pour le for principal courant
			Data a = determine(forPrincipal, noOfsCommunesVaudoises);
			if (a == null) {
				continue;
			}

			// on fractionne l'assujettissement, si nécessaire
			a = fractionner(a, forPrincipal.current, fractionnements);
			if (a == null) {
				continue;
			}

			// on traite les cas particuliers, si nécessaire
			a = traiterCasParticuliers(a, fractionnements, casParticuliers);
			if (a == null) {
				continue;
			}

			domicile.add(a);
		}

		return domicile;
	}

	/**
	 * Applique les règles des cas particuliers sur l'assujettissement courant.
	 *
	 * @param data            un assujettissement
	 * @param fractions       la liste des fractionnements
	 * @param casParticuliers les cas particuliers à traiter
	 * @return un assujettissement, éventuellement modifié; ou <b>null</b> si l'assujettissement a disparu suite au traitement.
	 */
	private static Data traiterCasParticuliers(Data data, List<Fraction> fractions, CasParticuliers casParticuliers) {

		if (casParticuliers.isEmpty()) {
			return data;
		}

		RegDate debut = data.debut;
		RegDate fin = data.fin;

		if (debut != null) {
			final Limites limites = Limites.determine(debut, debut, fractions);
			final RegDate dernierFractionnement = limites == null ? null : limites.getLeft() == null ? null : limites.getLeft().date;

			if (casParticuliers.isMenageCommun() && casParticuliers.hasMariage(debut.year())) {
				// en cas de mariage, le ménage commun est assumé assujetti depuis le début de l'année (ou depuis le dernier fractionnement)
				debut = RegDateHelper.maximum(getDernier1Janvier(debut), dernierFractionnement, NullDateBehavior.EARLIEST);
			}

			if (!casParticuliers.isMenageCommun() && casParticuliers.hasDivorce(debut.year())) {
				// en cas de divorce, la personne physique est assumée assujettie depuis le début de l'année (ou depuis le dernier fractionnement)
				debut = RegDateHelper.maximum(getDernier1Janvier(debut), dernierFractionnement, NullDateBehavior.EARLIEST);
			}
		}
		
		if (fin != null) {
			final Limites limites = Limites.determine(fin, fin, fractions);
			final RegDate dernierFractionnement = limites == null ? null : limites.getLeft() == null ? null : limites.getLeft().date;
			final RegDate prochainFractionnement = limites == null ? null : limites.getRight() == null ? null : limites.getRight().date;

			final boolean finDejaFractionnee = (prochainFractionnement != null && fin == prochainFractionnement.getOneDayBefore());
			if (!finDejaFractionnee) {
				if (casParticuliers.isMenageCommun()) {
					final Mutation divorce = casParticuliers.getDivorce(fin.year());
					if (divorce != null && !is31Decembre(divorce.date)) {
						// en cas de divorce, le ménage commun n'est plus assujetti à partir du 1er janvier de l'année (ou depuis le dernier fractionnement)
						fin = RegDateHelper.maximum(RegDate.get(fin.year() - 1, 12, 31), dernierFractionnement, NullDateBehavior.EARLIEST);
					}
				}

				if (!casParticuliers.isMenageCommun()) {
					final Mutation mariage = casParticuliers.getMariage(fin.year());
					if (mariage != null && !is31Decembre(mariage.date)) {
						// en cas de mariage, la personne physique n'est plus assujettie à partir du 1er janvier de l'année (ou depuis le dernier fractionnement)
						fin = RegDateHelper.maximum(RegDate.get(fin.year() - 1, 12, 31), dernierFractionnement, NullDateBehavior.EARLIEST);
					}
				}
			}
		}

		if (debut != null && fin != null && fin.isBefore(debut)) {
			// plus d'assujettissement
			return null;
		}

		data.debut = debut;
		data.fin = fin;

		return data;
	}

	/**
	 * Limites de fractionnement à gauche et à droite d'une range déterminé.
	 */
	private static class Limites {

		private Fraction left;
		private Fraction right;

		private Limites(Fraction left, Fraction right) {
			this.left = left;
			this.right = right;
		}

		public Fraction getLeft() {
			return left;
		}

		public Fraction getRight() {
			return right;
		}

		/**
		 * Détermine les fractionnements immédiatement à gauche (borne inclue) et droite (borne exclue) du range de dates spécifié. Par principe, les fractions à l'intérieur du range sont ignorées.
		 *
		 * @param range     un range de dates
		 * @param fractions une liste de fractions
		 * @return les fractions gauche et droite déterminées; ou <b>null</b> si aucune limite n'a été trouvée.
		 */
		public static Limites determine(DateRange range, List<Fraction> fractions) {
			return determine(range.getDateDebut(), range.getDateFin(), fractions);
		}

		/**
		 * Détermine les fractionnements immédiatement à gauche (borne inclue) et droite (borne exclue) du range de dates spécifié. Par principe, les fractions à l'intérieur du range sont ignorées.
		 *
		 * @param dateDebut la date de début du range
		 * @param dateFin   la date de fin du range
		 * @param fractions une liste de fractions
		 * @return les fractions gauche et droite déterminées; ou <b>null</b> si aucune limite n'a été trouvée.
		 */
		public static Limites determine(RegDate dateDebut, RegDate dateFin, List<Fraction> fractions) {

			if (fractions.isEmpty()) {
				return null;
			}

			Fraction left = null;
			Fraction right = null;
			for (Fraction f : fractions) {
				if (dateDebut != null && f.date.isBeforeOrEqual(dateDebut)) {
					if (left == null || left.date.isBefore(f.date)) {
						left = f;
					}
				}
				if (dateFin != null && f.date.isAfter(dateFin)) {
					if (right == null || right.date.isAfter(f.date)) {
						right = f;
					}
				}
			}

			if (left == null && right == null) {
				return null;
			}

			return new Limites(left, right);
		}
	}

	/**
	 * Applique les régles de fractionnement sur l'assujettissement spécifié.
	 *
	 * @param a         un assujettissement
	 * @param forSource le for principal à la source de l'assujettissement spécifié.
	 * @param fractions la liste des fractions
	 * @return un assujettissement, fractionné si nécessaire.
	 */
	private static Data fractionner(Data a, ForFiscalPrincipal forSource, List<Fraction> fractions) {

		if (fractions.isEmpty()) {
			return a;
		}

		// on détermine les fractionnements immédiatement à gauche et droite du for principal à la source 
		// de l'assujettissement (logiquement, il n'est pas possible d'avoir un fractionnement à l'intérieur du for)
		final Limites limites = Limites.determine(forSource, fractions);
		final Fraction left = (limites == null ? null : limites.getLeft());
		final Fraction right = (limites == null ? null : limites.getRight());

		// on réduit l'assujettissement en conséquence
		if (left != null && left.date.isAfter(a.debut)) {
			a.debut = left.date;

			if (a.motifDebut == MotifFor.ARRIVEE_HC && left.motif == MotifFor.DEPART_HS) {
				// dans le cas d'un départ HS et d'une arrivée HC, on ne veut pas collater les deux assujettissements,
				// il faut donc garder le motif de début sans changement (voir isCollatable())
			}
			else {
				a.motifDebut = left.motif;
			}
		}


		if (right != null && right.date.isBeforeOrEqual(a.fin)) {
			a.fin = right.date.getOneDayBefore();
			a.motifFin = right.motif;
		}

		return a;
	}

	/**
	 * Détermine les données d'assujettissements brutes pour les rattachements de type économique.
	 *
	 * @param secondaires            les fors secondaires d'un contribuable
	 * @param fractionnements        la liste des fractionnements d'assujettissement calculés lors de l'analyse des fors principaux
	 * @param noOfsCommunesVaudoises si renseigné, détermine le assujettissements du point de vue des communes spécifiées; si null, détermine les assujettissements du point de vue cantonal.
	 * @return la liste des assujettissements brutes calculés
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static List<Data> determineAssujettissementEconomique(List<ForFiscalSecondaire> secondaires, List<Fraction> fractionnements, @Nullable Set<Integer> noOfsCommunesVaudoises) throws
			AssujettissementException {
		List<Data> economique = new ArrayList<Data>();
		// Détermine les assujettissements pour le rattachement de type économique
		for (ForFiscalSecondaire f : secondaires) {
			final Data a = determine(f, fractionnements, noOfsCommunesVaudoises);
			if (a != null) {
				economique.add(a);
			}
		}
		return economique;
	}

	/**
	 * Découpe les assujettissements spécifié année par année pour les années spécifiées
	 *
	 * @param list      les assujettissements à découper
	 * @param startYear l'année de départ (comprise)
	 * @param endYear   l'année de fin (comprise
	 * @return une liste d'assujettissements découpés année par année
	 */
	private static List<Assujettissement> split(List<Assujettissement> list, int startYear, int endYear) {

		List<Assujettissement> splitted = new ArrayList<Assujettissement>();

		// split des assujettissement par années
		for (int year = startYear; year <= endYear; ++year) {
			List<Assujettissement> extracted = DateRangeHelper.extract(list, RegDate.get(year, 1, 1), RegDate.get(year, 12, 31), adapter);
			splitted.addAll(extracted);
		}

		return splitted;
	}

	/**
	 * Code de retour des méthodes d'adaption, utilisé à la place de booléens par mesure de clarté.
	 */
	private enum AdaptionResult {
		LISTE_NON_MODIFIEE,
		LISTE_MODIFIEE
	}

	/**
	 * Cette méthode adapte les dates de début et de fin de validité des assujettissements pour qu'elles tombent exactement sur des débuts et fins de mois; et ceci dans certains cas précis.
	 * <p/>
	 * Les dates de début/fin d'un assujettissement sont adaptées si les conditions suivantes sont respectées: <ul> <li>l'assujettissement est de type source pure</li> <li>l'assujettissement
	 * précédent/suivant est de type ordinaire non-mixte (ordinaire, dépense, ... mais pas sourcier mixte)</li> </ul>
	 * <p/>
	 * En gros, à chaque passage source pure à ordinaire et vice-versa les dates de début et fin sont adaptées.
	 *
	 * @param assujettissements une liste des assujettissement
	 */
	private static void adapteDatesDebutEtFin(List<Assujettissement> assujettissements) {
		final int size = assujettissements.size();
		if (size == 0) {
			return;
		}

		// Note: algo un peu spécial parce que le liste sur lequel est basé l'itérateur peut se trouver modifiée. Dans ce cas, on reprend le
		// travail au début (jusqu'à ce que toutes les modifications nécessaires aient été apportées).
		AdaptionResult res;
		do {
			res = AdaptionResult.LISTE_NON_MODIFIEE;
			final TripletIterator<Assujettissement> iter = new TripletIterator<Assujettissement>(assujettissements.iterator());

			while (res == AdaptionResult.LISTE_NON_MODIFIEE && iter.hasNext()) {
				final Triplet<Assujettissement> triplet = iter.next();
				res = adapteTriplet(assujettissements, triplet);
			}
		} while (res == AdaptionResult.LISTE_MODIFIEE);
	}

	/**
	 * Adapte les dates de fin et de début pour le triplet d'assujettissement courant.
	 *
	 * @param assujettissements la liste complète des assujettissements
	 * @param triplet           le triple courant
	 * @return {@link AdaptionResult#LISTE_NON_MODIFIEE} si les assujettissements précédent et suivant ont pu être adaptés sans problème; ou {@link AdaptionResult#LISTE_MODIFIEE} si l'un d'entre eux a
	 *         été supprimé de la liste et que cette dernière a donc été modifiée.
	 */
	private static AdaptionResult adapteTriplet(List<Assujettissement> assujettissements, final Triplet<Assujettissement> triplet) {

		AdaptionResult res = AdaptionResult.LISTE_NON_MODIFIEE;

		final Assujettissement courant = triplet.current;

		if (courant instanceof SourcierPur) {
			final RegDate debut = courant.getDateDebut();
			final RegDate fin = courant.getDateFin();
			final MotifFor motifDebut = courant.getMotifFractDebut();
			final MotifFor motifFin = courant.getMotifFractFin();

			// faut-il adapter la date de début ?
			final Assujettissement precedent = triplet.previous;
			if ((precedent == null || !(precedent instanceof Sourcier)) && !isDepartOuArriveeHorsSuisse(motifDebut)) { // [UNIREG-2155]
				// on doit arrondir au début du mois
				final RegDate newDebut = RegDate.get(debut.year(), debut.month(), 1);
				if (newDebut != debut) {
					if (precedent != null && precedent.getDateFin().getOneDayAfter() == debut) {
						final AdaptionResult r = adapteDateFin(precedent, newDebut.getOneDayBefore(), assujettissements);
						if (r == AdaptionResult.LISTE_MODIFIEE) {
							res = AdaptionResult.LISTE_MODIFIEE;
						}
					}
					courant.setDateDebut(newDebut);
				}
			}

			// faut-il adapter la date de fin ?
			if (fin != null && !isDepartOuArriveeHorsSuisse(motifFin)) { // [UNIREG-2155]
				final Assujettissement suivant = triplet.next;
				if (suivant == null || !(suivant instanceof Sourcier)) {
					// on doit arrondir à la fin du mois
					final RegDate newFin = RegDate.get(fin.year(), fin.month(), 1).addMonths(1).getOneDayBefore();
					if (newFin != fin) {
						if (suivant != null && suivant.getDateDebut().getOneDayBefore() == fin) {
							final AdaptionResult r = adapteDateDebut(suivant, newFin.getOneDayAfter(), assujettissements);
							if (r == AdaptionResult.LISTE_MODIFIEE) {
								res = AdaptionResult.LISTE_MODIFIEE;
							}
						}
						courant.setDateFin(newFin);
					}
				}
			}
		}

		return res;
	}

	private static boolean isDepartOuArriveeHorsSuisse(MotifFor motif) {
		return motif == MotifFor.DEPART_HS || motif == MotifFor.ARRIVEE_HS;
	}

	/**
	 * Adapte la date de fin de l'assujettissement précédent à la nouvelle valeur spécifiée.
	 * <p/>
	 * Cette méthode applique intelligemment la nouvelle date, c'est-à-dire qu'elle s'assure que l'assujettissement précédent reste valide avec la nouvelle date. Si ce n'est pas le cas,
	 * l'assujettissement précédent est supprimé de la liste des assujettissements.
	 *
	 * @param precedent         l'assujettissement précédent dont la date de fin doit être adaptée.
	 * @param newDateFin        la nouvelle date de fin à appliquer.
	 * @param assujettissements la liste complète des assujettissements (qui doit contenir l'assujettissement précédent).
	 * @return {@link AdaptionResult#LISTE_NON_MODIFIEE} si l'assujettissement précédent à pu être adapté sans problème; ou {@link AdaptionResult#LISTE_MODIFIEE} si l'assujettissement a été supprimé de
	 *         la liste et que cette dernière a donc été modifiée.
	 */
	private static AdaptionResult adapteDateFin(Assujettissement precedent, RegDate newDateFin, List<Assujettissement> assujettissements) {
		if (precedent.getDateDebut().isBeforeOrEqual(newDateFin)) {
			precedent.setDateFin(newDateFin);
			return AdaptionResult.LISTE_NON_MODIFIEE;
		}
		else {
			// la nouvelle date de fin est *avant* la date de début de l'assujettissement précédent -> on l'annule
			final int index = assujettissements.indexOf(precedent);
			assujettissements.remove(index);

			// et il faut remonter dans la liste d'assujettissement pour adapter le précédent du précédent
			if (index > 0) {
				Assujettissement precedentprecedent = assujettissements.get(index - 1);
				adapteDateFin(precedentprecedent, newDateFin, assujettissements);
			}

			return AdaptionResult.LISTE_MODIFIEE;
		}
	}

	/**
	 * Adapte la date de début de l'assujettissement suivant à la nouvelle valeur spécifiée.
	 * <p/>
	 * Cette méthode applique intelligemment la nouvelle date, c'est-à-dire qu'elle s'assure que l'assujettissement suivant reste valide avec la nouvelle date. Si ce n'est pas le cas, l'assujettissement
	 * suivant est supprimé de la liste des assujettissements.
	 *
	 * @param suivant           l'assujettissement suivant dont la date de début doit être adaptée.
	 * @param newDateDebut      la nouvelle date de début à appliquer.
	 * @param assujettissements la liste complète des assujettissements (qui doit contenir l'assujettissement suivant).
	 * @return {@link AdaptionResult#LISTE_NON_MODIFIEE} si l'assujettissement suivant à pu être adapté sans problème; ou {@link AdaptionResult#LISTE_MODIFIEE} si l'assujettissement a été supprimé de la
	 *         liste et que cette dernière a donc été modifiée.
	 */
	private static AdaptionResult adapteDateDebut(Assujettissement suivant, RegDate newDateDebut, List<Assujettissement> assujettissements) {
		if (suivant.getDateFin() == null || newDateDebut.isBeforeOrEqual(suivant.getDateFin())) {
			suivant.setDateDebut(newDateDebut);
			return AdaptionResult.LISTE_NON_MODIFIEE;
		}
		else {
			// la nouvelle date de début est *après* la date de fin de l'assujettissement suivant -> on l'annule
			final int index = assujettissements.indexOf(suivant);
			assujettissements.remove(index);

			// et il faut descendre dans la liste d'assujettissement pour adapter le suivant du suivant
			if (index < assujettissements.size()) {
				Assujettissement suivantsuivant = assujettissements.get(index);
				adapteDateDebut(suivantsuivant, newDateDebut, assujettissements);
			}

			return AdaptionResult.LISTE_MODIFIEE;
		}
	}

	private static boolean is31Decembre(RegDate date) {
		return date.month() == 12 && date.day() == 31;
	}

	private static boolean roleSourcierPur(ForFiscalPrincipal forPrecedent) {
		return forPrecedent.getModeImposition() == ModeImposition.SOURCE;
	}

	private static boolean roleSourcierMixte(ForFiscalPrincipal forPrecedent) {
		return forPrecedent.getModeImposition() == ModeImposition.MIXTE_137_1 || forPrecedent.getModeImposition() == ModeImposition.MIXTE_137_2;
	}

	private static boolean roleOrdinaireNonMixte(ForFiscalPrincipal forCourant) {
		final ModeImposition mode = forCourant.getModeImposition();
		return ModeImposition.SOURCE != mode && ModeImposition.MIXTE_137_1 != mode && ModeImposition.MIXTE_137_2 != mode;
	}

	/**
	 * Détermine s'il y a un départ ou une arrivée hors-Suisse entre le deux fors fiscaux spécifiés. Cette méthode s'assure que les types d'autorité fiscales sont cohérentes de manière à détecter les
	 * faux départs/arrivées hors-Suisse dûs à des motifs d'ouverture/fermetures incohérents.
	 * <p/>
	 * <b>Note:</b> si les deux fors sont renseignés, ils doivent se toucher.
	 *
	 * @param left  le for fiscal de gauche (peut être nul)
	 * @param right le for fiscal de droite (peut être nul)
	 * @return <b>true</b> si un départ ou une arrivée hors-Suisse est détecté.
	 */
	private static boolean isDepartOuArriveeHorsSuisse(ForFiscalPrincipal left, ForFiscalPrincipal right) {
		if (left == null && right == null) {
			throw new IllegalArgumentException();
		}

		final boolean fraction;

		if (left != null && right != null && left.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE && right.getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE) {
			if (left.getDateFin().getOneDayAfter() != right.getDateDebut()) {
				throw new IllegalArgumentException();
			}

			//noinspection SimplifiableIfStatement
			if (isArriveeHCApresDepartHSMemeAnnee(left) && !roleSourcierPur(left)) {
				// dans le cas d'un départ HS et d'arrivée HC dans le même année (donc avec un seul for fiscal HS avec
				// ces deux motifs), il ne faut pas que l'arrivée HC fractionne l'assujettissement.
				// [UNIREG-3261] sauf si le for courant possède un mode d'imposition source, dans ce cas, la date de fin de l'assujettissement est quand même fractionné
				fraction = false;
			}
			else {
				// dans tous les autres cas, on ignore les motifs d'ouverture/fermeture (qui sont souvent faux) et
				// on se base uniquement sur les autorités fiscales (qui sont des valeurs sûres)
				fraction = (left.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS || right.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) &&
						(left.getTypeAutoriteFiscale() != right.getTypeAutoriteFiscale());
			}
		}
		else {
			// Autrement, on se base sur les motifs d'ouverture et de fermeture
			boolean motifDetecte = (left != null && (left.getMotifFermeture() == MotifFor.ARRIVEE_HS || left.getMotifFermeture() == MotifFor.DEPART_HS));
			motifDetecte = motifDetecte || (right != null && (right.getMotifOuverture() == MotifFor.ARRIVEE_HS || right.getMotifOuverture() == MotifFor.DEPART_HS));

			fraction = motifDetecte;
		}

		return fraction;
	}

	/**
	 * [UNIREG-3261] Détermine si le for fiscal se ferme avec un motif arrivée hors-canton la même année qu'il s'est ouvert avec un départ hors-Suisse.
	 *
	 * @param current le for fiscal principal à tester
	 * @return <b>vrai</b> si le for fiscal se ferme avec un motif arrivée hors-canton la même année qu'il s'est ouvert avec un départ hors-Suisse; <b>faux</b> autrement.
	 */
	private static boolean isArriveeHCApresDepartHSMemeAnnee(ForFiscalPrincipal current) {
		return current.getDateDebut().year() == current.getDateFin().year() && current.getMotifOuverture() == MotifFor.DEPART_HS && current.getMotifFermeture() == MotifFor.ARRIVEE_HC;
	}

//	/**
//	 * [UNIREG-3261] Détermine si le for fiscal se ferme avec un motif arrivée hors-canton la même année qu'il s'est ouvert avec un départ hors-Suisse.
//	 *
//	 * @param current le for fiscal principal à tester
//	 * @param next    le for fiscal principal qui suit immédiatement; ou <b>null</b> s'il n'y en a pas
//	 * @return <b>vrai</b> si le for fiscal se ferme avec un motif arrivée hors-canton la même année qu'il s'est ouvert avec un départ hors-Suisse; <b>faux</b> autrement.
//	 */
//	private static boolean isArriveeHCApresDepartHSMemeAnnee(ForFiscalPrincipal current, ForFiscalPrincipal next) {
//		if (current.getDateDebut().year() != current.getDateFin().year()) {
//			return false;
//		}
//		if (next == null) {
//			// pas de for fiscal immédiatement suivant, on se rabat sur le motif de fermeture
//			return current.getMotifOuverture() == MotifFor.DEPART_HS && current.getMotifFermeture() == MotifFor.ARRIVEE_HC;
//		}
//		else {
//			// autant que possible, on se base sur les types d'autorités fiscales pour déterminer l'arrivée de HC plutôt que les motifs (qui sont souvent faux)
//			return current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
//		}
//	}

	/**
	 * [UNIREG-2759] Détermine si le for fiscal se ferme avec un départ hors-canton la même année qu'il s'est ouvert avec une arrivée de hors-Suisse
	 *
	 * @param current le for fiscal principal à tester
	 * @param next    le for fiscal principal qui suit immédiatement; ou <b>null</b> s'il n'y en a pas
	 * @return <b>vrai</b> si le for fiscal se ferme avec un départ hors-canton la même année qu'une arrivée de hors-Suisse; <b>faux</b> autrement.
	 */
	private static boolean isDepartHCApresArriveHSMemeAnnee(ForFiscalPrincipal current, ForFiscalPrincipal next) {

		if (current == null) {
			return false;
		}

		final RegDate fin = current.getDateFin();
		if (fin == null) {
			return false;
		}

		if (fin.year() != current.getDateDebut().year()) {
			return false;
		}

		if (next != null) {
			// autant que possible, on se base sur les types d'autorités fiscales pour déterminer le départ HC plutôt que les motifs (qui sont souvent faux)
			return current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
		}
		else {
			// pas de for fiscal immédiatement suivant, on se rabat sur le motif de fermeture
			return current.getMotifFermeture() == MotifFor.DEPART_HC;
		}
	}

	/**
	 * @param left  le for fiscal de gauche (peut être nul)
	 * @param right le for fiscal de droite (peut être null)
	 * @return <b>true</b> si un départ ou une arrivée hors-canton est détectée entre les forts fiscaux spécifiés. Cette méthode s'assure que les types d'autorité fiscales sont cohérentes de manière à
	 *         détecter les faux départs/arrivées hors-canton.
	 */
	private static boolean isDepartOuArriveeHorsCanton(ForFiscalPrincipal left, ForFiscalPrincipal right) {
		if (left == null && right == null) {
			throw new IllegalArgumentException();
		}

		boolean motifDetecte = (left != null && (left.getMotifFermeture() == MotifFor.ARRIVEE_HC || left.getMotifFermeture() == MotifFor.DEPART_HC));
		motifDetecte = motifDetecte || (right != null && (right.getMotifOuverture() == MotifFor.ARRIVEE_HC || right.getMotifOuverture() == MotifFor.DEPART_HC));

		boolean typeAutoriteOk = (left == null || left.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC);
		typeAutoriteOk = typeAutoriteOk || (right == null || right.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC);

		return motifDetecte && typeAutoriteOk;
	}

	private static boolean isFractionOuverture(ForFiscalPrincipalContext forPrincipal) {

		final ForFiscalPrincipal previous = forPrincipal.previous;
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		final MotifFor motifOuverture = current.getMotifOuverture();
		final ModeImposition modeImposition = current.getModeImposition();

		final ModeImposition previousModeImposition = (previous == null ? null : previous.getModeImposition());

		boolean fraction = false;

		if (motifOuverture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date d'ouverture pour ce motif
			fraction = true;
		}
		else if (isDepartOuArriveeHorsSuisse(previous, current) &&
				(modeImposition == ModeImposition.SOURCE || (isDepartDepuisOuArriveeVersVaud(current, previous) && !isDepartHCApresArriveHSMemeAnnee(current, next)))) {
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			// [UNIREG-3261] l'arrivée de hors-Suisse doit quand même fractionner si le contribuable est sourcier pur
			fraction = true;
		}
		else if (isDepartOuArriveeHorsCanton(previous, current) &&
				(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 ||
						previousModeImposition == ModeImposition.SOURCE || previousModeImposition == ModeImposition.MIXTE_137_2)) {
			// [UNIREG-1742] Le départ ou l'arrivée hors-Canton d'un sourcier pur ou mixte 137 al. 2 (donc sans for secondaire) doit provoquer un fractionnement
			fraction = true;
		}
		else if (previous != null && ((roleSourcierPur(previous) && roleOrdinaireNonMixte(current)) || (roleOrdinaireNonMixte(previous) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle ordinaire non-mixte (et vice versa) doit provoquer un fractionnement.
			fraction = true;
		}
		else if (previous != null && current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS &&
				((roleSourcierPur(previous) && roleSourcierMixte(current)) || (roleSourcierMixte(previous) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle source-mixte (et vice versa) doit provoquer un fractionnement (hors-Suisse uniquement).
			fraction = true;
		}

		return fraction;
	}

	private static boolean isFractionFermeture(ForFiscalPrincipalContext forPrincipal) {

		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		if (current.getDateFin() == null) {
			return false;
		}

		final MotifFor motifFermeture = current.getMotifFermeture();
		final ModeImposition modeImposition = current.getModeImposition();

		final ModeImposition nextModeImposition = (next == null ? null : next.getModeImposition());

		boolean fraction = false;

		if (motifFermeture == MotifFor.VEUVAGE_DECES) {
			// fractionnement systématique à la date de fermeture pour ce motif
			fraction = true;
		}
		else if (isDepartOuArriveeHorsSuisse(current, next) &&
				(nextModeImposition == ModeImposition.SOURCE || (isDepartDepuisOuArriveeVersVaud(current, next) &&
				!isDepartHCApresArriveHSMemeAnnee(next, forPrincipal.nextnext)))) {
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			// [UNIREG-3261] l'arrivée de hors-Suisse doit quand même fractionner si le contribuable est sourcier pur
			fraction = true;
		}
		else if (isDepartOuArriveeHorsCanton(current, next) &&
				(modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_2 ||
						nextModeImposition == ModeImposition.SOURCE || nextModeImposition == ModeImposition.MIXTE_137_2)) {
			// [UNIREG-1742] Le départ ou l'arrivée hors-Canton d'un sourcier pur ou mixte 137 al. 2 (donc sans for secondaire) doit provoquer un fractionnement
			fraction = true;
		}
		else if (next != null && ((roleSourcierPur(next) && roleOrdinaireNonMixte(current)) || (roleOrdinaireNonMixte(next) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle ordinaire non-mixte (et vice versa) doit provoquer un fractionnement.
			fraction = true;
		}
		else if (next != null && current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS &&
				((roleSourcierPur(next) && roleSourcierMixte(current)) || (roleSourcierMixte(next) && roleSourcierPur(current)))) {
			// le passage du rôle source pur au rôle source-mixte (et vice versa) doit provoquer un fractionnement (hors-Suisse uniquement).
			fraction = true;
		}

		return fraction;
	}

	/**
	 * Détermine la date de début d'un assujettissement induit par un for fiscal principal.
	 *
	 * @param forPrincipal le for fiscal principal dont on veut déterminer la date de début d'assujettissement
	 * @return la date de début de l'assujettissement
	 */
	private static RegDate determineDateDebutAssujettissement(ForFiscalPrincipalContext forPrincipal) {

		final RegDate debut;
		final ForFiscalPrincipal current = forPrincipal.current;

		if (isFractionOuverture(forPrincipal) || current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			debut = current.getDateDebut();
		}
		else {
			// dans tous les autres cas, l'assujettissement débute au 1er janvier de l'année courante
			debut = getDernier1Janvier(current.getDateDebut());
		}

		return debut;
	}

	/**
	 * Détermine la date de fin d'un assujettissement induit par un for fiscal principal.
	 *
	 * @param forPrincipal le for fiscal principal dont on veut déterminer la date de fin d'assujettissement
	 * @return la date de fin de l'assujettissement
	 */
	private static RegDate determineDateFinAssujettissement(ForFiscalPrincipalContext forPrincipal) {

		final ForFiscalPrincipal current = forPrincipal.current;
		final RegDate fin = current.getDateFin();

		final RegDate afin;
		if (fin == null || isFractionFermeture(forPrincipal) || current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			afin = fin;
		}
		else {
			// dans tous les autres cas, l'assujettissement finit à la fin de l'année précédente
			afin = getDernier31Decembre(fin);
		}

		return afin;
	}

	/**
	 * Détermine la date de début de la période de non-assujettissement correspondant à un for fiscal principal (période durant laquelle un for secondaire pourrait provoquer un assujettissement).
	 *
	 * @param forPrincipal le for fiscal principal dont on veut déterminer la date de début de non-assujettissement
	 * @return la date de fin de la période de non-assujettissement
	 */
	private static RegDate determineDateDebutNonAssujettissement(ForFiscalPrincipalContext forPrincipal) {

		final ForFiscalPrincipal previous = forPrincipal.previous;
		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		final RegDate debut = current.getDateDebut();

		final RegDate adebut;
		if (isDepartOuArriveeHorsSuisse(previous, current) && (!isDepartDepuisOuArriveeVersVaud(current, previous) || isDepartHCApresArriveHSMemeAnnee(current, next))) {
			// cas du départ/arrivée HS depuis hors-canton : on ignore le fractionnement est on applique l'assujettissement depuis le début de l'année
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			adebut = getDernier1Janvier(debut);
		}
		else if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && current.getMotifOuverture() == MotifFor.DEPART_HC) {
			// cas limite du ctb qui part HC et arrive de HS dans la même année -> la durée précise de la période hors-Suisse n'est pas connue et on prend la solution
			// la plus avantageuse pour l'ACI : arrivée de HS au 1er janvier de l'année suivante
			adebut = getProchain1Janvier(debut);
		}
		else if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && isMariageOuDivorce(current.getMotifOuverture())) {
			// [UNIREG-2432] Exception : si le motif d'ouverture est MARIAGE ou SEPARATION, la date de début est ramenée au 1 janvier de l'année courante.
			// L'idée est que dans ces cas-là, le rattachement est transféré de la PP vers le ménage (ou inversément) sur l'entier de la période.
			adebut = getDernier1Janvier(debut);
		}
		else {
			// Cas général
			if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
				adebut = getDernier1Janvier(debut);
			}
			else { // for hors-Suisse
				adebut = debut; // le rattachement économmique est limité à la période de validité du for pour les HS
			}
		}

		return adebut;
	}

	/**
	 * Détermine la date de fin de la période de non-assujettissement correspondant à un for fiscal principal (période durant laquelle un for secondaire pourrait provoquer un assujettissement).
	 *
	 * @param forPrincipal le for fiscal principal dont on veut déterminer la date de fin de non-assujettissement
	 * @return la date de fin de la période de non-assujettissement
	 */
	private static RegDate determineDateFinNonAssujettissement(ForFiscalPrincipalContext forPrincipal) {

		final ForFiscalPrincipal current = forPrincipal.current;
		final ForFiscalPrincipal next = forPrincipal.next;

		final RegDate fin = current.getDateFin();

		final RegDate afin;
		if (fin == null) {
			afin = null;
		}
		else if (isDepartOuArriveeHorsSuisse(current, next) && (!isDepartDepuisOuArriveeVersVaud(current, next) || isDepartHCApresArriveHSMemeAnnee(next, forPrincipal.nextnext))) {
			// cas du départ/arrivée HS depuis hors-canton : on ignore le fractionnement est on applique l'assujettissement jusqu'au 31 décembre précédant
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			afin = getDernier31Decembre(fin);
		}
		else if (isArriveeHCApresDepartHSMemeAnnee(current) && !roleSourcierPur(current)) {
			// cas limite du ctb qui part HS et arrive de HC dans la même année -> la durée précise de la période hors-Suisse n'est pas connue et on prend la solution
			// la plus avantageuse pour l'ACI : arrivée de HS au 31 décembre de l'année précédente.
			// [UNIREG-3261] sauf si le for courant possède un mode d'imposition source, dans ce cas l'assujettissement est fractionné à la date d'arrivée
			afin = getDernier31Decembre(fin);
		}
		else if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC && isDernierForPrincipalDansAnnee(forPrincipal)) {
			// si le for principal se ferme mais qu'il n'y a pas de for immédiatement suivant (par exemple: cas du contribuable hors-canton avec immeuble, qui vend son immeuble et
			// dont le for principal hors-canton est fermé à la date de vente), alors il s'agit d'une "fausse" fermeture du for et on le considère valide jusqu'à la fin de l'année.
			afin = getProchain31Decembre(fin); // le rattachement économique s'étend à toute l'année pour le HC
		}
		else {
			// Cas général
			if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
				afin = getProchain31Decembre(fin);
			}
			else {
				afin = fin; // le rattachement économmique est limité à la période de validité du for pour les HS
			}
		}

		return afin;
	}

	/**
	 * Détermine si le for fiscal principal spécifié est le dernier dans l'année courante (= qu'il existe pas d'autre for fiscal principal valide entre la date de fin du for fiscal et le 31 décembre de
	 * la même année)
	 *
	 * @param forPrincipal un for fiscal principal
	 * @return <b>vrai</b> si le for spécifiée est le dernier dans l'année; <b>faux</b> autement.
	 */
	private static boolean isDernierForPrincipalDansAnnee(ForFiscalPrincipalContext forPrincipal) {

		final RegDate dateFin = forPrincipal.current.getDateFin();
		if (forPrincipal.next != null || dateFin == null) {
			return false;
		}

		// (msi 7.11.2011), c'est pas génial de remonter sur le tiers pour récupérer tous les fors fiscaux, c'est clair. Mais d'un
		// autre côté, on le fait tellement peu souvent dans le cas réel que ça n'a pas d'impact négatif sur les performances.
		final List<ForFiscalPrincipal> all = forPrincipal.current.getTiers().getForsFiscauxPrincipauxActifsSorted();

		// si on ne trouve aucun for principal entre la date de fin du for spécifié et la fin de l'année, alors c'est que le for spécifié est le dernier dans l'année
		final RegDate finAnnee = getProchain31Decembre(dateFin);
		for (ForFiscalPrincipal f : all) {
			if (RegDateHelper.isBetween(f.getDateDebut(), dateFin.getOneDayAfter(), finAnnee, NullDateBehavior.EARLIEST)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param date une date
	 * @return le 31 décembre le plus proche de la date spécifiée et qui ne soit pas dans le futur.
	 */
	private static RegDate getDernier31Decembre(RegDate date) {
		final RegDate afin;
		if (is31Decembre(date)) {
			afin = date;
		}
		else {
			afin = RegDate.get(date.year() - 1, 12, 31);
		}
		return afin;
	}

	/**
	 * @param date une date
	 * @return le 31 décembre le plus proche de la date spécifiée et qui ne soit pas dans le passé.
	 */
	private static RegDate getProchain31Decembre(RegDate date) {
		return RegDate.get(date.year(), 12, 31);
	}

	private static RegDate getDernier1Janvier(RegDate date) {
		return RegDate.get(date.year(), 1, 1);
	}

	private static RegDate getProchain1Janvier(RegDate debut) {
		return RegDate.get(debut.year() + 1, 1, 1);
	}

	private enum Type {
		VaudoisOrdinaire,
		VaudoisDepense,
		SourcierMixte1,
		SourcierMixte2,
		SourcierPur,
		Indigent,
		HorsSuisse,
		HorsCanton,
		DiplomateSuisse,
		NonAssujetti
	}

	/**
	 * Représente les données brutes d'une période d'assujettissement
	 */
	private static class Data implements DateRange {

		RegDate debut;
		RegDate fin;
		MotifFor motifDebut;
		MotifFor motifFin;
		Type type;
		final TypeAutoriteFiscale typeAut;

		/**
		 * Collections des motifs d'ouverture de for qui ne donnent normalement pas lieu à un début d'assujettissement
		 * ou qui doivent, le cas échéant, laisser la priorité au motif d'ouverture du for "économique" - si existant à la même date - dans la méthode
		 * {@link #merge(ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, java.util.Set) merge}
		 */
		@SuppressWarnings({"deprecation"})
		private static final Set<MotifFor> DEBUT_ASSUJETTISSEMENT = new HashSet<MotifFor>(Arrays.asList(MotifFor.INDETERMINE,
		                                                                                                MotifFor.VENTE_IMMOBILIER,
		                                                                                                MotifFor.ANNULATION,
		                                                                                                MotifFor.FIN_ACTIVITE_DIPLOMATIQUE,
		                                                                                                MotifFor.FIN_EXPLOITATION));

		/**
		 * Collections des motifs de fermeture de for qui ne donnent normalement pas lieu à une fin d'assujettissement
		 * ou qui doivent, le cas échéant, laisser la priorité au motif de fermeture du for "économique" - si existant à la même date - dans la méthode
		 * {@link #merge(ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.MotifFor, java.util.Set) merge}
		 */
		@SuppressWarnings({"deprecation"})
		private static final Set<MotifFor> FIN_ASSUJETTISSEMENT = new HashSet<MotifFor>(Arrays.asList(MotifFor.INDETERMINE,
		                                                                                              MotifFor.ACHAT_IMMOBILIER,
		                                                                                              MotifFor.REACTIVATION,
		                                                                                              MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE,
		                                                                                              MotifFor.DEBUT_EXPLOITATION));

		private Data(RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin, Type type, TypeAutoriteFiscale typeAut) {
			this.debut = debut;
			this.fin = fin;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
			this.type = type;
			this.typeAut = typeAut;
		}

		private Data(Data right) {
			this.debut = right.debut;
			this.fin = right.fin;
			this.motifDebut = right.motifDebut;
			this.motifFin = right.motifFin;
			this.type = right.type;
			this.typeAut = right.typeAut;
		}

		@Override
		public boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, debut, fin, NullDateBehavior.LATEST);
		}

		@Override
		public RegDate getDateDebut() {
			return debut;
		}

		@Override
		public RegDate getDateFin() {
			return fin;
		}

		/**
		 * Cette méthode fusionne une donnée d'assujettissement <i>domicile</i> avec une donnée d'assujettissement <i>économique</i>.
		 *
		 * @param eco une donnée d'assujettissement économique
		 * @return une liste de données d'assujettissement résultante qui doivent remplacer l'assujettissement courant; ou <b>null</b> s'il n'y a rien à faire.
		 */
		public List<Data> merge(Data eco) {

			if (type != Type.NonAssujetti) {
				// l'assujettissement 'this' (qui est forcéement de type domicile) est différent de non-assujetti : quelque soit la valeur de 'eco', il prime sur ce dernier et il n'y a rien à faire.
				return null;
			}

			if (!DateRangeHelper.intersect(this, eco)) {
				// pas d'intersection -> rien à faire
				return null;
			}

			final List<Data> list;
			if (eco.debut.isBeforeOrEqual(this.debut) && RegDateHelper.isAfterOrEqual(eco.fin, this.fin, NullDateBehavior.LATEST)) {
				// eco dépasse des deux côtés de this -> pas besoin de découper quoique ce soit
				list = null;
			}
			else {
				list = new ArrayList<Data>(3);
				list.add(this);
				if (eco.debut.isAfter(this.debut)) {
					// on découpe à gauche
					Data left = new Data(this);
					left.fin = eco.debut.getOneDayBefore();
					left.motifFin = eco.motifDebut;
					list.add(0, left);

					// on décale la date de début
					this.debut = eco.debut;
					this.motifDebut = eco.motifDebut;
				}

				if (!RegDateHelper.isAfterOrEqual(eco.fin, this.fin, NullDateBehavior.LATEST)) {
					// on découpe à droite
					Data right = new Data(this);
					right.debut = eco.fin.getOneDayAfter();
					right.motifDebut = eco.motifFin;
					list.add(right);

					// on décale la date de fin
					this.fin = eco.fin;
					this.motifFin = eco.motifFin;
				}

			}

			// si les motifs de début/fin manquent, on profite de ceux du for économique pour les renseigner
			this.motifDebut = merge(this.debut, this.motifDebut, eco.debut, eco.motifDebut, DEBUT_ASSUJETTISSEMENT);
			this.motifFin = merge(this.fin, this.motifFin, eco.fin, eco.motifFin, FIN_ASSUJETTISSEMENT);

			// pas assujetti + immeuble/activité indépendante = hors-canton ou hors-Suisse
			this.type = getAType(this.typeAut);

			return list;
		}

		/**
		 * Fusionne le motif de début/fin d'assujettissement pour raison de domicile avec le motif de début/fin d'assujettissement pour raison économique.
		 *
		 * @param dateDomicile  la date de début/fin de l'assujettissement pour raison de domicile
		 * @param motifDomicile le motif de début/fin de l'assujettissement pour raison de domicile
		 * @param dateEco       la date de début/fin de l'assujettissement pour raison économique
		 * @param motifEco      le motif de début/fin de l'assujettissement pour raison économique
		 * @param motifsDomicileNonPrioritaires la liste des motifs de fors "Domicile" pour lesquels le motif "Econonique" prend la priorité
		 * @return le motid de début/fin d'assujettissement résultant.
		 */
		private static MotifFor merge(RegDate dateDomicile, MotifFor motifDomicile, RegDate dateEco, MotifFor motifEco, Set<MotifFor> motifsDomicileNonPrioritaires) {
			if (dateDomicile == dateEco && motifEco != null && (motifDomicile == null || motifsDomicileNonPrioritaires.contains(motifDomicile))) {
				return motifEco;
			}
			else {
				return motifDomicile;
			}
		}

		@Override
		public String toString() {
			return "Data{" +
					"debut=" + debut +
					", fin=" + fin +
					", type=" + type +
					", typeAut=" + typeAut +
					'}';
		}
	}

	/**
	 * Détermine les données d'assujettissement pour un for fiscal principal.
	 *
	 * @param forPrincipal           le for fiscal dont on veut calculer l'assujettissement (plus ceux qui précèdent et suivent immédiatement)
	 * @param noOfsCommunesVaudoises si renseigné, détermine le assujettissements du point de vue des communes spécifiées; si null, détermine les assujettissements du point de vue cantonal.
	 * @return les données d'assujettissement, ou <b>null</b> si le for principal n'induit aucun assujettissement
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static Data determine(ForFiscalPrincipalContext forPrincipal, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final Data data;
		final ForFiscalPrincipal current = forPrincipal.current;

		switch (current.getTypeAutoriteFiscale()) {
		case COMMUNE_OU_FRACTION_VD: {

			final RegDate adebut = determineDateDebutAssujettissement(forPrincipal);
			final RegDate afin = determineDateFinAssujettissement(forPrincipal);

			if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
				final MotifRattachement motifRattachement = current.getMotifRattachement();

				if (noOfsCommunesVaudoises != null && !noOfsCommunesVaudoises.contains(forPrincipal.current.getNumeroOfsAutoriteFiscale())) {
					// [SIFISC-1769] le for principal est sur une autre commune : non-assujetti du point de vue des communes vaudoises spécifiées.
					data = new Data(adebut, afin, null, null, Type.NonAssujetti, current.getTypeAutoriteFiscale());
				}
				else if (motifRattachement == MotifRattachement.DIPLOMATE_SUISSE) {
					// cas particulier du diplomate suisse basé à l'étranger
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), Type.DiplomateSuisse, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				}
				else { // cas général
					if (motifRattachement != MotifRattachement.DOMICILE) {
						throw new AssujettissementException("Le contribuable n°" + current.getTiers().getNumero() + " possède un for fiscal principal avec un motif de rattachement [" +
								motifRattachement + "] ce qui est interdit.");
					}
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), getAType(current.getModeImposition()), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				}
			}
			else {
				// pas d'assujettissement
				data = null;
			}

			break;
		}
		case COMMUNE_HC:
		case PAYS_HS: {

			// [SIFISC-1769] l'assujettissement source hors-canton/hors-Suisse est seulement pris en compte d'un point de vue cantonal (= pas en cas de point de vue communes vaudoises)
			if (isSource(current.getModeImposition()) && noOfsCommunesVaudoises == null) {

				final Type type = getAType(current.getModeImposition());
				final RegDate adebut = determineDateDebutAssujettissement(forPrincipal);
				final RegDate afin = determineDateFinAssujettissement(forPrincipal);

				if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), type, current.getTypeAutoriteFiscale());
				}
				else {
					// pas d'assujettissement
					data = null;
				}
			}
			else {
				final RegDate adebut = determineDateDebutNonAssujettissement(forPrincipal);
				final RegDate afin = determineDateFinNonAssujettissement(forPrincipal);

				if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
					data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), Type.NonAssujetti, current.getTypeAutoriteFiscale());
				}
				else {
					// pas d'assujettissement
					data = null;
				}
			}
			break;
		}

		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + current.getTypeAutoriteFiscale() + ']');
		}

		return data;
	}

	private static boolean isSource(ModeImposition modeImposition) {
		return modeImposition == ModeImposition.SOURCE || modeImposition == ModeImposition.MIXTE_137_1 || modeImposition == ModeImposition.MIXTE_137_2;
	}

	private static Data determine(ForFiscalSecondaire ffs, List<Fraction> fractionnements, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final RegDate debut = ffs.getDateDebut();
		final RegDate fin = ffs.getDateFin();

		if (noOfsCommunesVaudoises != null && !noOfsCommunesVaudoises.contains(ffs.getNumeroOfsAutoriteFiscale())) {
			// [SIFISC-1769] le for secondaire n'est pas sur une des communes vaudoises spécifiées : pas d'assujettissement.
			return null;
		}

		// La période d'assujettissement en raison d'un rattachement économique s'étend à toute l'année pour tous les types de
		// rattachement (art. 8 al. 6 LI). Sauf en cas de décès, veuvage, départ ou arrivée hors-Suisse, où la durée
		// d'assujettissement est réduite en conséquence.
		// [UNIREG-1360] Ce point a été confirmé par Thierry Declercq le 31 août 2009.

		RegDate adebut;
		if (isMariageOuDivorce(ffs.getMotifOuverture())) {
			// [UNIREG-2432] Exception : si le motif d'ouverture est MARIAGE ou SEPARATION, la date de début est ramenée au 1 janvier de l'année courante.
			// L'idée est que dans ces cas-là, le rattachement est transféré de la PP vers le ménage (ou inversément) sur l'entier de la période.
			adebut = getDernier1Janvier(debut);
		}
		else if (isHorsSuisse(ffs.getTiers(), debut)) {
			adebut = debut;
		}
		else {
			adebut = getDernier1Janvier(debut);
		}

		RegDate afin;
		if (fin == null) {
			afin = fin;
		}
		else if (isMariageOuDivorce(ffs.getMotifFermeture())) {
			// [UNIREG-2432] Exception : si le motif de fermeture est MARIAGE ou SEPARATION, la date de fin est ramenée au 31 décembre de l'année précédente.
			// L'idée est que dans ces cas-là, le rattachement est transféré de la PP vers le ménage (ou inversément) sur l'entier de la période.
			afin = getDernier31Decembre(fin);
		}
		else if (isHorsSuisse(ffs.getTiers(), fin)) {
			afin = fin;
		}
		else {
			afin = getProchain31Decembre(fin);
		}

		// Dans tous les cas, si on trouve une date de fractionnement entre la date réelle du début du for et la date de début de l'assujettissement,
		// on adapte cette dernière en conséquence. Même chose pour les dates de fin d'assujettissement.
		for (Fraction f : fractionnements) {
			if (RegDateHelper.isBetween(f.date, adebut, debut, NullDateBehavior.LATEST)) {
				adebut = f.date;
			}

			if (fin != null && RegDateHelper.isBetween(f.date.getOneDayBefore(), fin, afin, NullDateBehavior.LATEST)) {
				afin = f.date.getOneDayBefore();
			}
		}

		if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) { // [UNIREG-2559]
			return new Data(adebut, afin, ffs.getMotifOuverture(), ffs.getMotifFermeture(), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		}
		else {
			// pas d'assujettissement
			return null;
		}
	}

	private static boolean isMariageOuDivorce(MotifFor motif) {
		return motif == MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT || motif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
	}

	private static boolean isHorsSuisse(Tiers tiers, RegDate date) {
		Set<ForFiscal> fors = tiers.getForsFiscaux();
		for (ForFiscal f : fors) {
			if (f.isPrincipal() && f.isValidAt(date)) {
				return f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS;
			}
		}
		return false;
	}

	/**
	 * Fusionne les assujettissements économiques aux assujettissement domicile en appliquant les régles métier. Le résultat est stocké dans la liste des assujettissements domicile.
	 *
	 * @param domicile   les assujettissements pour motif de rattachement domicile
	 * @param economique les assujettissements pour motif de rattachement économique
	 */
	private static void fusionne(List<Data> domicile, List<Data> economique) {

		for (int i = 0; i < domicile.size(); i++) {
			for (Data e : economique) {
				final Data d = domicile.get(i);
				final List<Data> sub = d.merge(e);
				if (sub != null) {
					domicile.set(i, sub.get(0));
					for (int j = 1; j < sub.size(); ++j) {
						domicile.add(i + j, sub.get(j));
					}
				}
			}
		}

		Collections.sort(domicile, new DateRangeComparator<DateRange>());
	}

	private static List<Assujettissement> instanciate(Contribuable ctb, List<Data> all) {
		final List<Assujettissement> assujettissements = new ArrayList<Assujettissement>(all.size());
		for (Data a : all) {
			final Assujettissement assujettissement;
			switch (a.type) {
			case VaudoisOrdinaire:
				assujettissement = new VaudoisOrdinaire(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case VaudoisDepense:
				assujettissement = new VaudoisDepense(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case Indigent:
				assujettissement = new Indigent(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case HorsCanton:
				assujettissement = new HorsCanton(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case HorsSuisse:
				assujettissement = new HorsSuisse(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case SourcierPur:
				assujettissement = new SourcierPur(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut);
				break;
			case SourcierMixte1:
				assujettissement = new SourcierMixteArt137Al1(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut);
				break;
			case SourcierMixte2:
				assujettissement = new SourcierMixteArt137Al2(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut);
				break;
			case DiplomateSuisse:
				assujettissement = new DiplomateSuisse(ctb, a.debut, a.fin, a.motifDebut, a.motifFin);
				break;
			case NonAssujetti:
				assujettissement = null;
				break;
			default:
				throw new IllegalArgumentException("Type inconnu = [" + a.type + ']');
			}
			if (assujettissement != null) {
				assujettissements.add(assujettissement);
			}
		}
		return assujettissements;
	}

	private static Type getAType(TypeAutoriteFiscale typeAutoriteFiscale) {
		return (typeAutoriteFiscale == TypeAutoriteFiscale.PAYS_HS ? Type.HorsSuisse : Type.HorsCanton);
	}

	private static boolean isDepartDepuisOuArriveeVersVaud(ForFiscalPrincipal left, ForFiscalPrincipal right) {
		// un des deux fors fiscaux doit être dans le canton de Vaud
		return (left != null && left.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) ||
				(right != null && right.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
	}

	private static Type getAType(ModeImposition modeImposition) throws AssujettissementException {
		final Type type;
		switch (modeImposition) {
		case ORDINAIRE:
			type = Type.VaudoisOrdinaire;
			break;
		case MIXTE_137_1:
			type = Type.SourcierMixte1;
			break;
		case MIXTE_137_2:
			type = Type.SourcierMixte2;
			break;
		case INDIGENT:
			type = Type.Indigent;
			break;
		case DEPENSE:
			type = Type.VaudoisDepense;
			break;
		case SOURCE:
			type = Type.SourcierPur;
			break;
		default:
			throw new AssujettissementException("Mode d'imposition inconnu : " + modeImposition);
		}
		return type;
	}

	private static class Adapter implements DateRangeHelper.AdapterCallback<Assujettissement> {
		@Override
		public Assujettissement adapt(Assujettissement assujettissement, RegDate debut, RegDate fin) {
			if (debut != null && assujettissement.getDateDebut() != debut) {
				assujettissement.setDateDebut(debut);
				assujettissement.setMotifDebut(null);
			}
			if (fin != null && assujettissement.getDateFin() != fin) {
				assujettissement.setDateFin(fin);
				assujettissement.setMotifFin(null);
			}
			return assujettissement;
		}
	}

	/**
	 * Un for fiscal principal et son context, c'est-à-dire les fors fiscaux principaux qui précèdent et qui suivent immédiatement.
	 */
	private static class ForFiscalPrincipalContext {

		public final ForFiscalPrincipal previousprevious;
		public final ForFiscalPrincipal previous;
		public final ForFiscalPrincipal current;
		public final ForFiscalPrincipal next;
		public final ForFiscalPrincipal nextnext;

		/**
		 * Construit le contact d'un for fiscal principal à partir du triplet de fors fiscaux (qui ne se touchent pas forcément).
		 *
		 * @param triplet le triplet de fors fiscaux initials
		 */
		public ForFiscalPrincipalContext(Triplet<ForFiscalPrincipal> triplet) {
			current = triplet.current;
			previous = (triplet.previous != null && DateRangeHelper.isCollatable(triplet.previous, current) ? triplet.previous : null);
			previousprevious = (previous != null && triplet.previousprevious != null && DateRangeHelper.isCollatable(triplet.previousprevious, previous) ? triplet.previousprevious : null);
			next = (triplet.next != null && DateRangeHelper.isCollatable(current, triplet.next) ? triplet.next : null);
			nextnext = (next != null && triplet.nextnext != null && DateRangeHelper.isCollatable(next, triplet.nextnext) ? triplet.nextnext : null);
		}
	}

	/**
	 * Liste spécialisée pour contenir des données brutes d'assujettissements.
	 */
	private static class DataList extends ArrayList<Data> {

		private int nonAssujettissementCount;

		@SuppressWarnings({"UnusedDeclaration"})
		private DataList(int initialCapacity) {
			super(initialCapacity);
			this.nonAssujettissementCount = 0;
		}

		private DataList() {
			this.nonAssujettissementCount = 0;
		}

		@SuppressWarnings({"UnusedDeclaration"})
		private DataList(Collection<? extends Data> c) {
			super(c);
			this.nonAssujettissementCount = countNonAssujettissements(c);
		}

		private static int countNonAssujettissements(Collection<? extends Data> c) {
			if (c instanceof DataList) {
				return ((DataList) c).nonAssujettissementCount;
			}
			else {
				int count = 0;
				for (Data data : c) {
					if (data.type == Type.NonAssujetti) {
						++count;
					}
				}
				return count;
			}
		}

		@Override
		public Data set(int index, Data element) {
			Data previous = super.set(index, element);
			if (previous != null && previous.type == Type.NonAssujetti && (element == null || element.type != Type.NonAssujetti)) {
				--nonAssujettissementCount;
			}
			else if ((previous == null || previous.type!= Type.NonAssujetti) && element != null && element.type == Type.NonAssujetti) {
				++nonAssujettissementCount;
			}
			return previous;
		}

		@Override
		public boolean add(Data data) {
			if (data != null && data.type == Type.NonAssujetti) {
				++nonAssujettissementCount;
			}
			return super.add(data);
		}

		@Override
		public void add(int index, Data element) {
			if (element != null && element.type == Type.NonAssujetti) {
				++nonAssujettissementCount;
			}
			super.add(index, element);
		}

		@Override
		public Data remove(int index) {
			Data removed = super.remove(index);
			if (removed != null && removed.type == Type.NonAssujetti) {
				--nonAssujettissementCount;
			}
			return removed;
		}

		@Override
		public boolean remove(Object o) {
			boolean removed = super.remove(o);
			if (removed && ((Data)o).type == Type.NonAssujetti) {
				--nonAssujettissementCount;
			}
			return removed;
		}

		@Override
		public void clear() {
			nonAssujettissementCount = 0;
			super.clear();
		}

		@Override
		public boolean addAll(Collection<? extends Data> c) {
			nonAssujettissementCount += countNonAssujettissements(c);
			return super.addAll(c);
		}

		@Override
		public boolean addAll(int index, Collection<? extends Data> c) {
			nonAssujettissementCount += countNonAssujettissements(c);
			return super.addAll(index, c);
		}

		@Override
		protected void removeRange(int fromIndex, int toIndex) {
			throw new NotImplementedException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new NotImplementedException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new NotImplementedException();
		}

		/**
		 * [SIFISC-2939] Compacte la liste en réduisant les non-assujettissements qui chevauchent des assujettissements.
		 * <p/>
		 * Dans certains cas particuliers (mais légaux), des non-assujettissements qui couvrent toute une année (du 1er janvier au 31 décembre) coexistent avec des assujettissements normaux (par exemple,
		 * dans le cas d'un contribuable hors-canton qui possède temporairement un for secondaire dans le canton, attend quelques semaines, puis vient s'installer dans le canton la même année). Dans ces
		 * cas, cette méthode s'assure que les non-assujettissements laissent gracieusement leurs places aux assujettissements.
		 *
		 * @param forRolesCommunes <b>vrai</b> si cette méthode est appelée dans le contexte du rôle des communes; <b>faux</b> autrement.
		 */
		public void compacterNonAssujettissements(boolean forRolesCommunes) {
			if (nonAssujettissementCount > 0) {

				// on sépare le bon grain de l'ivraie
				List<Data> nonA = new ArrayList<Data>(nonAssujettissementCount);
				final List<Data> vraiA = new ArrayList<Data>(size());
				for (Data data : this) {
					if (data.type == Type.NonAssujetti) {
						nonA.add(data);
					}
					else {
						vraiA.add(data);
					}
				}

				// on fusionne les non-assujettissements qui peuvent l'être
				nonA = fusionnerNonAssujettissements(nonA, forRolesCommunes);

				// on réduit la durée des non-assujettissement si nécessaire
				final List<Data> list = DateRangeHelper.override(nonA, vraiA, new DateRangeHelper.AdapterCallbackExtended<Data>() {
					@Override
					public Data adapt(Data range, RegDate debut, RegDate fin) {
						throw new IllegalArgumentException("ne devrait pas être appelé");
					}

					@Override
					public Data adapt(Data range, RegDate debut, Data surchargeDebut, RegDate fin, Data surchargeFin) {
						final Data a = new Data(range);
						if (debut != null) {
							a.debut = debut;
							a.motifDebut = surchargeDebut.motifFin;
						}
						if (fin != null) {
							a.fin = fin;
							a.motifFin = surchargeFin.motifDebut;
						}
						return a;
					}

					@Override
					public Data duplicate(Data range) {
						return new Data(range);
					}
				});

				// on met-à-jour la liste elle-même
				clear();
				addAll(list);
			}
		}

		/**
		 * Fusionne les non-assujettissement qui s'intersectent. Des non-assujettissements peuvent s'intersecter lorsque - par exemple - un contribuable possède plusieurs fors fiscaux principaux hors-canton
		 * dans une même année.
		 *
		 * @param list             une liste de données qui doivent être des non-assujettisssments
		 * @param forRolesCommunes <b>vrai</b> si cette méthode est appelée dans le contexte du rôle des communes; <b>faux</b> autrement.
		 * @return la liste fusionnée des non-assujettissments
		 */
		private List<Data> fusionnerNonAssujettissements(List<Data> list, final boolean forRolesCommunes) {
			return DateRangeHelper.merge(list, DateRangeHelper.MergeMode.INTERSECTING, new DateRangeHelper.MergeCallback<Data>() {
				@Override
				public Data merge(Data left, Data right) {
					final RegDate debut = RegDateHelper.minimum(left.getDateDebut(), right.getDateDebut(), NullDateBehavior.EARLIEST);
					final RegDate fin = RegDateHelper.maximum(left.getDateFin(), right.getDateFin(), NullDateBehavior.LATEST);
					final TypeAutoriteFiscale typeAut = fusionnerTypeAutPourNonAssujettissements(left, right, forRolesCommunes);
					return new Data(debut, fin, left.motifDebut, right.motifFin, Type.NonAssujetti, typeAut);
				}

				@Override
				public Data duplicate(Data range) {
					return new Data(range);
				}
			});
		}

		private static TypeAutoriteFiscale fusionnerTypeAutPourNonAssujettissements(Data left, Data right, boolean forRolesCommunes) {
			if (forRolesCommunes) {
				// [SIFISC-4682] Dans le cas du calcul de l'assujettissement du point de vue d'une commune vaudoise, il peut y avoir
				// un for fiscal principal hors-canton associé à un for secondaire dans le canton qui provoquent chacun un non-assujettissement
				// de type différent (hors-canton et commune_vd) et qui se chevauchent.
				// Il s'agit d'une situation est correcte dans ce cas-là, et le type d'autorité fiscale qui nous intéresse est celle qui n'est PAS vaudoise
				// (puisque si un for fiscal vaudois a généré un non-assujettissement, c'est justement parce qu'on ne veut pas en tenir compte).
				return left.typeAut != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD ? left.typeAut : right.typeAut;
			}
			else {
				// Dans le cas du calcul de l'assujettissement normal, seuls les fors fiscaux principaux hors-canton et hors-Suisse peuvent
				// générer des non-assujettissement. Comme il y a forcément un fractionnement entre un for hors-Suisse et un for d'un autre type,
				// on ne devrait jamais avoir des fors avec des types différents qui se chevauchent, sauf en cas d'erreur dans l'algorithme.
				if (left.typeAut != right.typeAut) {
					throw new IllegalArgumentException("Détecté deux non-assujettissements de type différents qui s'intersectent [" + left + "] et [" + right + "] (erreur dans l'algorithme ?)");
				}
				return left.typeAut;
			}
		}
	}
}
