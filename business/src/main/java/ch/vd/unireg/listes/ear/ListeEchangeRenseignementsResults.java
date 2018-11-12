package ch.vd.unireg.listes.ear;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.ListesResults;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.HorsCanton;
import ch.vd.unireg.metier.assujettissement.HorsSuisse;
import ch.vd.unireg.metier.assujettissement.Sourcier;
import ch.vd.unireg.metier.assujettissement.TypeAssujettissement;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Classe de résultats du batch d'extraction des assujettis d'une période fiscale
 */
public class ListeEchangeRenseignementsResults extends ListesResults<ListeEchangeRenseignementsResults> {

	private final int anneeFiscale;

	private final List<InfoIdentifiantCtb> ppIdentifies = new LinkedList<>();
	private final List<InfoCtbIgnore> ppIgnores = new LinkedList<>();
	private int nbCtbPpIdentifies = 0;

	private final List<InfoIdentifiantCtb> pmIdentifies = new LinkedList<>();
	private final List<InfoCtbIgnore> pmIgnores = new LinkedList<>();
	private int nbCtbPmIdentifies = 0;
	private final AssujettissementService assujettissementService;
	private final boolean avecContribuablesPP;

	private final boolean avecContribuablesPM;

	public abstract static class InfoCtb<T extends InfoCtb> implements Comparable<T> {
		public final long noCtb;

		public InfoCtb(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public int compareTo(@NotNull T o) {
			return Long.compare(noCtb, o.noCtb);
		}
	}


	public static class InfoIdentifiantCtb extends InfoCtb<InfoIdentifiantCtb>{

		public final String identifiant;

		public InfoIdentifiantCtb(long noCtb, String identifiant) {
			super(noCtb);
			this.identifiant = identifiant;
		}
	}


	public enum CauseIgnorance {
		NON_ASSUJETTI("Non assujetti"),
		NON_ASSUJETTI_VAUDOIS("Pas d'assujettissement vaudois sur la période"),
		ABSENCE_FOR_VAUDOIS("Pas de for vaudois à la date de fin de l'assujetissement calculé"),
		ABSENCE_RAPPORT_TRAVAIL_SOURCIER("Pas de rapport de travail pour le ctb source"),
		ABSENCE_DOMICILE_VAUDOIS("Pas de Domicile vaudois sur la période pour le ctb source"),
		ABSENCE_NAVS("Pas de numéro AVS trouvé pour ce contribuable"),
		ABSENCE_IDE("Pas de numéro IDE trouvé pour cette entreprise");
		public String getDescription() {
			return description;
		}

		public final String description;

		CauseIgnorance(String description) {
			this.description = description;
		}
	}

	public static class InfoCtbIgnore extends InfoCtb<InfoCtbIgnore> {
		public final CauseIgnorance cause;

		public InfoCtbIgnore(long noCtb, CauseIgnorance cause) {
			super(noCtb);
			this.cause = cause;
		}
	}

	public ListeEchangeRenseignementsResults(RegDate dateTraitement, int nombreThreads, int anneeFiscale, final boolean avecContribuablesPP, final boolean avecContribuablesPM, TiersService tiersService,
	                                         AssujettissementService assujettissementService, AdresseService adresseService) {
		super(dateTraitement, nombreThreads, tiersService, adresseService);
		this.anneeFiscale = anneeFiscale;
		this.assujettissementService = assujettissementService;
		this.avecContribuablesPP = avecContribuablesPP;
		this.avecContribuablesPM = avecContribuablesPM;
	}

	@Override
	public void addContribuable(Contribuable ctb) throws AssujettissementException, DonneesCivilesException {

		final CauseIgnorance causeIgnorance = determineCauseIgnorance(ctb);

		if (causeIgnorance != null) {
			if (ctb instanceof PersonnePhysique || ctb instanceof MenageCommun) {
				ppIgnores.add(new InfoCtbIgnore(ctb.getNumero(), causeIgnorance));
			}
			else if (ctb instanceof Entreprise) {
				pmIgnores.add(new InfoCtbIgnore(ctb.getNumero(), causeIgnorance));
			}


		}
		else {
			sauvegarderIdentifiant(ctb);
		}
	}

	private void sauvegarderIdentifiant(Contribuable ctb) {
		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			traiterPersonnePhysique(pp);
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(mc, anneeFiscale);
			if (ensembleTiersCouple != null) {
				final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
				final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
				traiterPersonnePhysique(principal);
				if (conjoint != null) {
					traiterPersonnePhysique(conjoint);
				}


			}


		}
		else if (ctb instanceof Entreprise) {
			final Entreprise entreprise = (Entreprise) ctb;
			final String numeroIDE = tiersService.getNumeroIDE(entreprise);
			if (numeroIDE != null) {
				++nbCtbPmIdentifies;
				pmIdentifies.add(new InfoIdentifiantCtb(entreprise.getNumero(), StringUtils.remove(numeroIDE, "CHE")));

			}
			else {
				pmIgnores.add(new InfoCtbIgnore(entreprise.getNumero(), CauseIgnorance.ABSENCE_IDE));
			}
		}
	}

	private void traiterPersonnePhysique(PersonnePhysique pp) {
		final String numeroAssureSocial = tiersService.getNumeroAssureSocial(pp);
		if (numeroAssureSocial != null) {
			++nbCtbPpIdentifies;
			ppIdentifies.add(new InfoIdentifiantCtb(pp.getNumero(), numeroAssureSocial));

		}
		else {
			ppIgnores.add(new InfoCtbIgnore(pp.getNumero(), CauseIgnorance.ABSENCE_NAVS));
		}
	}


	public CauseIgnorance determineCauseIgnorance(Contribuable ctb) throws AssujettissementException, DonneesCivilesException {

		final List<Assujettissement> assujettissements = assujettissementService.determine(ctb, anneeFiscale);



		//Presence d'un assujetissement sur la PF
		if (CollectionUtils.isEmpty(assujettissements)) {
			return CauseIgnorance.NON_ASSUJETTI;
		}
		//Il faut que ça soit un assujettissement vaudois
	//	final List<Assujettissement> assujettissementsVaudois = assujettissements;
		final List<Assujettissement> assujettissementsVaudois = assujettissements.stream()
				.filter(this::isAutoriteVaudoise)
				.collect(Collectors.toList());

		//SIFISC-29785
		if (assujettissementsVaudois.isEmpty()) {
			return CauseIgnorance.NON_ASSUJETTI_VAUDOIS;
		}
		//On va trier les assujettissements pour choper la date de fin du dernier qui peut être null
		assujettissementsVaudois.sort(DateRangeComparator::compareRanges);
		final Assujettissement lastAssujetissement = assujettissementsVaudois.get(assujettissementsVaudois.size() - 1);
		//vérifcation de la présence d'un for en date de fin de l'assujetissement, peut être null
		List<ForFiscal> forValides = ctb.getForsFiscauxValidAt(lastAssujetissement.getDateFin());
		if (absenceForPrincipalVaudois(forValides)) {
			return CauseIgnorance.ABSENCE_FOR_VAUDOIS;
		}

		//Pour les sourcier, on regarde en plus la présence d'un rapport de travail d'au moins 1 journée sur la PF ainsi que l'existence d'une adresse de domicile valide sur vaud au moins 1 jour sur la PF
		if (isAssujettissementSourcier(lastAssujetissement)) {
			if (absenceRapportTravail(ctb, anneeFiscale)) {
				return CauseIgnorance.ABSENCE_RAPPORT_TRAVAIL_SOURCIER;
			}

			if (absenceDomicileVaudois(ctb, anneeFiscale)) {
				return CauseIgnorance.ABSENCE_DOMICILE_VAUDOIS;
			}

		}

		//aucune cause trouvée, on a un assujetti dont les informations vont être transmises
		return null;
	}

	/**
	 * Permet de savoir su l'assujettissement analysé à un type d'autorité fiscale vaudois
	 * @param a l'assujettissement analysé
	 * @return true si l'autorité est vaudoise, false sinon
	 */
	private boolean isAutoriteVaudoise(Assujettissement a){
		if (a instanceof HorsCanton || a instanceof HorsSuisse) {
			return false;
		}
		if (a instanceof Sourcier) {
			Sourcier assujSource = (Sourcier)a;
			return ((Sourcier) a).getTypeAutoriteFiscalePrincipale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
		}

		//Les autres types d'assujettissements sont forcement vaudois(Diplomate, Indigent, dépense, Ordianaire)
		return true;
	}

	private boolean absenceForPrincipalVaudois(List<ForFiscal> forValides) {
		return forValides.stream()
				.filter(ForFiscal::isPrincipal)
				.noneMatch(f->f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);

	}

	private boolean absenceDomicileVaudois(Contribuable ctb, int anneeFiscale) throws DonneesCivilesException {
		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			return absencePeriodeDeResidence(pp, anneeFiscale);
		}
		if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(mc, anneeFiscale);
			if (ensembleTiersCouple == null) {
				return true;
			}
			final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();

			return absencePeriodeDeResidence(principal, anneeFiscale) && absencePeriodeDeResidence(conjoint, anneeFiscale);
		}

		return true;
	}

	private boolean absenceRapportTravail(Contribuable ctb, int anneeFiscale) {
		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			return absencePrestationImposable(pp, anneeFiscale);
		}
		if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(mc, anneeFiscale);
			if (ensembleTiersCouple == null) {
				return true;
			}
			final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();

			return absencePrestationImposable(principal, anneeFiscale) && absencePrestationImposable(conjoint, anneeFiscale);
		}

		return true;
	}


	private boolean absencePrestationImposable(PersonnePhysique pp, int pf) {
		if (pp == null) {
			return true;
		}
		final RegDate debut = RegDate.get(pf, 1, 1);
		final RegDate fin = RegDate.get(pf, 12, 31);
		final DateRange rangePeriode = new DateRangeHelper.Range(debut, fin);
		final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
		return rapports == null || rapports.stream()
				.filter(r->!r.isAnnule())
				.filter(r -> r instanceof RapportPrestationImposable)
				.noneMatch(prestation -> DateRangeHelper.intersect(prestation, rangePeriode));

	}

	private boolean absencePeriodeDeResidence(PersonnePhysique pp, int pf) throws DonneesCivilesException {
		if (pp == null) {
			return true;
		}
		final RegDate debut = RegDate.get(pf, 1, 1);
		final RegDate fin = RegDate.get(pf, 12, 31);
		final DateRange rangePeriode = new DateRangeHelper.Range(debut, fin);
		final List<DateRange> periodesDeResidence = tiersService.getPeriodesDeResidence(pp, false);
		return !DateRangeHelper.intersect(rangePeriode, periodesDeResidence);
	}

	@Override
	public void addAll(ListeEchangeRenseignementsResults sources) {
		super.addAll(sources);
		nbCtbPpIdentifies += sources.nbCtbPpIdentifies;
		nbCtbPmIdentifies += sources.nbCtbPmIdentifies;
		ppIdentifies.addAll(sources.ppIdentifies);
		ppIgnores.addAll(sources.ppIgnores);
		pmIdentifies.addAll(sources.pmIdentifies);
		pmIgnores.addAll(sources.pmIgnores);
	}

	@Override
	public void sort() {
		super.sort();
		Collections.sort(ppIdentifies);
		Collections.sort(ppIgnores);
		Collections.sort(pmIdentifies);
		Collections.sort(pmIgnores);
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// ne fait rien en particulier
	}


	private boolean isAssujettissementSourcier(Assujettissement a) {
		return a.getType() == TypeAssujettissement.SOURCE_PURE ||
				a.getType() == TypeAssujettissement.MIXTE_137_1 ||
				a.getType() == TypeAssujettissement.MIXTE_137_2;
	}

	public int getAnneeFiscale() {
		return anneeFiscale;
	}


	public int getNbCtbPpIdentifies() {
		return nbCtbPpIdentifies;
	}


	public int getNbCtbPpIgnores() {
		return ppIgnores.size();
	}

	public int getNbCtbPmIdentifies() {
		return nbCtbPmIdentifies;
	}


	public int getNbCtbPmIgnores() {
		return pmIgnores.size();
	}


	public int getNbContribuablesInspectes() {
		return nbCtbPpIdentifies + nbCtbPmIdentifies + getNbCtbPpIgnores() + getNbCtbPmIgnores() + getListeErreurs().size();
	}


	public List<InfoIdentifiantCtb> getPpIdentifies() {
		return ppIdentifies;
	}

	public List<InfoIdentifiantCtb> getPmIdentifies() {
		return pmIdentifies;
	}

	public List<InfoCtbIgnore> getPmIgnores() {
		return pmIgnores;
	}

	public List<InfoCtbIgnore> getPpIgnores() {
		return ppIgnores;
	}

	public boolean isAvecContribuablesPP() {
		return avecContribuablesPP;
	}

	public boolean isAvecContribuablesPM() {
		return avecContribuablesPM;
	}
}
