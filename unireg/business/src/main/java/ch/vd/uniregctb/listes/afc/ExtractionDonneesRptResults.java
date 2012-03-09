package ch.vd.uniregctb.listes.afc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class ExtractionDonneesRptResults extends ListesResults<ExtractionDonneesRptResults> {

	public final int periodeFiscale;

	public static final String NON_ASSUJETTI = "Non assujetti sur la période fiscale";

	protected final ServiceInfrastructureService infraService;

	public static abstract class InfoCtbBase<T extends InfoCtbBase> implements Comparable<T> {
		public final long noCtb;

		public InfoCtbBase(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public int compareTo(T o) {
			return (int) (noCtb - o.noCtb);
		}

		public abstract String[] getNomsColonnes();
		public abstract Object[] getValeursColonnes();
	}

	public static class InfoCtbIgnore extends InfoCtbBase {
		public final String raisonIgnore;

		private static final String[] NOMS_COLONNES = { "NO_CTB", "RAISON" };

		public InfoCtbIgnore(long noCtb, String raisonIgnore) {
			super(noCtb);
			this.raisonIgnore = raisonIgnore;
		}

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		@Override
		public Object[] getValeursColonnes() {
			return new Object[] { noCtb, raisonIgnore };
		}
	}

	public static class InfoIdentificationCtb {
		public final String nom;
		public final String prenom;
		public final String numeroAvs;
		public final RegDate dateNaissance;
		public final Long noCtbPrincipal;
		public final Long noCtbConjoint;

		public InfoIdentificationCtb(String nom, String prenom, String numeroAvs, RegDate dateNaissance, Long noCtbPrincipal, Long noCtbConjoint) {
			this.nom = nom;
			this.prenom = prenom;
			this.numeroAvs = numeroAvs;
			this.dateNaissance = dateNaissance;
			this.noCtbPrincipal = noCtbPrincipal;
			this.noCtbConjoint = noCtbConjoint;
		}
	}

	public static class InfoPeriodeImposition extends InfoCtbBase<InfoPeriodeImposition> {
		public final InfoIdentificationCtb identification;
		public final ModeImposition modeImposition;
		public final RegDate debutPeriodeImposition;
		public final RegDate finPeriodeImposition;
		public final MotifRattachement motifRattachement;
		public final MotifFor motifOuverture;
		public final MotifFor motifFermeture;
		public final Integer ofsCommuneForGestion;
		public final TypeAutoriteFiscale autoriteFiscaleForPrincipal;

		protected static final String[] NOMS_COLONNES = { "NO_CTB", "NOM", "PRENOM", "AVS", "DATE_NAISSANCE", "NO_CTB_PRINCIPAL", "NO_CTB_CONJOINT", "MODE_IMPOSITION",
														"DEBUT_PERIODE_IMPOSITION", "FIN_PERIODE_IMPOSITION", "MOTIF_RATTACHEMENT", "MOTIF_OUVERTURE", "MOTIF_FERMETURE",
														"OFS_COMMUNE_GESTION", "AUTORITE_FISC_FOR_PRN" };

		public InfoPeriodeImposition(long noCtb, InfoIdentificationCtb identification, ModeImposition modeImposition,
		                             RegDate debutPeriodeImposition, RegDate finPeriodeImposition, MotifRattachement motifRattachement, MotifFor motifOuverture, MotifFor motifFermeture,
		                             Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
			super(noCtb);
			this.identification = identification;
			this.modeImposition = modeImposition;
			this.debutPeriodeImposition = debutPeriodeImposition;
			this.finPeriodeImposition = finPeriodeImposition;
			this.motifRattachement = motifRattachement;
			this.motifOuverture = motifOuverture;
			this.motifFermeture = motifFermeture;
			this.ofsCommuneForGestion = ofsCommuneForGestion;
			this.autoriteFiscaleForPrincipal = autoriteFiscaleForPrincipal;
		}

		@Override
		public int compareTo(InfoPeriodeImposition o) {
			int comparison = super.compareTo(o);
			if (comparison == 0) {
				// on compare ensuite la date de début de la période d'assujettissement
				comparison = NullDateBehavior.EARLIEST.compare(debutPeriodeImposition, o.debutPeriodeImposition);
			}
			return comparison;
		}

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		@Override
		public Object[] getValeursColonnes() {
			return new Object[] { noCtb, identification.nom, identification.prenom, identification.numeroAvs, identification.dateNaissance, identification.noCtbPrincipal, identification.noCtbConjoint,
					modeImposition, debutPeriodeImposition, finPeriodeImposition, motifRattachement, motifOuverture, motifFermeture, ofsCommuneForGestion, autoriteFiscaleForPrincipal };
		}
	}

	protected static class CoupleInvalideException extends Exception {

		private final MenageCommun menageCommun;

		public CoupleInvalideException(MenageCommun menageCommun) {
			this.menageCommun = menageCommun;
		}

		public MenageCommun getMenageCommun() {
			return menageCommun;
		}
	}

	public ExtractionDonneesRptResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService) {
		super(dateTraitement, nbThreads, tiersService);
		this.periodeFiscale = periodeFiscale;
		this.infraService = infraService;
	}

	public abstract TypeExtractionDonneesRpt getMode();

	private int nbContribuablesAnalyses = 0;
	private final List<InfoPeriodeImposition> listePeriode = new LinkedList<InfoPeriodeImposition>();
	private final List<InfoCtbIgnore> listeCtbsIgnores = new LinkedList<InfoCtbIgnore>();

	protected static class ContribuableIgnoreException extends Exception {
		public ContribuableIgnoreException(String message) {
			super(message);
		}
	}

	/**
	 * Calcul de la liste des périodes à faire apparaître dans l'extraction pour un contribuable donné
	 * @param ctb le contribuable à analyser
	 * @return périodes à faire apparaître dans l'extraction
	 * @throws ServiceInfrastructureException problème dans l'infrastructure fiscale
	 * @throws CoupleInvalideException contribuable ménage commun sans lien vers des personnes physiques
	 * @throws AssujettissementException problème dans le calcul de l'assujettissement du contribuable
	 * @throws ContribuableIgnoreException si le contribuable est ignoré (le message inclus dans l'exception en explique la raison)
	 */
	private List<InfoPeriodeImposition> buildInfoPeriodes(Contribuable ctb) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException {
		final DecompositionForsAnneeComplete decomposition = new DecompositionForsAnneeComplete(ctb, periodeFiscale);
		return buildInfoPeriodes(decomposition);
	}

	/**
	 * Calcul de la liste des périodes à faire apparaître dans l'extraction pour un contribuable donné
	 * @param decomposition décomposition des fors pour le contribuable inspecté sur l'année de la période fiscale
	 * @return périodes à faire apparaître dans l'extraction
	 * @throws ServiceInfrastructureException problème dans l'infrastructure fiscale
	 * @throws CoupleInvalideException contribuable ménage commun sans lien vers des personnes physiques
	 * @throws AssujettissementException problème dans le calcul de l'assujettissement du contribuable
	 * @throws ContribuableIgnoreException si le contribuable est ignoré (le message inclus dans l'exception en explique la raison)
	 */
	protected abstract List<InfoPeriodeImposition> buildInfoPeriodes(DecompositionForsAnneeComplete decomposition) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException;

	/**
	 * Construit un objet {@link InfoIdentificationCtb} pour le contribuable donné à la date donnée
	 * @param ctb contribuable
	 * @param dateReference date de référence (pour les rapports d'appartenance ménage)
	 * @return une nouvelle instance de InfoIdentificationCtb correspondant au contribuable
	 * @throws CoupleInvalideException si le ménage commun n'a pas de contribuable principal à la date donnée
	 */
	protected InfoIdentificationCtb buildInfoIdentification(Contribuable ctb, RegDate dateReference) throws CoupleInvalideException {

		final String nom;
		final String prenom;
		final RegDate dateNaissance;
		final String numeroAvs;
		final Long noCtbPrincipal;
		final Long noCtbConjoint;
		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp);
			if (nomPrenom != null) {
				nom = nomPrenom.getNom();
				prenom = nomPrenom.getPrenom();
			}
			else {
				nom = null;
				prenom = null;
			}
			numeroAvs = tiersService.getNumeroAssureSocial(pp);
			dateNaissance = tiersService.getDateNaissance(pp);
			noCtbPrincipal = null;
			noCtbConjoint = null;
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateReference);
			if (couple == null || couple.getPrincipal() == null) {
				throw new CoupleInvalideException(mc);
			}

			final PersonnePhysique principal = couple.getPrincipal();
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(principal);
			if (nomPrenom != null) {
				nom = nomPrenom.getNom();
				prenom = nomPrenom.getPrenom();
			}
			else {
				nom = null;
				prenom = null;
			}
			numeroAvs = tiersService.getNumeroAssureSocial(principal);
			dateNaissance = tiersService.getDateNaissance(principal);
			noCtbPrincipal = principal.getNumero();

			final PersonnePhysique conjoint = couple.getConjoint(principal);
			noCtbConjoint = conjoint != null ? conjoint.getNumero() : null;
		}
		else {
			throw new IllegalArgumentException("Type de tiers non-supporté : " + ctb.getClass().getName() + " (" + ctb.getNumero() + ')');
		}

		return new InfoIdentificationCtb(nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint);
	}

	protected final InfoPeriodeImposition buildInfoPeriodeImposition(Contribuable ctb, InfoIdentificationCtb identification, ModeImposition modeImposition, MotifRattachement motifRattachement, DateRange range,
	                                                                 MotifFor motifDebut, MotifFor motifFin, Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
		return new InfoPeriodeImposition(ctb.getNumero(), identification, modeImposition, range.getDateDebut(), range.getDateFin(),
		                                 motifRattachement, motifDebut, motifFin, ofsCommuneForGestion, autoriteFiscaleForPrincipal);
	}

	@Override
	public final void addContribuable(Contribuable ctb) throws ServiceInfrastructureException {
		++ nbContribuablesAnalyses;
		try {
			final List<InfoPeriodeImposition> periodes = buildInfoPeriodes(ctb);
			Assert.notEmpty(periodes);
			for (InfoPeriodeImposition periode : periodes) {
				addPeriodeImposition(periode);
			}
		}
		catch (AssujettissementException e) {
			addErrorException(ctb, e);
		}
		catch (CoupleInvalideException e) {
			addErrorManqueLiensMenage(e.getMenageCommun());
		}
		catch (ContribuableIgnoreException e) {
			addContribuableIgnore(ctb, e.getMessage());
		}
	}

	protected void addContribuableIgnore(Contribuable ctb, String raison) {
		listeCtbsIgnores.add(new InfoCtbIgnore(ctb.getNumero(), raison));
	}

	protected void addPeriodeImposition(InfoPeriodeImposition periode) {
		listePeriode.add(periode);
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// rien à faire, le tiers sera dans la liste des erreurs, c'est tout
	}

	@Override
	public void addAll(ExtractionDonneesRptResults sources) {
		super.addAll(sources);
		listeCtbsIgnores.addAll(sources.listeCtbsIgnores);
		listePeriode.addAll(sources.listePeriode);
		nbContribuablesAnalyses += sources.nbContribuablesAnalyses;
	}

	@Override
	public void sort() {
		super.sort();
		Collections.sort(listePeriode);
		Collections.sort(listeCtbsIgnores);
	}

	public int getNombreCtbAnalyses() {
		return nbContribuablesAnalyses;
	}

	public List<InfoPeriodeImposition> getListePeriode() {
		return listePeriode;
	}

	public List<InfoCtbIgnore> getListeCtbsIgnores() {
		return listeCtbsIgnores;
	}

	public Map<ModeImposition, Integer> getDecoupageEnModeImposition() {
		final int[] array = new int[ModeImposition.values().length + 1];
		Arrays.fill(array, 0);
		final int zeroPosition = ModeImposition.values().length;
		for (InfoPeriodeImposition periode : listePeriode) {
			final ModeImposition modeImposition = periode.modeImposition;
			if (modeImposition == null) {
				++ array[zeroPosition];
			}
			else {
				++ array[modeImposition.ordinal()];
			}
		}

		// on constuit maintenant la map à renvoyer
		final Map<ModeImposition, Integer> map = new HashMap<ModeImposition, Integer>(ModeImposition.values().length + 1);
		for (ModeImposition modeImposition : ModeImposition.values()) {
			final int index = modeImposition.ordinal();
			if (array[index] > 0) {
				map.put(modeImposition, array[index]);
			}
		}
		if (array[zeroPosition] > 0) {
			map.put(null, array[zeroPosition]);
		}
		return map;
	}
}
