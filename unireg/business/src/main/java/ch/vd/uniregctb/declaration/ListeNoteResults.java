package ch.vd.uniregctb.declaration;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;

public class ListeNoteResults extends JobResults<Long, ListeNoteResults> {

	private static final Logger LOGGER = Logger.getLogger(ListeNoteResults.class);

	public ListeNoteResults(RegDate dateTraitement) {
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

			if (adresseEnvoi != null) {
				this.adresseEnvoi = adresseEnvoi.getLignes();
			}
			else {
				this.adresseEnvoi = null;
			}

			nomsPrenoms = new ArrayList<NomPrenom>(2);
			nosAvs = new ArrayList<String>(2);
			fillNomsPrenomsEtNosAvs(ctb, date.year(), tiersService, nomsPrenoms, nosAvs);
			fillSituationFiscale(ctb, date, tiersService, infraService);


		}

		private void fillSituationFiscale(Contribuable ctb, RegDate date, TiersService tiersService, ServiceInfrastructureService infraService) {
			ForGestion forGestion = tiersService.getForGestionActif(ctb, date);
			if (forGestion != null) {
				try {
					final int ofsCommune = forGestion.getNoOfsCommune();
					this.communeVaudoise = infraService.getCommuneByNumeroOfsEtendu(ofsCommune, date);

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
					this.communeHC = infraService.getCommuneByNumeroOfsEtendu(autoriteFiscale, date);
					this.nomCantonHC = infraService.getCantonBySigle(communeHC.getSigleCanton()).getNomMinuscule();
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
					this.communeFinPeriode = infraService.getCommuneByNumeroOfsEtendu(ofsCommune, date);

					if (communeFinPeriode == null) {
						this.paysFinPeriode = infraService.getPays(ofsCommune);

					}
					else{
						this.nomCantonFinPeriode = infraService.getCantonBySigle(communeFinPeriode.getSigleCanton()).getNomMinuscule();
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
				final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp);
				final String noAvs = tiersService.getNumeroAssureSocial(pp);
				nomsPrenoms.add(nomPrenom);
				nosAvs.add(noAvs);
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, annee);
				final PersonnePhysique principal = ensemble.getPrincipal();
				final PersonnePhysique conjoint = ensemble.getConjoint();
				if (principal != null) {
					nomsPrenoms.add(tiersService.getDecompositionNomPrenom(principal));
					nosAvs.add(tiersService.getNumeroAssureSocial(principal));
				}
				if (conjoint != null) {
					nomsPrenoms.add(tiersService.getDecompositionNomPrenom(conjoint));
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
		public String message;

		public Erreur(long id, String message) {
			super(id);
			this.message = message;
		}
	}

	public RegDate dateTraitement;
	public boolean interrompu;
	public int nbContribuable;
	public int periode;

	public List<InfoContribuableAvecNote> listeContribuableAvecNote = new ArrayList<InfoContribuableAvecNote>();
	public List<Erreur> erreurs = new ArrayList<Erreur>();

	public ListeNoteResults(RegDate dateTraitement, int periode) {
		this.dateTraitement = dateTraitement;
		this.periode = periode;
	}

	public void addContribuableAvecNote(InfoContribuableAvecNote cont) {
		listeContribuableAvecNote.add(cont);
	}

	public void addErrorException(Long element, Exception e) {
		erreurs.add(new Erreur(element, e.getMessage()));

	}

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
