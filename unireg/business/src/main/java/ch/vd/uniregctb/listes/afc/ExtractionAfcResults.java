package ch.vd.uniregctb.listes.afc;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class ExtractionAfcResults extends ListesResults<ExtractionAfcResults> {

	public final int periodeFiscale;
	public final TypeExtractionAfc mode;

	private final ServiceInfrastructureService infraService;

	public static final class InfoComparator<T extends InfoCtbBase> implements Comparator<T> {
		public int compare(T o1, T o2) {
			return (int) (o1.noCtb - o2.noCtb);
		}
	}

	private static final String NON_ASSUJETTI_OU_SOURCIER_PUR = "Non-assujetti ou sourcier pur";
	private static final String HORS_CANTON = "Hors canton";
	private static final String ASSUJETTI_SANS_FOR_VD_AU_31_12 = "Assujetti sans for vaudois au 31 décembre";

	public static abstract class InfoCtbBase {
		public final long noCtb;

		public InfoCtbBase(long noCtb) {
			this.noCtb = noCtb;
		}
	}

	public static class InfoCtbIgnore extends InfoCtbBase {
		public final String raisonIgnore;

		public InfoCtbIgnore(long noCtb, String raisonIgnore) {
			super(noCtb);
			this.raisonIgnore = raisonIgnore;
		}
	}

	public static class InfoCtbListe extends InfoCtbBase {
		public final String nomPrenom;
		public final Integer ofsCommuneForGestion;
		public final boolean assujettissementIllimite;

		public InfoCtbListe(long noCtb, String nomPrenom, Integer ofsCommuneForGestion, boolean assujettissementIllimite) {
			super(noCtb);
			this.nomPrenom = nomPrenom;
			this.ofsCommuneForGestion = ofsCommuneForGestion;
			this.assujettissementIllimite = assujettissementIllimite;
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

	public ExtractionAfcResults(RegDate dateTraitement, int periodeFiscale, TypeExtractionAfc mode, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService) {
		super(dateTraitement, nbThreads, tiersService);
		this.periodeFiscale = periodeFiscale;
		this.mode = mode;
		this.infraService = infraService;
	}

	private final List<InfoCtbListe> listePrincipale = new LinkedList<InfoCtbListe>();
	private final List<InfoCtbListe> listeSecondaire = new LinkedList<InfoCtbListe>();
	private final List<InfoCtbIgnore> listeCtbsIgnores = new LinkedList<InfoCtbIgnore>();

	private InfoCtbListe buildInfoCtbListe(Contribuable ctb, boolean assujettissementIllimite) throws ServiceInfrastructureException, CoupleInvalideException {

		final RegDate finAnnee = RegDate.get(periodeFiscale, 12, 31);
		final ForFiscalPrincipal ffp = ctb.getDernierForFiscalPrincipalAvant(finAnnee);
		if (ffp == null) {
			throw new RuntimeException("Comment ce contribuable " + ctb.getNumero() + " peut-il être avoir été inspecté pour " + periodeFiscale + " alors qu'il n'a pas de for principal ?");
		}
		final RegDate dateReference = ffp.getDateFin() != null && ffp.getDateFin().isBefore(finAnnee) ? ffp.getDateFin() : finAnnee;

		final String nomPrenom;
		if (ctb instanceof PersonnePhysique) {
			nomPrenom = tiersService.getNomPrenom((PersonnePhysique) ctb);
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;
			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateReference);
			if (couple == null || couple.getPrincipal() == null) {
				throw new CoupleInvalideException(mc);
			}

			final PersonnePhysique principal = couple.getPrincipal();
			nomPrenom = tiersService.getNomPrenom(principal);
		}
		else {
			throw new IllegalArgumentException("Type de tiers non-supporté : " + ctb.getClass().getName() + " (" + ctb.getNumero() + ")");
		}

		final ForGestion forGestion = tiersService.getDernierForGestionConnu(ctb, dateReference);
		final Integer ofsCommuneForGestion;
		if (forGestion == null) {
			ofsCommuneForGestion = null;
		}
		else {
			final Commune commune = infraService.getCommuneByNumeroOfsEtendu(forGestion.getNoOfsCommune(), dateReference);
			if (commune.isFraction()) {
				final Commune communeFaitiere = infraService.getCommuneFaitiere(commune, dateReference);
				ofsCommuneForGestion = communeFaitiere.getNoOFSEtendu();
			}
			else {
				ofsCommuneForGestion = commune.getNoOFSEtendu();
			}
		}

		return new InfoCtbListe(ctb.getNumero(), nomPrenom, ofsCommuneForGestion, assujettissementIllimite);
	}

	@Override
	public void addContribuable(Contribuable ctb) throws ServiceInfrastructureException {
		addContribuable(ctb, true, true);
	}

	public void addContribuableNonAssujettiOuSourcierPur(Contribuable ctb) {
		addContribuableIgnore(ctb, NON_ASSUJETTI_OU_SOURCIER_PUR);
	}

	public void addContribuableAssujettiMaisSansForVaudoisEnFinDePeriode(Contribuable ctb) {
		addContribuableIgnore(ctb, ASSUJETTI_SANS_FOR_VD_AU_31_12);
	}

	private void addContribuable(Contribuable ctb, boolean dansListePrincipale, boolean assujettissementIllimite) throws ServiceInfrastructureException {
		try {
			final InfoCtbListe info = buildInfoCtbListe(ctb, assujettissementIllimite);
			if (dansListePrincipale) {
				listePrincipale.add(info);
			}
			else {
				listeSecondaire.add(info);
			}
		}
		catch (CoupleInvalideException e) {
			addErrorManqueLiensMenage(e.getMenageCommun());
		}
	}

	private void addContribuableIgnore(Contribuable ctb, String raison) {
		listeCtbsIgnores.add(new InfoCtbIgnore(ctb.getNumero(), raison));
	}

	public void addContribuableLimiteHS(Contribuable ctb) throws ServiceInfrastructureException {
		Assert.isTrue(mode == TypeExtractionAfc.REVENU || mode == TypeExtractionAfc.FORTUNE);
		addContribuable(ctb, mode == TypeExtractionAfc.REVENU, false);
	}

	public void addContribuableLimiteHC(Contribuable ctb) throws ServiceInfrastructureException {
		if (mode == TypeExtractionAfc.REVENU) {
			addContribuableIgnore(ctb, HORS_CANTON);
		}
		else if (mode == TypeExtractionAfc.FORTUNE) {
			addContribuable(ctb, false, false);
		}
		else {
			throw new IllegalArgumentException("Mode non-supporté : " + mode);
		}
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// rien à faire, le tiers sera dans la liste des erreurs, c'est tout
	}

	@Override
	public void addAll(ExtractionAfcResults sources) {
		super.addAll(sources);
		listeCtbsIgnores.addAll(sources.listeCtbsIgnores);
		listePrincipale.addAll(sources.listePrincipale);
		listeSecondaire.addAll(sources.listeSecondaire);
	}

	@Override
	public void sort() {
		super.sort();
		final Comparator<InfoCtbBase> comparator = new InfoComparator<InfoCtbBase>();
		Collections.sort(listeCtbsIgnores, comparator);
		Collections.sort(listePrincipale, comparator);
		Collections.sort(listeSecondaire, comparator);
	}

	public int getNombreCtbAnalyses() {
		return listeCtbsIgnores.size() + listePrincipale.size() + listeSecondaire.size() + getListeErreurs().size();
	}

	public List<InfoCtbListe> getListePrincipale() {
		return listePrincipale;
	}

	public List<InfoCtbListe> getListeSecondaire() {
		return listeSecondaire;
	}

	public List<InfoCtbIgnore> getListeCtbsIgnores() {
		return listeCtbsIgnores;
	}
}
