package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.metier.common.DecalageDateHelper;
import ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.Fractionnements;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Le calculateur d'assujettissement selon le régime des personnes physiques
 */
public class AssujettissementPersonnesPhysiquesCalculator implements AssujettissementCalculator<ContribuableImpositionPersonnesPhysiques> {

	public static final AssujettissementSurCommuneAnalyzer COMMUNE_ANALYZER = new AssujettissementSurCommuneAnalyzer() {
		@Override
		public List<ForFiscalRevenuFortune> getForsVaudoisDeterminantsPourCommunes(Assujettissement assujettissement) {
			// pour l'assujettissement PP, les fors déterminants sont :
			// - tous les fors secondaires de la période
			// - le dernier for principal suisse de la période , s'il est vaudois

			final DecompositionFors fors = assujettissement.getFors();
			final List<ForFiscalRevenuFortune> determinants = new ArrayList<>(1 + fors.secondairesDansLaPeriode.size());
			final ForFiscalPrincipal ffp = fors.principal != null ? fors.principal : fors.principauxDansLaPeriode.last();
			if (ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				determinants.add(ffp);
			}
			for (ForFiscalSecondaire ffs : fors.secondairesDansLaPeriode) {
				if (ffs != null && ffs.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
					determinants.add(ffs);
				}
			}
			return determinants;
		}
	};

	/**
	 * Méthode principale pour la détermination des assujettissements d'un contribuable (PP).
	 * <p/>
	 * <p/>
	 * <b>Introduction.</b> Cette méthode prend en entrée les fors fiscaux d'un contribuable, et en déduit les assujettissements. Un <i>for fiscal</i> représente le rattachement concret d'un contribuable
	 * à une commune. Un <i>assujettissement</i> représente la raison pour laquelle un contribuable est assujetti à l'impôt. Dans Unireg, les assujettissements sont déduits dynamiquement des fors fiscaux
	 * (= la fonction de cette méthode).
	 * <p/>
	 * <p/>
	 * <b>Spécifications.</b> L'algorithme de cette méthode découle des régles des spécifications <i>SCU-DeterminerDI-PPAEmettre.doc</i> et <i>SCU-EnvoyerAutomatiquementDI-PP.doc</i>; ainsi que de toutes
	 * les règles supplémentaires édictées dans les cas JIRAs qui apparaissent dans les commentaires de ce fichier. Il n'y a pas de spécification sur l'assujettissement à proprement parler.
	 * <p/>
	 * <p/>
	 * <b>Principe de base.</b> L'algorithme de cette méthode est basé sur deux principes simples : <ol> <li>chaque for fiscal génère un assujettissement;</li> <li>les assujettissements sont fusionnés
	 * pour établir la vue globale.</li> </ol> Ainsi, pour un contribuable qui possède les fors fiscaux suivants (arrivée HC le 1er janvier 2000 + achat d'un immeuble le 1er juillet 2003) :
	 * <pre>
	 *                               +---------------------------------
	 *     For fiscal principal :    | 2000.01.01   Lausanne
	 *                               +---------------------------------
	 *
	 *                                        +------------------------
	 *     For fiscal secondaire :            | 2003.07.01   Morges
	 *                                        +------------------------
	 * </pre>
	 * L'algorithme va générer les assujettissements suivants (de type {@link Data}) :
	 * <pre>
	 *                               +---------------------------------
	 *     Vaudois ordinaire :       | 2000.01.01
	 *                               +---------------------------------
	 *
	 *                                    +----------------------------
	 *     Economique :                   | 2003.01.01
	 *                                    +----------------------------
	 * </pre>
	 * .., qui seront finalement fusionnés pour donner le résultat suivant (de type {@link Assujettissement})  :
	 * <pre>
	 *                               +---------------------------------
	 *     Vaudois ordinaire :       | 2000.01.01
	 *                               +---------------------------------
	 * </pre>
	 * <p/>
	 * <p/>
	 * <b>Domiciles et économiques.</b> L'exemple ci-dessus permet immédiatement de distinguer deux catégories d'assujettissements : <ul> <li>L'assujettissement pour raison de domicile.</li>
	 * <li>L'assujettissement pour raison économique.</li> </ul> Les assujettissements pour raison de domicile sont les plus importants et priment sur les assujettissements pour raison économique dans
	 * tous les cas. La méthode {@link #fusionne(java.util.List, java.util.List)} s'occupe de cette fusion.
	 * <p/>
	 * <p/>
	 * <b>Durées des assujettissement.</b> La durée de l'assujettissement généré par un for fiscal varie en fonction de son type et de son rattachement. De manière générale, on a : <ul> <li><i>for fiscal
	 * principal vaudois ou hors-canton</i> : valable du 1er janvier de l'année d'arrivée jusqu'au 31 décembre de l'année précédant le départ.</li> <li><i>for fiscal principal hors-Suisse</i> : valable
	 * du jour d'arrivée jusqu'au jour de départ, précisemment.</li> <li><i>for fiscal secondaire</i> : valable du 1er janvier de l'année d'ouverture jusqu'au 31 décembre de l'année de fermeture.</li>
	 * </ul> Il s'agit donc de règles générales, qui ne tiennent pas compte des fractionnements et des cas particuliers (voir ci-dessous).
	 * <p/>
	 * Les méthodes {@link #determineDateDebutAssujettissement(ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext, ch.vd.uniregctb.metier.common.Fractionnements)}
	 * et {@link #determineDateFinAssujettissement(ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext, ch.vd.uniregctb.metier.common.Fractionnements)} déterminent
	 * les durées des assujettissements pour raison de domicile sur sol vaudois. Les méthodes {@link #determineDateDebutNonAssujettissement(ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext)}
	 * et {@link #determineDateFinNonAssujettissement(ch.vd.uniregctb.metier.common.ForFiscalPrincipalContext)} déterminent les durées des assujettissements pour
	 * raison de domicile hors-canton ou hors-Suisse (qui ne correspondent pas à des assujettissements vaudois et sont donc appelés des "non-assujettissements". Ces "non-assujettissements" sont
	 * nécessaires plus tard pour fusionner les assujettissements économiques).
	 * <p/>
	 * <p/>
	 * <b>Fractionnement de l'assujettissement.</b> De manière générale, un assujettissement s'étend sur une année fiscale complète. Cependant, dans certains cas, l'assujettissement est fractionné : il
	 * commence ou s'arrête en milieu d'année. L'exemple typique est le cas du départ hors-Suisse d'un contribuable vaudois :
	 * <pre>
	 *                               +---------------------------------------------+------------------------
	 *     For fiscal principal :    | 2000.01.01       Lausanne        2008.05.15 | 2008.05.16   Paris
	 *                               +---------------------------------------------+------------------------
	 *
	 *                               +---------------------------------------------+
	 *     Assujettissement :        | 2000.01.01   Vaudois ordinaire   2008.05.15 |
	 *                               +---------------------------------------------+
	 * </pre>
	 * Lors d'un départ (ou d'une arrivée) hors-Suisse, l'assujettissement est fractionné à la date de départ (ou d'arrivée). Dans le cas ci-dessus, comme le contribuable ne possède plus de for fiscal
	 * sur le canton de Vaud, il n'est plus assujetti à partir de son départ.
	 * <p/>
	 * Le constructeur de la classe {@link FractionnementsRole} s'occupe de déterminer les dates de fractionnement.
	 * <p/>
	 * <p/>
	 * <b>Cas particuliers.</b> La difficulté de l'algorithme de l'assujettissement est le nombre de cas particuliers dont il faut tenir compte. Il n'est pas possible d'en dresser une liste exhaustive,
	 * car il se combinent souvents entre eux pour donner de nouveaux cas particuliers. Parmis les plus évidents, citons : <ul> <li><i>les départs ou arrivées de hors-Suisse</i> : fractionnement de
	 * l'assujettissement,</li> <li><i>les passages source pur à ordinaire</i> : fractionnement de l'assujettissement + arrondi des dates au mois près,</li> <li><i>les décès et veuvage</i> :
	 * fractionnement de l'assujettissement,</li> <li><i>les mariages et divorces</i> : l'assujettissement est reporté sur le ménage en cas de mariage, ou sur les divorcés en cas de divorce.</li> </ul>
	 *
	 * @param ctb                    le contribuable dont les assujettissements doivent être déterminés
	 * @param fors                   les fors fiscaux triés par types du contribuable
	 * @param noOfsCommunesVaudoises (optionnel) filtre d'inclusion des numéros Ofs des communes qui doivent être considérées comme vaudoises (seulement pour le rôle des communes, null dans tous les
	 *                               autres cas)
	 * @return la liste des assujettissements; ou <b>null</b> si le contribuable n'est pas assujetti.
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement du contribuable.
	 */
	@Override
	public List<Assujettissement> determine(ContribuableImpositionPersonnesPhysiques ctb, ForsParType fors, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		ajouteForsPrincipauxFictifs(fors.principauxPP);
		final List<Assujettissement> role = determineRole(ctb, fors, noOfsCommunesVaudoises);
		final List<SourcierPur> source = determineSource(ctb, fors, noOfsCommunesVaudoises);

		final List<Assujettissement> assujettissements = fusionneAssujettissements(role, source);
		AssujettissementHelper.assertCoherenceRanges(assujettissements);

		return assujettissements.isEmpty() ? null : assujettissements;
	}

	/**
	 * Fusionne les assujettissements <i>source</i> et <i>rôle</i> spécifié. Dans le cas où ces assujettissements se chevauchent, les assujettissements <i>rôle</i> sont prioritaires.
	 *
	 * @param role   les assujettissements <i>rôle</i>
	 * @param source les assujettissements <i>source</i>
	 * @return les assujettissements <i>rôle</i> et <i>source</i> fusionnés
	 */
	private static List<Assujettissement> fusionneAssujettissements(List<Assujettissement> role, List<SourcierPur> source) {
		return DateRangeHelper.override(new ArrayList<>(source), role, new OverrideAssujettissementCallback<>());
	}

	@NotNull
	public static List<SourcierPur> determineSource(ContribuableImpositionPersonnesPhysiques ctb, ForsParType fors, Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final Fractionnements<ForFiscalPrincipalPP> fractionnements = new FractionnementsSource(fors.principauxPP);

		List<SourcierPur> list = new ArrayList<>();

		final MovingWindow<ForFiscalPrincipalPP> iter = new MovingWindow<>(fors.principauxPP);
		while (iter.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipalPP> snapshot = iter.next();
			final ForFiscalPrincipalPP ffp = snapshot.getCurrent();
			final boolean forVaudois = ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;

			// de manière générale, un for fiscal avec un mode d'imposition source va générer un assujettissement source
			if (ffp.getModeImposition().isSource() &&
					// [SIFISC-1769] l'assujettissement source hors-canton/hors-Suisse est seulement pris en compte d'un point de vue cantonal (= pas en cas de point de vue communes vaudoises)
					(noOfsCommunesVaudoises == null || (forVaudois && noOfsCommunesVaudoises.contains(ffp.getNumeroOfsAutoriteFiscale())))) {

				final ForFiscalPrincipalContext<ForFiscalPrincipalPP> ffpContext = new ForFiscalPrincipalContext<>(snapshot);
				final RegDate dateDebut = determineDateDebutAssujettissementSource(ffpContext, fractionnements);
				final RegDate dateFin = determineDateFinAssujettissementSource(ffpContext, fractionnements);

				if (RegDateHelper.isBeforeOrEqual(dateDebut, dateFin, NullDateBehavior.LATEST)) {
					// on ne fait pas de distinction entre les modes d'imposition source et mixte, car du point de vue 'source' la partie 'rôle' du mode d'imposition mixte n'existe pas
					Data a = new Data(dateDebut, dateFin, ffp.getMotifOuverture(), ffp.getMotifFermeture(), Type.SourcierPur, ffp.getTypeAutoriteFiscale());

					// on fractionne l'assujettissement, si nécessaire
					a = fractionner(a, ffp, fractionnements);
					if (a == null) {
						continue;
					}

					list.add(new SourcierPur(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut, COMMUNE_ANALYZER));
				}
			}
		}

		// Dans certains cas particuliers (voir test 'testDeterminerSourcierPassageOrdinaireUnJourPuisRetourSourcier'), deux assujettissements
		// source peuvent se chevaucher. Dans ce cas-là, on privilégie un assujettissement vaudois si possible; autrement on choisit le plus récent.
		list = compacterAssujettissementsSource(list);

		return DateRangeHelper.collate(list,
		                               SourcierPur::isCollatable,
		                               SourcierPur::collate);
	}

	/**
	 * Détecte si deux assujettissements source se chevauchent, et si c'est le cas, privilégie l'assujettissement vaudois. Si les deux assujettissements possède la même autorité fiscale, privilégie
	 * l'assujettissement le plus récent.
	 *
	 * @param list une liste d'assujettissements source
	 * @return la liste d'assujettissements source, modifié ou non, selon les cas.
	 */
	private static List<SourcierPur> compacterAssujettissementsSource(List<SourcierPur> list) {

		boolean modified;
		do {
			modified = false;

			SourcierPur previous = null;
			for (SourcierPur current : list) {
				if (previous != null) {
					if (DateRangeHelper.intersect(previous, current)) { // on a trouvé un assujettissement qui chevauche le précédent
						if (previous.getTypeAutoriteFiscalePrincipale() == current.getTypeAutoriteFiscalePrincipale() || current.getTypeAutoriteFiscalePrincipale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
							list = overrideCurrentOnPrevious(list, current);
						}
						else {
							list = overrideCurrentOnNext(list, previous, current);
						}
						modified = true;
						break;
					}
				}
				previous = current;
			}

		}
		while (modified);

		return list;
	}

	private static List<SourcierPur> overrideCurrentOnPrevious(List<SourcierPur> list, SourcierPur current) {

		final OverrideAssujettissementCallback<SourcierPur> callback = new OverrideAssujettissementCallback<>();

		// comme les assujettissements après l'assujettissement courant peuvent se chevaucher, on découpe la liste
		// en deux : une première liste 'entête' considérée comme sûre, et en seconde qui contient le tout-venant.
		final List<SourcierPur> entete = new ArrayList<>();
		final List<SourcierPur> queue = new ArrayList<>();
		boolean bascule = false;

		for (SourcierPur sp : list) {
			if (sp == current) {
				bascule = true;
				continue;
			}
			if (!bascule) {
				entete.add(sp);
			}
			else {
				queue.add(sp);
			}
		}

		// on ajoute l'assujettissement courant
		list = DateRangeHelper.override(entete, Collections.singletonList(current), callback);
		// et on finit avec le reste
		list.addAll(queue);

		return list;
	}

	private static List<SourcierPur> overrideCurrentOnNext(List<SourcierPur> list, SourcierPur current, SourcierPur next) {

		final OverrideAssujettissementCallback<SourcierPur> callback = new OverrideAssujettissementCallback<>();

		// comme les assujettissements après l'assujettissement courant peuvent se chevaucher, on découpe la liste
		// en deux : une première liste 'entête' considérée comme sûre, et en seconde qui contient le tout-venant.
		final List<SourcierPur> entete = new ArrayList<>();
		final List<SourcierPur> queue = new ArrayList<>();
		boolean bascule = false;

		for (SourcierPur sp : list) {
			if (sp == current || sp == next) {
				bascule = true;
				continue;
			}
			if (!bascule) {
				entete.add(sp);
			}
			else {
				queue.add(sp);
			}
		}

		// on commence par ajouter l'assujettissement suivant (des fois qu'il chevauche un des assujettissements de l'entête)
		list = DateRangeHelper.override(entete, Collections.singletonList(next), callback);
		// puis on ajouter l'assujettissement courant
		list = DateRangeHelper.override(list, Collections.singletonList(current), callback);
		// et on finit avec le reste
		list.addAll(queue);

		return list;
	}

	private static RegDate determineDateDebutAssujettissementSource(ForFiscalPrincipalContext<ForFiscalPrincipalPP> ffpContext, Fractionnements<ForFiscalPrincipalPP> fractionnements) {

		final ForFiscalPrincipalPP precedent = ffpContext.getPrevious();
		final ForFiscalPrincipalPP courant = ffpContext.getCurrent();
		final RegDate debut = courant.getDateDebut();

		final Fraction fraction = fractionnements.getAt(debut);

		// faut-il adapter la date de début ?
		if (courant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			// pays HS => pas d'arrondi
			return debut;
		}
		else if (fraction != null) {
			return fraction.getDate();
		}
		else if ((precedent == null || !precedent.getModeImposition().isSource()) && // début d'assujettissement source
				courant.getMotifOuverture() != MotifFor.VEUVAGE_DECES && // sauf en cas de décès
				!isDepartOuArriveeHorsSuisse(courant.getMotifOuverture())) { // [UNIREG-2155]
			// début d'assujettissement source => on arrondi au début du mois
			return RegDate.get(debut.year(), debut.month(), 1);
		}
		else {
			// cas normal
			return debut;
		}
	}

	private static RegDate determineDateFinAssujettissementSource(ForFiscalPrincipalContext<ForFiscalPrincipalPP> ffpContext, Fractionnements<ForFiscalPrincipalPP> fractionnements) {

		final ForFiscalPrincipalPP suivant = ffpContext.getNext();
		final ForFiscalPrincipalPP courant = ffpContext.getCurrent();
		final RegDate fin = courant.getDateFin();

		final Fraction fraction = (fin == null ? null : fractionnements.getAt(fin.getOneDayAfter()));

		// faut-il adapter la date de fin ?
		if (courant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			// pays HS => pas d'arrondi
			return fin;
		}
		else if (fraction != null) {
			return fraction.getDate().getOneDayBefore();
		}
		else if (fin != null && (suivant == null || !suivant.getModeImposition().isSource()) && courant.getMotifFermeture() == MotifFor.PERMIS_C_SUISSE) {
			return DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(fin.getOneDayAfter()).getOneDayBefore();
		}
		else if (fin != null &&
				(suivant == null || !suivant.getModeImposition().isSource()) && // fin d'assujettissement source
				courant.getMotifFermeture() != MotifFor.VEUVAGE_DECES && // sauf en cas de décès
				!isDepartOuArriveeHorsSuisse(courant.getMotifFermeture())) {
			// fin d'assujettissement source => on arrondi à la fin du mois
			return RegDate.get(fin.year(), fin.month(), 1).addMonths(1).getOneDayBefore();
		}
		else {
			// cas normal
			return fin;
		}
	}

	public static List<Assujettissement> determineRole(ContribuableImpositionPersonnesPhysiques ctb, ForsParType fors, Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
		// Détermination des données d'assujettissement brutes
		final Fractionnements<ForFiscalPrincipalPP> fractionnements = new FractionnementsRole(fors.principauxPP);
		final CasParticuliers casParticuliers = determineCasParticuliers(ctb, fors.principauxPP);

		final DataList domicile = determineAssujettissementDomicile(fors.principauxPP, fractionnements, casParticuliers, noOfsCommunesVaudoises);
		domicile.compacterNonAssujettissements(noOfsCommunesVaudoises != null); // SIFISC-2939
		AssujettissementHelper.assertCoherenceRanges(domicile);

		final List<Data> economique = determineAssujettissementEconomique(ctb, fors.secondaires, fractionnements, noOfsCommunesVaudoises);
		fusionne(domicile, economique);

		// Création des assujettissements finaux
		List<Assujettissement> assujettissements = instanciate(ctb, domicile);
		assujettissements = DateRangeHelper.collate(assujettissements);

		return assujettissements;
	}

	/**
	 * Cette méthode ajoute des fors fiscaux principaux fictifs, c'est-à-dire des fors qui devraient exister en fonction des caractéristiques des fors existants, mais qui manquent.
	 *
	 * @param forsPrincipaux les fors principaux (non-fictifs) du contribuable.
	 */
	private static void ajouteForsPrincipauxFictifs(List<ForFiscalPrincipalPP> forsPrincipaux) {

		List<ForFiscalPrincipalPP> forsFictifs = null;

		// [UNIREG-2444] si un for fiscal principal possède un motif d'ouverture d'obtention de permis C et qu'il n'y a pas de for fiscal principal précédent,
		// on suppose que le contribuable était sourcier et qu'il y a passage du rôle source pur au rôle ordinaire. Dans les faits pour ne pas casser l'algorithme général,
		// on ajoute artificiellement un for principal avec le mode d'imposition source.
		final MovingWindow<ForFiscalPrincipalPP> iter = new MovingWindow<>(forsPrincipaux);
		while (iter.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipalPP> snapshot = iter.next();
			final ForFiscalPrincipalPP current = snapshot.getCurrent();
			final ForFiscalPrincipalPP previous = snapshot.getPrevious();

			if (current.getMotifOuverture() == MotifFor.PERMIS_C_SUISSE && (previous == null || !DateRangeHelper.isCollatable(previous, current))) {
				// On ajoute un for principal source compris entre le début de l'année et la date d'ouverture du for principal courant. Cette période est raccourcie si nécessaire.
				final RegDate debut;
				final RegDate fin = current.getDateDebut().getOneDayBefore();
				if (previous == null) {
					debut = RegDate.get(current.getDateDebut().year(), 1, 1);
				}
				else {
					debut = RegDateHelper.maximum(RegDate.get(current.getDateDebut().year(), 1, 1), previous.getDateFin().getOneDayAfter(), NullDateBehavior.EARLIEST);
				}
				final ForFiscalPrincipalPP forFictif = new ForFiscalPrincipalPP(debut, MotifFor.INDETERMINE, fin, MotifFor.PERMIS_C_SUISSE, current.getNumeroOfsAutoriteFiscale(),
				                                                                current.getTypeAutoriteFiscale(), current.getMotifRattachement(), ModeImposition.SOURCE);
				if (forsFictifs == null) {
					forsFictifs = new ArrayList<>();
				}
				forsFictifs.add(forFictif);
			}
		}

		if (forsFictifs != null) {
			forsPrincipaux.addAll(forsFictifs);
			forsPrincipaux.sort(new DateRangeComparator<ForFiscalPrincipal>());
		}
	}

	private static class CasParticuliers {

		private final boolean menageCommun;
		private final Map<Integer, Mutation> mariagesDivorces = new HashMap<>();

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

		private final RegDate date;
		private final Type type;

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
	private static CasParticuliers determineCasParticuliers(ContribuableImpositionPersonnesPhysiques ctb, List<ForFiscalPrincipalPP> principaux) {
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
	private static DataList determineAssujettissementDomicile(List<ForFiscalPrincipalPP> principaux, Fractionnements<ForFiscalPrincipalPP> fractionnements, CasParticuliers casParticuliers,
	                                                          @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final DataList domicile = new DataList();

		// Détermine les assujettissements pour le rattachement de type domicile
		final MovingWindow<ForFiscalPrincipalPP> iter = new MovingWindow<>(principaux);
		while (iter.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipalPP> snapshot = iter.next();

			// on détermine les fors principaux qui précèdent et suivent immédiatement
			final ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal = new ForFiscalPrincipalContext<>(snapshot);

			// on détermine l'assujettissement pour le for principal courant
			Data a = determine(forPrincipal, fractionnements, noOfsCommunesVaudoises);
			if (a == null) {
				continue;
			}

			// on fractionne l'assujettissement, si nécessaire
			a = fractionner(a, forPrincipal.getCurrent(), fractionnements);
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
	private static Data traiterCasParticuliers(Data data, Fractionnements<ForFiscalPrincipalPP> fractions, CasParticuliers casParticuliers) {

		if (casParticuliers.isEmpty()) {
			return data;
		}

		RegDate debut = data.debut;
		RegDate fin = data.fin;

		if (debut != null) {
			final LimitesAssujettissement limites = LimitesAssujettissement.determine(debut, debut, fractions);
			final RegDate dernierFractionnement = limites == null ? null : limites.getLeft() == null ? null : limites.getLeft().getDate();

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
			final LimitesAssujettissement limites = LimitesAssujettissement.determine(fin, fin, fractions);
			final RegDate dernierFractionnement = limites == null ? null : limites.getLeft() == null ? null : limites.getLeft().getDate();
			final RegDate prochainFractionnement = limites == null ? null : limites.getRight() == null ? null : limites.getRight().getDate();

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
	 * Applique les régles de fractionnement sur l'assujettissement spécifié.
	 *
	 * @param a         un assujettissement
	 * @param ffp       le for principal à la source de l'assujettissement spécifié.
	 * @param fractions la liste des fractions
	 * @return un assujettissement, fractionné si nécessaire.
	 */
	private static Data fractionner(Data a, ForFiscalPrincipalPP ffp, Fractionnements<ForFiscalPrincipalPP> fractions) {

		if (fractions.isEmpty()) {
			return a;
		}

		// on détermine les fractionnements immédiatement à gauche et droite du for principal à la source
		// de l'assujettissement (logiquement, il n'est pas possible d'avoir un fractionnement à l'intérieur du for)
		final LimitesAssujettissement limites = LimitesAssujettissement.determine(ffp, fractions);
		final Fraction left = (limites == null ? null : limites.getLeft());
		final Fraction right = (limites == null ? null : limites.getRight());

		// on réduit l'assujettissement en conséquence
		if (left != null && left.getDate().isAfter(a.debut)) {
			a.debut = left.getDate();

			if (a.motifDebut == MotifAssujettissement.ARRIVEE_HC && left.getMotif() == MotifFor.DEPART_HS) {
				// dans le cas d'un départ HS et d'une arrivée HC, on ne veut pas collater les deux assujettissements,
				// il faut donc garder le motif de début sans changement (voir isCollatable())
			}
			else {
				a.motifDebut = MotifAssujettissement.of(left.getMotif());
			}
		}

		if (right != null && right.getDate().isBeforeOrEqual(a.fin)) {
			a.fin = right.getDate().getOneDayBefore();
			a.motifFin = MotifAssujettissement.of(right.getMotif());
		}

		return a;
	}

	/**
	 * Détermine les données d'assujettissements brutes pour les rattachements de type économique.
	 *
	 * @param ctb                    le contribuable
	 * @param secondaires            les fors secondaires d'un contribuable
	 * @param fractionnements        la liste des fractionnements d'assujettissement calculés lors de l'analyse des fors principaux
	 * @param noOfsCommunesVaudoises si renseigné, détermine le assujettissements du point de vue des communes spécifiées; si null, détermine les assujettissements du point de vue cantonal.
	 * @return la liste des assujettissements brutes calculés
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static List<Data> determineAssujettissementEconomique(ContribuableImpositionPersonnesPhysiques ctb, List<ForFiscalSecondaire> secondaires, Fractionnements fractionnements, @Nullable Set<Integer> noOfsCommunesVaudoises) throws
			AssujettissementException {
		List<Data> economique = new ArrayList<>();
		// Détermine les assujettissements pour le rattachement de type économique
		for (ForFiscalSecondaire f : secondaires) {
			final Data a = determine(ctb, f, fractionnements, noOfsCommunesVaudoises);
			if (a != null) {
				economique.add(a);
			}
		}
		return economique;
	}

	private static boolean isDepartOuArriveeHorsSuisse(MotifFor motif) {
		return motif == MotifFor.DEPART_HS || motif == MotifFor.ARRIVEE_HS;
	}

	private static boolean is31Decembre(RegDate date) {
		return date.month() == 12 && date.day() == 31;
	}

	protected static boolean roleSourcierPur(ForFiscalPrincipalPP forFiscal) {
		return forFiscal.getModeImposition() == ModeImposition.SOURCE;
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
	protected static boolean isDepartOuArriveeHorsSuisse(ForFiscalPrincipalPP left, ForFiscalPrincipalPP right) {
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
	protected static boolean isArriveeHCApresDepartHSMemeAnnee(ForFiscalPrincipal current) {
		return current.getDateDebut().year() == current.getDateFin().year() && current.getMotifOuverture() == MotifFor.DEPART_HS && current.getMotifFermeture() == MotifFor.ARRIVEE_HC;
	}

	/**
	 * [UNIREG-2759] Détermine si le for fiscal se ferme avec un départ hors-canton la même année qu'il s'est ouvert avec une arrivée de hors-Suisse
	 * [SIFISC-12325] Attention : en cas de retour HC dans la même PF (sans passage HS intermédiaire), il doit y avoir fractionnement quand-même
	 *
	 * @param ctxt context à tester (depuis current vers le futur)
	 * @return <b>vrai</b> si le for fiscal se ferme avec un départ hors-canton la même année qu'une arrivée de hors-Suisse; <b>faux</b> autrement.
	 */
	protected static boolean isDepartHCApresArriveHSMemeAnnee(ForFiscalPrincipalContext<ForFiscalPrincipalPP> ctxt) {

		final ForFiscalPrincipalPP current = ctxt.getCurrent();
		if (current == null) {
			return false;
		}

		final RegDate fin = current.getDateFin();
		if (fin == null) {
			return false;
		}

		if (fin.year() != current.getDateDebut().year() || fin.getOneDayAfter().year() != fin.year()) {
			return false;
		}

		if (current.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return false;
		}

		if (ctxt.hasNext()) {
			final List<ForFiscalPrincipalPP> thisYear = extractDebutDansPeriodeFiscale(fin.year(), ctxt.getAllNext());
			for (int i = 0 ; i < thisYear.size() ; ++ i) {
				// [SIFISC-12325] on s'arrête au premier for HS de toute façon, et ce qui nous intéresse au final, c'est le type d'autorité fiscale du
				// for juste avant le départ HS ou la fin d'assujettissement
				final ForFiscalPrincipal ffp = thisYear.get(i);
				if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
					// si c'est le premier for, il s'agit d'une arrivée puis d'un départ HS tout de suite -> il n'y a pas de départ HS qui tienne
					if (i == 0) {
						return false;
					}
					else {
						final ForFiscalPrincipal precedent = thisYear.get(i - 1);
						return precedent.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
					}
				}
			}

			// si on est ici, c'est qu'on n'a pas trouvé de for HS... où sommes nous donc en fin d'année ?
			final ForFiscalPrincipal lastInYear = thisYear.get(thisYear.size() - 1);
			return lastInYear.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
		}
		else {
			// pas de for fiscal immédiatement suivant, on se rabat sur le motif de fermeture
			return current.getMotifFermeture() == MotifFor.DEPART_HC;
		}
	}

	protected static boolean isMixte2Vaudois(ForFiscalPrincipalContext<ForFiscalPrincipalPP> ctxt) {
		final ForFiscalPrincipalPP ffp = ctxt.getCurrent();
		return ffp != null && ffp.getModeImposition() == ModeImposition.MIXTE_137_2 && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	/**
	 * Extrait de la collection donnée tous les fors dont la date de début est dans la PF donnée
	 * @param pf période fiscale qui nous intéresse
	 * @param fors liste des fors à analyser
	 * @return une nouvelle liste avec tous les fors de la source qui ont une date de début dans la période fiscale donnée (l'ordre est conservé)
	 */
	private static <F extends ForFiscalPrincipal> List<F> extractDebutDansPeriodeFiscale(int pf, List<F> fors) {
		final List<F> extracted = new ArrayList<>(fors.size());
		for (F candidate : fors) {
			if (candidate.getDateDebut().year() == pf) {
				extracted.add(candidate);
			}
		}
		return extracted;
	}

	/**
	 * @param current le for fiscal courant
	 * @param next    le for fiscal immédiatement suivant (peut être nul)
	 * @return <b>true</b> si un départ hors-canton est détectée entre les forts fiscaux spécifiés. Cette méthode s'assure que les types d'autorité fiscales sont cohérentes de manière à détecter les faux
	 *         départs hors-canton.
	 */
	protected static boolean isDepartDansHorsCanton(@NotNull ForFiscalPrincipal current, @Nullable ForFiscalPrincipal next) {
		if (next == null) {
			return current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && current.getMotifFermeture() == MotifFor.DEPART_HC;
		}
		else {
			return current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC;
		}
	}

	/**
	 * Détermine la date de début d'un assujettissement induit par un for fiscal principal.
	 *
	 * @param forPrincipal    le for fiscal principal dont on veut déterminer la date de début d'assujettissement
	 * @param fractionnements les fractionnements déterminés par avance
	 * @return la date de début de l'assujettissement
	 */
	private static RegDate determineDateDebutAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal, Fractionnements<ForFiscalPrincipalPP> fractionnements) {

		final RegDate debut;
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPP previous = forPrincipal.getPrevious();

		final MotifFor motifOuverture = current.getMotifOuverture();
		final ModeImposition modeImposition = current.getModeImposition();

		final Fraction fraction = fractionnements.getAt(current.getDateDebut());

		if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			debut = current.getDateDebut();
		}
		else if (fraction != null) {
			debut = fraction.getDate();
		}
		else if ((previous == null || previous.getModeImposition() == ModeImposition.SOURCE) && modeImposition.isRole() && motifOuverture == MotifFor.PERMIS_C_SUISSE) {
			// [SIFISC-8095] l'obtention d'un permis C ou nationalité suisse doit fractionner la période d'assujettissement et on arrondi la date de début au 1er du mois suivant
			debut = DecalageDateHelper.getDateDebutAssujettissementOrdinaireApresPermisCNationaliteSuisse(current.getDateDebut());
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
	 * @param forPrincipal    le for fiscal principal dont on veut déterminer la date de fin d'assujettissement
	 * @param fractionnements les fractionnements déterminés par avance
	 * @return la date de fin de l'assujettissement
	 */
	private static RegDate determineDateFinAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal, Fractionnements<ForFiscalPrincipalPP> fractionnements) {

		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final RegDate fin = current.getDateFin();

		final Fraction fraction = (fin == null ? null : fractionnements.getAt(fin.getOneDayAfter()));

		final RegDate afin;
		if (fin == null || current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			afin = fin;
		}
		else if (fraction != null && (fraction.getMotifFermeture() != null || fraction.getMotifOuverture() != MotifFor.VEUVAGE_DECES)) {
			// Dans le cas d'un for fiscal qui s'ouvre par un motif 'veuvage', le cas standard est que le contribuable appartienne à un ménage-commun le jour précédent.
			// Lorsque deux fors principaux se touchent, l'existence de rapports d'appartenance ménage est interdite par les régles de validation. Cependant, si le second
			// for principal s'ouvre avec un motif 'veuvage', alors on considère que le motif d'ouverture prime sur l'absence de rapport d'appartenance ménage et on suppose
			// l'existence d'un ménage-commun sur lequel l'assujettissement du contribuable courant existe (selon conversation par email entre Manuel Siggen et
			// David Radelfinger du 12 décembre 2012).
			//
			// La conséquence pratique est que le fractionnement induit par le motif d'ouverture 'veuvage' est ignoré
			// dans le calcul de la date de fin d'assujettissement du for précédent.
			afin = fraction.getDate().getOneDayBefore();
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
	private static RegDate determineDateDebutNonAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {

		final ForFiscalPrincipalPP previous = forPrincipal.getPrevious();
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();

		final RegDate debut = current.getDateDebut();

		final RegDate adebut;
		if (isDepartOuArriveeHorsSuisse(previous, current)
				&& (!isDepartDepuisOuArriveeVersVaud(forPrincipal.slideToPrevious()) || (isDepartHCApresArriveHSMemeAnnee(forPrincipal) && !isMixte2Vaudois(forPrincipal.slideToPrevious())))) {
			// cas du départ/arrivée HS depuis hors-canton : on ignore le fractionnement est on applique l'assujettissement depuis le début de l'année
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			// [SIFISC-14388] le cas d'une arrivée HS de mixte 2 doit fractionner à l'arrivée, même en cas de départ HC ensuite dans la même année
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
	private static RegDate determineDateFinNonAssujettissement(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal) {

		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();
		final ForFiscalPrincipalPP next = forPrincipal.getNext();

		final RegDate fin = current.getDateFin();

		final RegDate afin;
		if (fin == null) {
			afin = null;
		}
		else if (isDepartOuArriveeHorsSuisse(current, next)
				&& (!isDepartDepuisOuArriveeVersVaud(forPrincipal) || (isDepartHCApresArriveHSMemeAnnee(forPrincipal.slideToNext()) && !isMixte2Vaudois(forPrincipal.slideToNext())))) {
			// cas du départ/arrivée HS depuis hors-canton : on ignore le fractionnement est on applique l'assujettissement jusqu'au 31 décembre précédent
			// [UNIREG-1742] le départ hors-Suisse depuis hors-canton ne doit pas fractionner la période d'assujettissement (car le rattachement économique n'est pas interrompu)
			// [UNIREG-2759] l'arrivée de hors-Suisse ne doit pas fractionner si le for se ferme dans la même année avec un départ hors-canton
			// [SIFISC-14388] le cas d'une arrivée HS de mixte 2 doit fractionner à l'arrivée, même en cas de départ HC ensuite dans la même année
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

		final RegDate dateFin = forPrincipal.getCurrent().getDateFin();
		if (forPrincipal.getNext() != null || dateFin == null) {
			return false;
		}

		// (msi 7.11.2011), c'est pas génial de remonter sur le tiers pour récupérer tous les fors fiscaux, c'est clair. Mais d'un
		// autre côté, on le fait tellement peu souvent dans le cas réel que ça n'a pas d'impact négatif sur les performances.
		final List<? extends ForFiscalPrincipal> all = forPrincipal.getCurrent().getTiers().getForsFiscauxPrincipauxActifsSorted();

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
	private static class Data implements CollatableDateRange<Data> {

		RegDate debut;
		RegDate fin;
		MotifAssujettissement motifDebut;
		MotifAssujettissement motifFin;
		Type type;
		final TypeAutoriteFiscale typeAut;

		/**
		 * Collections des motifs d'ouverture de for qui ne donnent normalement pas lieu à un début d'assujettissement
		 * ou qui doivent, le cas échéant, laisser la priorité au motif d'ouverture du for "économique" - si existant à la même date - dans la méthode
		 * {@link #merge(ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement, java.util.Set) merge}
		 */
		@SuppressWarnings({"deprecation"})
		private static final Set<MotifAssujettissement> DEBUT_ASSUJETTISSEMENT = EnumSet.of(MotifAssujettissement.INDETERMINE,
		                                                                                    MotifAssujettissement.VENTE_IMMOBILIER,
		                                                                                    MotifAssujettissement.ANNULATION,
		                                                                                    MotifAssujettissement.FIN_ACTIVITE_DIPLOMATIQUE,
		                                                                                    MotifAssujettissement.FIN_EXPLOITATION);

		/**
		 * Collections des motifs de fermeture de for qui ne donnent normalement pas lieu à une fin d'assujettissement
		 * ou qui doivent, le cas échéant, laisser la priorité au motif de fermeture du for "économique" - si existant à la même date - dans la méthode
		 * {@link #merge(ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement, java.util.Set) merge}
		 */
		@SuppressWarnings({"deprecation"})
		private static final Set<MotifAssujettissement> FIN_ASSUJETTISSEMENT = EnumSet.of(MotifAssujettissement.INDETERMINE,
		                                                                                  MotifAssujettissement.ACHAT_IMMOBILIER,
		                                                                                  MotifAssujettissement.REACTIVATION,
		                                                                                  MotifAssujettissement.DEBUT_ACTIVITE_DIPLOMATIQUE,
		                                                                                  MotifAssujettissement.DEBUT_EXPLOITATION);

		private Data(RegDate debut, RegDate fin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin, Type type, TypeAutoriteFiscale typeAut) {
			this.debut = debut;
			this.fin = fin;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
			this.type = type;
			this.typeAut = typeAut;
		}

		private Data(RegDate debut, RegDate fin, MotifFor motifDebut, MotifFor motifFin, Type type, TypeAutoriteFiscale typeAut) {
			this(debut, fin, MotifAssujettissement.of(motifDebut), MotifAssujettissement.of(motifFin), type, typeAut);
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
		public RegDate getDateDebut() {
			return debut;
		}

		@Override
		public RegDate getDateFin() {
			return fin;
		}

		@Override
		public boolean isCollatable(Data next) {
			return fin == next.debut.getOneDayBefore() && motifFin == next.motifDebut && type == next.type && typeAut == next.typeAut;
		}

		@Override
		public Data collate(Data next) {
			return new Data(debut, next.fin, motifDebut, next.motifFin, type, typeAut);
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
				list = new ArrayList<>(3);
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
		private static MotifAssujettissement merge(RegDate dateDomicile, MotifAssujettissement motifDomicile, RegDate dateEco, MotifAssujettissement motifEco, Set<MotifAssujettissement> motifsDomicileNonPrioritaires) {
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
	 * @param fractionnements        les fractionnements déterminés par avance
	 * @param noOfsCommunesVaudoises si renseigné, détermine le assujettissements du point de vue des communes spécifiées; si null, détermine les assujettissements du point de vue cantonal.  @return les
	 *                               données d'assujettissement, ou <b>null</b> si le for principal n'induit aucun assujettissement
	 * @throws AssujettissementException en cas d'impossibilité de calculer l'assujettissement
	 */
	private static Data determine(ForFiscalPrincipalContext<ForFiscalPrincipalPP> forPrincipal, Fractionnements<ForFiscalPrincipalPP> fractionnements, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

		final Data data;
		final ForFiscalPrincipalPP current = forPrincipal.getCurrent();

		if (!current.getModeImposition().isRole()) {
			// seule la vue 'rôle' nous intéresse
			return null;
		}

		switch (current.getTypeAutoriteFiscale()) {
		case COMMUNE_OU_FRACTION_VD: {

			final RegDate adebut = determineDateDebutAssujettissement(forPrincipal, fractionnements);
			final RegDate afin = determineDateFinAssujettissement(forPrincipal, fractionnements);

			if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
				final MotifRattachement motifRattachement = current.getMotifRattachement();

				if (noOfsCommunesVaudoises != null && !noOfsCommunesVaudoises.contains(current.getNumeroOfsAutoriteFiscale())) {
					// [SIFISC-1769] le for principal est sur une autre commune : non-assujetti du point de vue des communes vaudoises spécifiées.
					data = new Data(adebut, afin, (MotifAssujettissement) null, null, Type.NonAssujetti, current.getTypeAutoriteFiscale());
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

			final RegDate adebut = determineDateDebutNonAssujettissement(forPrincipal);
			final RegDate afin = determineDateFinNonAssujettissement(forPrincipal);

			if (RegDateHelper.isBeforeOrEqual(adebut, afin, NullDateBehavior.LATEST)) {
				data = new Data(adebut, afin, current.getMotifOuverture(), current.getMotifFermeture(), Type.NonAssujetti, current.getTypeAutoriteFiscale());
			}
			else {
				// pas d'assujettissement
				data = null;
			}
			break;
		}

		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu = [" + current.getTypeAutoriteFiscale() + ']');
		}

		return data;
	}

	private static Data determine(ContribuableImpositionPersonnesPhysiques ctb, ForFiscalSecondaire ffs, Fractionnements<?> fractionnements, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {

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
		else if (AssujettissementHelper.isForPrincipalHorsSuisse(ctb, debut)) {
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
		else if (AssujettissementHelper.isForPrincipalHorsSuisse(ctb, fin)) {
			afin = fin;
		}
		else {
			afin = getProchain31Decembre(fin);
		}

		// Dans tous les cas, si on trouve une date de fractionnement entre la date réelle du début du for et la date de début de l'assujettissement,
		// on adapte cette dernière en conséquence. Même chose pour les dates de fin d'assujettissement.
		for (Fraction f : fractionnements) {
			if (RegDateHelper.isBetween(f.getDate(), adebut, debut, NullDateBehavior.LATEST)) {
				adebut = f.getDate();
			}

			if (fin != null && RegDateHelper.isBetween(f.getDate().getOneDayBefore(), fin, afin, NullDateBehavior.LATEST)) {
				afin = f.getDate().getOneDayBefore();
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

		domicile.sort(new DateRangeComparator<>());
	}

	private static List<Assujettissement> instanciate(ContribuableImpositionPersonnesPhysiques ctb, List<Data> all) {
		final List<Assujettissement> assujettissements = new ArrayList<>(all.size());
		for (Data a : all) {
			final Assujettissement assujettissement;
			switch (a.type) {
			case VaudoisOrdinaire:
				assujettissement = new VaudoisOrdinaire(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, COMMUNE_ANALYZER);
				break;
			case VaudoisDepense:
				assujettissement = new VaudoisDepense(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, COMMUNE_ANALYZER);
				break;
			case Indigent:
				assujettissement = new Indigent(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, COMMUNE_ANALYZER);
				break;
			case HorsCanton:
				assujettissement = new HorsCanton(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, COMMUNE_ANALYZER);
				break;
			case HorsSuisse:
				assujettissement = new HorsSuisse(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, COMMUNE_ANALYZER);
				break;
			case SourcierPur:
				assujettissement = new SourcierPur(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut, COMMUNE_ANALYZER);
				break;
			case SourcierMixte1:
				assujettissement = new SourcierMixteArt137Al1(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut, COMMUNE_ANALYZER);
				break;
			case SourcierMixte2:
				assujettissement = new SourcierMixteArt137Al2(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, a.typeAut, COMMUNE_ANALYZER);
				break;
			case DiplomateSuisse:
				assujettissement = new DiplomateSuisse(ctb, a.debut, a.fin, a.motifDebut, a.motifFin, COMMUNE_ANALYZER);
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

	protected static <F extends ForFiscalPrincipal> boolean isDepartDepuisOuArriveeVersVaud(ForFiscalPrincipalContext<F> ctxt) {
		final F current = ctxt.getCurrent();
		final F next = ctxt.getNext();

		// si au moins l'un des deux fors est vaudois, il n'y a pas photo, c'est OUI
		if ((current != null && current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
				|| (next != null && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)) {
			return true;
		}

		// [SIFISC-12325]
		// si on est ici, c'est qu'aucun des deux fors fiscaux n'est vaudois
		// si l'un des deux est hors-Suisse et l'autre hors-canton, il faut vérifier que on n'arrive pas sur un vaudois du côté du hors-Canton dans le même période fiscale sans passer par HS
		if (current != null && next != null) {
			if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
				return isArriveeVaudDepuisHorsCantonMemePeriodeApres(ctxt, next.getDateDebut().year());
			}
			else if (current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				return isDepartVaudVersHorsCantonMemePeriodeAvant(ctxt, current.getDateFin().year());
			}
		}
		else if (current == null && next != null && next.getMotifOuverture() == MotifFor.ARRIVEE_HS && next.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
			return isArriveeVaudDepuisHorsCantonMemePeriodeApres(ctxt, next.getDateDebut().year());
		}
		else if (next == null && current != null && current.getMotifFermeture() == MotifFor.DEPART_HS && current.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {
			return isDepartVaudVersHorsCantonMemePeriodeAvant(ctxt, current.getDateFin().year());
		}

		return false;
	}

	private static boolean isArriveeVaudDepuisHorsCantonMemePeriodeApres(ForFiscalPrincipalContext<?> ctxt, int pf) {
		for (ForFiscalPrincipal ffp : ctxt.getAllNext()) {
			if (ffp.getDateDebut().year() != pf || ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				break;
			}
			else if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDepartVaudVersHorsCantonMemePeriodeAvant(ForFiscalPrincipalContext<?> ctxt, int pf) {
		for (ForFiscalPrincipal ffp : ctxt.getAllPrevious()) {
			if (ffp.getDateFin().year() != pf || ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				break;
			}
			else if (ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				return true;
			}
		}
		return false;
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
		public boolean removeAll(@NotNull Collection<?> c) {
			throw new NotImplementedException();
		}

		@Override
		public boolean retainAll(@NotNull Collection<?> c) {
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
				List<Data> nonA = new ArrayList<>(nonAssujettissementCount);
				final List<Data> vraiA = new ArrayList<>(size());
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
			final List<Data> merged = DateRangeHelper.merge(list, DateRangeHelper.MergeMode.INTERSECTING, new DateRangeHelper.MergeCallback<Data>() {
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
			return DateRangeHelper.collate(merged);
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

	@SuppressWarnings("unchecked")
	private static class OverrideAssujettissementCallback<T extends Assujettissement> implements DateRangeHelper.AdapterCallbackExtended<T> {
		@Override
		public T adapt(Assujettissement range, RegDate debut, RegDate fin) {
			throw new IllegalArgumentException("Ne devrait pas être appelée");
		}

		@Override
		public T adapt(Assujettissement range, RegDate debut, Assujettissement surchargeDebut, RegDate fin, Assujettissement surchargeFin) {

			final MotifAssujettissement motifDebut;
			if (debut == null) {
				// pas de surcharge sur le début
				debut = range.getDateDebut();
				motifDebut = range.getMotifFractDebut();
			}
			else {
				// surcharge du début
				motifDebut = surchargeDebut.getMotifFractFin();
			}

			final MotifAssujettissement motifFin;
			if (fin == null) {
				// pas de surcharge sur la fin
				fin = range.getDateFin();
				motifFin = range.getMotifFractFin();
			}
			else {
				// surcharge de la fin
				motifFin = surchargeFin.getMotifFractDebut();
			}

			return (T) range.duplicate(debut, fin, motifDebut, motifFin);
		}

		@Override
		public T duplicate(Assujettissement range) {
			return (T) range.duplicate(range.getDateDebut(), range.getDateFin(), range.getMotifFractDebut(), range.getMotifFractFin());
		}
	}
}
