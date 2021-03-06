package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;

public class ListeNoteResults extends JobResults<Long, ListeNoteResults> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListeNoteResults.class);

	public ListeNoteResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public boolean isInterrompu() {
		return interrompu;
	}


	public static class InfoContribuable {
		public final long noCtb;

		public InfoContribuable(long noCtb) {

			this.noCtb = noCtb;
		}
	}

	public static class InfoContribuableAvecNote extends InfoContribuable {


		private final List<NomPrenom> nomsPrenoms;
		private final List<String> nosAvs;
		private final String[] adresseEnvoi;
		private int oidGestion;
		private Commune communeHC;
		private String nomCantonHC;
		private Commune communeVaudoise;
		private Commune communeFinPeriode;
		private String nomCantonFinPeriode;
		private Pays paysFinPeriode;
		private MotifFor motifOuvertureForPrincipal;
		private RegDate dateFermetureSecondaire;
		private MotifFor motifFinSecondaire;

		public InfoContribuableAvecNote(Contribuable ctb, RegDate date, AdresseService adresseService, TiersService tiersService, ServiceInfrastructureService infraService) {

			super(ctb.getNumero());

			AdresseEnvoiDetaillee adresseEnvoi;
			try {
				adresseEnvoi = adresseService.getAdresseEnvoi(ctb, RegDate.get(date.year(), 12, 31), TypeAdresseFiscale.COURRIER, false);
			}
			catch (AdresseException e) {
				LOGGER.warn("Résolution de l'adresse du contribuable " + ctb.getNumero() + " impossible", e);
				adresseEnvoi = null;
			}

			this.adresseEnvoi = Optional.ofNullable(adresseEnvoi)
					.map(AdresseEnvoiDetaillee::getLignes)
					.orElse(null);

			this.nomsPrenoms = new ArrayList<>(2);
			this.nosAvs = new ArrayList<>(2);
			fillNomsPrenomsEtNosAvs(ctb, date.year(), tiersService, nomsPrenoms, nosAvs);
			fillSituationFiscale(ctb, date, tiersService, infraService);
		}

		private void fillSituationFiscale(Contribuable ctb, RegDate date, TiersService tiersService, ServiceInfrastructureService infraService) {
			ForGestion forGestion = tiersService.getForGestionActif(ctb, date);
			if (forGestion != null) {
				try {
					final int ofsCommune = forGestion.getNoOfsCommune();
					this.communeVaudoise = infraService.getCommuneByNumeroOfs(ofsCommune, date);

					OfficeImpot office = infraService.getOfficeImpotDeCommune(ofsCommune);
					if (office != null) {
						this.oidGestion = office.getNoColAdm();

					}
				}
				catch (InfrastructureException e) {
					LOGGER.warn("Résolution de l'oid de gestion et de la commune  du contribuable " + ctb.getNumero() + " impossible", e);
				}

				this.dateFermetureSecondaire = forGestion.getSousjacent().getDateFin();
				this.motifFinSecondaire = forGestion.getSousjacent().getMotifFermeture();
			}

			ForFiscalPrincipal forPrincipal = ctb.getForFiscalPrincipalAt(date);
			if (forPrincipal != null) {
				final Integer autoriteFiscale = forPrincipal.getNumeroOfsAutoriteFiscale();
				try {
					this.communeHC = infraService.getCommuneByNumeroOfs(autoriteFiscale, date);
					this.nomCantonHC = infraService.getCantonBySigle(communeHC.getSigleCanton()).getNomOfficiel();
				}
				catch (InfrastructureException e) {
					LOGGER.warn("Résolution de la commune HC du contribuable " + ctb.getNumero() + " impossible", e);
				}
			}

			final int periode = date.year();
			final RegDate dateFinPeriode = RegDate.get(periode, 12, 31);
			ForFiscalPrincipal forPrincipalFinPeriode = ctb.getForFiscalPrincipalAt(dateFinPeriode);
			if (forPrincipalFinPeriode == null) {
				forPrincipalFinPeriode = ctb.getDernierForFiscalPrincipal();
			}
			if (forPrincipalFinPeriode != null) {
				try {
					final int ofsCommune = forPrincipalFinPeriode.getNumeroOfsAutoriteFiscale();
					this.communeFinPeriode = infraService.getCommuneByNumeroOfs(ofsCommune, date);

					if (communeFinPeriode == null) {
						this.paysFinPeriode = infraService.getPays(ofsCommune, date);

					}
					else{
						this.nomCantonFinPeriode = infraService.getCantonBySigle(communeFinPeriode.getSigleCanton()).getNomOfficiel();
					}

				}
				catch (InfrastructureException e) {
					LOGGER.warn("Résolution de de la commune en fin e période du contribuable " + ctb.getNumero() + " impossible", e);
				}

				this.motifOuvertureForPrincipal = forPrincipalFinPeriode.getMotifOuverture();

			}


		}

		private static void fillNomsPrenomsEtNosAvs(Contribuable ctb, int annee, TiersService tiersService, List<NomPrenom> nomsPrenoms, List<String> nosAvs) {
			if (ctb instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) ctb;
				final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
				final String noAvs = tiersService.getNumeroAssureSocial(pp);
				nomsPrenoms.add(nomPrenom);
				nosAvs.add(noAvs);
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, annee);
				final PersonnePhysique principal = ensemble.getPrincipal();
				final PersonnePhysique conjoint = ensemble.getConjoint();
				if (principal != null) {
					nomsPrenoms.add(tiersService.getDecompositionNomPrenom(principal, false));
					nosAvs.add(tiersService.getNumeroAssureSocial(principal));
				}
				if (conjoint != null) {
					nomsPrenoms.add(tiersService.getDecompositionNomPrenom(conjoint, false));
					nosAvs.add(tiersService.getNumeroAssureSocial(conjoint));
				}
			}
		}


		public List<NomPrenom> getNomsPrenoms() {
			return nomsPrenoms;
		}

		public List<String> getNosAvs() {
			return nosAvs;
		}

		public String[] getAdresseEnvoi() {
			return adresseEnvoi;
		}

		public int getOidGestion() {
			return oidGestion;
		}

		public Commune getCommuneHC() {
			return communeHC;
		}

		public Commune getCommuneVaudoise() {
			return communeVaudoise;
		}

		public Commune getCommuneFinPeriode() {
			return communeFinPeriode;
		}

		public MotifFor getMotifOuvertureForPrincipal() {
			return motifOuvertureForPrincipal;
		}

		public RegDate getDateFermetureSecondaire() {
			return dateFermetureSecondaire;
		}

		public MotifFor getMotifFinSecondaire() {
			return motifFinSecondaire;
		}

		public Pays getPaysFinPeriode() {
			return paysFinPeriode;
		}

		public String getNomCantonHC() {
			return nomCantonHC;
		}

		public String getNomCantonFinPeriode() {
			return nomCantonFinPeriode;
		}
	}


	public static class Erreur extends InfoContribuable {
		public final String message;

		public Erreur(long id, String message) {
			super(id);
			this.message = message;
		}
	}

	public final RegDate dateTraitement;
	public boolean interrompu;
	public int nbContribuable;
	public int periode;

	public final List<InfoContribuableAvecNote> listeContribuableAvecNote = new ArrayList<>();
	public final List<Erreur> erreurs = new ArrayList<>();

	public ListeNoteResults(RegDate dateTraitement, int periode, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
		this.periode = periode;
	}

	public void addContribuableAvecNote(InfoContribuableAvecNote cont) {
		listeContribuableAvecNote.add(cont);
	}

	@Override
	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur(element, e.getMessage()));

	}

	@Override
	public void addAll(ListeNoteResults right) {
		this.nbContribuable += right.nbContribuable;
		listeContribuableAvecNote.addAll(right.listeContribuableAvecNote);
		erreurs.addAll(right.erreurs);

	}

	public int getPeriode() {
		return periode;
	}

	public void setPeriode(int periode) {
		this.periode = periode;
	}
}
