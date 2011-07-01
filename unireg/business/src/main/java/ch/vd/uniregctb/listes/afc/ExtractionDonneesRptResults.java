package ch.vd.uniregctb.listes.afc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.declaration.ordinaire.ForsList;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.SourcierPur;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForGestion;
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

	private static final String NON_ASSUJETTI = "Non assujetti sur la période fiscale";

	private final ServiceInfrastructureService infraService;

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

	public static class InfoPeriodeImposition extends InfoCtbBase<InfoPeriodeImposition> {
		public final String nom;
		public final String prenom;
		public final String numeroAvs;
		public final RegDate dateNaissance;
		public final Long noCtbPrincipal;
		public final Long noCtbConjoint;
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

		public InfoPeriodeImposition(long noCtb, String nom, String prenom, String numeroAvs, RegDate dateNaissance, Long noCtbPrincipal, Long noCtbConjoint, ModeImposition modeImposition,
		                             RegDate debutPeriodeImposition, RegDate finPeriodeImposition, MotifRattachement motifRattachement, MotifFor motifOuverture, MotifFor motifFermeture,
		                             Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
			super(noCtb);
			this.nom = nom;
			this.prenom = prenom;
			this.numeroAvs = numeroAvs;
			this.dateNaissance = dateNaissance;
			this.noCtbPrincipal = noCtbPrincipal;
			this.noCtbConjoint = noCtbConjoint;
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
			return new Object[] { noCtb, nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint, modeImposition,
					debutPeriodeImposition, finPeriodeImposition, motifRattachement, motifOuverture, motifFermeture, ofsCommuneForGestion, autoriteFiscaleForPrincipal };
		}
	}

	private static class CoupleInvalideException extends Exception {

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

	private static class ContribuableIgnoreException extends Exception {
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
	private List<InfoPeriodeImposition> buildInfoPeriodeAssujettissement(Contribuable ctb) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException {

		final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, periodeFiscale);
		if (assujettissements == null || assujettissements.size() == 0) {
			throw new ContribuableIgnoreException(NON_ASSUJETTI);
		}

		final String raisonExclusion = filterAssujettissements(ctb, assujettissements);
		if (assujettissements.size() == 0) {
			if (StringUtils.isBlank(raisonExclusion)) {
				throw new RuntimeException("Tous les assujettissements de la période fiscale " + periodeFiscale + " ont été filtrés sans explication");
			}
			throw new ContribuableIgnoreException(raisonExclusion);
		}

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

			final Assujettissement dernier = assujettissements.get(assujettissements.size() - 1);
			final ForFiscalPrincipal dernierFfp = dernier.getFors().principauxDansLaPeriode.last();
			if (dernierFfp == null) {
				throw new RuntimeException("Comment ce contribuable " + ctb.getNumero() + " peut-il être avoir un assujettissement sur " + periodeFiscale + " alors qu'il n'a pas de for principal ?");
			}
			final RegDate dateReferenceMenage = RegDateHelper.minimum(dernierFfp.getDateFin(), dernier.getDateFin(), NullDateBehavior.LATEST);

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateReferenceMenage);
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
			throw new IllegalArgumentException("Type de tiers non-supporté : " + ctb.getClass().getName() + " (" + ctb.getNumero() + ")");
		}

		// on boucle ensuite sur les périodes d'assujettissement pour faire une ligne par période
		final List<InfoPeriodeImposition> liste = new ArrayList<InfoPeriodeImposition>(assujettissements.size());
		for (Assujettissement a : assujettissements) {

			final TypeAutoriteFiscale autoriteFiscaleForPrincipal;
			final ModeImposition modeImposition;
			final MotifRattachement motifRattachement;
			final Integer ofsCommuneForGestion;
			if (a instanceof SourcierPur) {
				final ForFiscalPrincipal ffp = extraireDernierForSource(a.getFors().principauxDansLaPeriode);
				modeImposition = ModeImposition.SOURCE;
				if (ffp == null) {
					// c'est toujours mieux que rien... cas du for SOURCE "inventé" par un for principal avec motif d'ouverture "CHGT_MODE_IMPOSITION"
					final ForFiscalPrincipal ffpNonSource = a.getFors().principal;
					motifRattachement = ffpNonSource.getMotifRattachement();
					autoriteFiscaleForPrincipal = a.getFors().principal.getTypeAutoriteFiscale();
					if (autoriteFiscaleForPrincipal == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						ofsCommuneForGestion = ffpNonSource.getNumeroOfsAutoriteFiscale();
					}
					else {
						ofsCommuneForGestion = null;
					}
				}
				else {
					motifRattachement = ffp.getMotifRattachement();     // on n'a pas encore les fors secondaires sources...
					autoriteFiscaleForPrincipal = ffp.getTypeAutoriteFiscale();
					if (autoriteFiscaleForPrincipal == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						ofsCommuneForGestion = ffp.getNumeroOfsAutoriteFiscale();
					}
					else {
						ofsCommuneForGestion = null;
					}
				}
			}
			else {
				final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, a.getDateFin());
				if (forGestion == null) {
					throw new RuntimeException("Assujettissement " + a + " non sourcier-pur sans for de gestion en fin d'assujettissement ?");
				}

				final Commune commune = infraService.getCommuneByNumeroOfsEtendu(forGestion.getNoOfsCommune(), forGestion.getDateDebut());
				if (commune.isFraction()) {
					final Commune communeFaitiere = infraService.getCommuneFaitiere(commune, forGestion.getDateDebut());
					ofsCommuneForGestion = communeFaitiere.getNoOFSEtendu();
				}
				else {
					ofsCommuneForGestion = commune.getNoOFSEtendu();
				}

				final ForFiscalRevenuFortune forRevenuFortune = forGestion.getSousjacent();
				motifRattachement = forRevenuFortune.getMotifRattachement();
				if (forRevenuFortune instanceof ForFiscalPrincipal) {
					autoriteFiscaleForPrincipal = TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
					modeImposition = ((ForFiscalPrincipal) forRevenuFortune).getModeImposition();
				}
				else {
					// les rattachements économiques sont au mode d'imposition ordinaire, enfin je crois...
					modeImposition = ModeImposition.ORDINAIRE;
					if (!(a instanceof HorsSuisse || a instanceof HorsCanton)) {
						throw new RuntimeException("Rattachement économique avec assujettissement différent de HS ou HC : " + a);
					}
					autoriteFiscaleForPrincipal = (a instanceof HorsSuisse ? TypeAutoriteFiscale.PAYS_HS : TypeAutoriteFiscale.COMMUNE_HC);
				}
			}

			final InfoPeriodeImposition info = buildInfoPeriodeImposition(ctb, nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint, modeImposition,
			                                                              motifRattachement, a, ofsCommuneForGestion, autoriteFiscaleForPrincipal);
			if (info != null) {
				liste.add(info);
			}
		}

		return liste;
	}

	/**
	 * On se sert de cette méthode pour récupérer le dernier for SOURCE dans la collection des fors principaux actifs dans une période d'assujettissement
	 * (cette collection est extraite de la décomposition des fors attachée à l'assujettissement lui-même)... La raison en est l'arrondi qui est fait dans
	 * l'assujettissement entre la date de fin de for source et la date de fin d'assujettissement source (fin du mois) dans le cas d'un changement de mode
	 * d'imposition
	 * @param principauxDansLaPeriode les fors principaux triés valides dans une période
	 * @return le dernier for principal de la liste fournie avec un mode d'imposition SOURCE, ou <code>null</code> s'il n'y en a pas...
	 */
	private static ForFiscalPrincipal extraireDernierForSource(ForsList<ForFiscalPrincipal> principauxDansLaPeriode) {
		final ListIterator<ForFiscalPrincipal> iterator = principauxDansLaPeriode.listIterator(principauxDansLaPeriode.size());
		while (iterator.hasPrevious()) {
			final ForFiscalPrincipal ffp = iterator.previous();
			if (ffp.getModeImposition() == ModeImposition.SOURCE) {
				return ffp;
			}
		}
		return null;
	}

	protected InfoPeriodeImposition buildInfoPeriodeImposition(Contribuable ctb, String nom, String prenom, String numeroAvs, RegDate dateNaissance, Long noCtbPrincipal,
	                                                           Long noCtbConjoint, ModeImposition modeImposition, MotifRattachement motifRattachement, Assujettissement a,
	                                                           Integer ofsCommuneForGestion, TypeAutoriteFiscale autoriteFiscaleForPrincipal) {
		return new InfoPeriodeImposition(ctb.getNumero(), nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint, modeImposition, a.getDateDebut(), a.getDateFin(),
		                                 motifRattachement, a.getMotifFractDebut(), a.getMotifFractFin(), ofsCommuneForGestion, autoriteFiscaleForPrincipal);
	}

	/**
	 * Implémentée par les classes dérivées pour oter les assujettissements qui ne doivent pas être pris en compte
	 * @param ctb contribuable assujetti
	 * @param listeAFiltrer liste à modifier en cas de filtrage effectif (en entrée, la liste n'est jamais vide)
	 * @return si la liste à filtrer ne contient plus d'éléments en sortie de la méthode, alors la valeur retournée doit être une description de la raison pour laquelle tous les assujettissements ont été filtrés (sinon, la valeur retournée sera ignorée de toute façon)
	 */
	protected abstract String filterAssujettissements(Contribuable ctb, List<Assujettissement> listeAFiltrer);

	@Override
	public final void addContribuable(Contribuable ctb) throws ServiceInfrastructureException {
		++ nbContribuablesAnalyses;
		try {
			final List<InfoPeriodeImposition> periodes = buildInfoPeriodeAssujettissement(ctb);
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
