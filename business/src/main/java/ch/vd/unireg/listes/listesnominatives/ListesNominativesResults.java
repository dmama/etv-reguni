package ch.vd.unireg.listes.listesnominatives;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseCourrierPourRF;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.ListesResults;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

/**
 * Tous les tiers sont placés dans la liste des tiers traités avec la meilleure estimation possible du nom.
 * <p/>
 * Parmis les traités, certains sont en erreur (ils sont alors aussi présents dans la liste des erreurs, avec une explication plus précise).
 */
public class ListesNominativesResults extends ListesResults<ListesNominativesResults> {

	private static final String[] LIGNES_ADRESSE_VIDE = {null, null, null, null, null, null};

	private final List<InfoTiers> tiers = new LinkedList<>();
	private final List<InfoTiersRpt> tiersRpt = new LinkedList<>();

	private final TypeAdresse typeAdressesIncluses;
	private final Set<Long> tiersList;
	private final AdresseService adresseService;
	private final boolean avecContribuablesPP;
	private final boolean avecContribuablesPM;
	private final boolean avecDebiteurs;
	private final boolean pourRPT;

	public ListesNominativesResults(RegDate dateTraitement, int nombreThreads, TypeAdresse typeAdressesIncluses, boolean avecContribuablesPP, boolean avecContribuablesPM, boolean avecDebiteurs,
	                                Set<Long> tiersList, TiersService tiersService, AdresseService adresseService, boolean pourRPT) {
		super(dateTraitement, nombreThreads, tiersService, adresseService);
		this.typeAdressesIncluses = typeAdressesIncluses;
		this.avecContribuablesPP = avecContribuablesPP;
		this.avecContribuablesPM = avecContribuablesPM;
		this.avecDebiteurs = avecDebiteurs;
		this.tiersList = tiersList;
		this.adresseService = adresseService;
		this.pourRPT = pourRPT;
	}

	public static class InfoTiers {
		public final long numeroTiers;
		public final String nomPrenom1;
		public final String nomPrenom2;

		public InfoTiers(long numeroTiers, String nomPrenom1, String nomPrenom2) {
			this.numeroTiers = numeroTiers;
			this.nomPrenom1 = nomPrenom1;
			this.nomPrenom2 = nomPrenom2;
		}
	}

	public static class InfoTiersRpt {
		public final long numeroTiers;
		public final String prenom;
		public final String nom;
		public final String type;

		public InfoTiersRpt(long numeroTiers, String prenom, String nom, String type) {
			this.numeroTiers = numeroTiers;
			this.prenom = prenom;
			this.nom = nom;
			this.type = type;
		}
	}

	public static class InfoTiersAvecAdresseFormattee extends InfoTiers {

		public final String[] adresse;

		public InfoTiersAvecAdresseFormattee(long numeroCtb, String nomPrenom1, String nomPrenom2, String[] adresse) {
			super(numeroCtb, nomPrenom1, nomPrenom2);
			this.adresse = adresse;
		}
	}

	public static class InfoTiersAvecAdresseStructureeRF extends InfoTiers {

		public final String rue;
		public final String npa;
		public final String localite;
		public final String pays;

		public InfoTiersAvecAdresseStructureeRF(long numeroTiers, String nomPrenom1, String nomPrenom2, String rue, String npa, String localite, String pays) {
			super(numeroTiers, nomPrenom1, nomPrenom2);
			this.rue = rue;
			this.npa = npa;
			this.localite = localite;
			this.pays = pays;
		}
	}

	/**
	 * Seuls les PersonnesPhysiques et les MenagesCommuns sont traités
	 */
	@Override
	public void addContribuable(Contribuable ctb) throws Exception {
		if (ctb instanceof PersonnePhysique) {

			final PersonnePhysique pp = (PersonnePhysique) ctb;
			final String nomPrenom = tiersService.getNomPrenom(pp);
			if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
				addTiers(ctb.getNumero(), nomPrenom, null, adresse.getLignes());
			}
			else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
				final AdresseCourrierPourRF adresse = adresseService.getAdressePourRF(ctb, null);
				addContribuable(ctb.getNumero(), nomPrenom, null, adresse.getRueEtNumero(), adresse.getNpa(), adresse.getLocalite(), adresse.getPays());
			}
			else {
				addTiers(ctb.getNumero(), nomPrenom, null);
				//Pour la RPT
				if (pourRPT) {
					final String prenom = tiersService.getDecompositionNomPrenom(pp, true).getPrenom();
					final String nom = tiersService.getDecompositionNomPrenom(pp, true).getNom();
					addTiersRpt(pp.getNumero(), prenom, nom, "PP");
				}

			}
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) ctb;

			final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, null);
			final PersonnePhysique principal = ensembleTiersCouple.getPrincipal();
			final PersonnePhysique conjoint = ensembleTiersCouple.getConjoint();
			if (principal != null || conjoint != null) {
				String nomPrenomPrincipal = null;
				String nomPrenomConjoint = null;
				if (principal != null) {
					nomPrenomPrincipal = tiersService.getNomPrenom(principal);
				}
				if (conjoint != null) {
					nomPrenomConjoint = tiersService.getNomPrenom(conjoint);
				}
				if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
					final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
					addTiers(ctb.getNumero(), nomPrenomPrincipal, nomPrenomConjoint, adresse.getLignes());
				}
				else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
					final AdresseCourrierPourRF adresse = adresseService.getAdressePourRF(ctb, null);
					addContribuable(ctb.getNumero(), nomPrenomPrincipal, nomPrenomConjoint, adresse.getRueEtNumero(), adresse.getNpa(), adresse.getLocalite(), adresse.getPays());
				}
				else {
					addTiers(menage.getNumero(), nomPrenomPrincipal, nomPrenomConjoint);

					//Pour la RPT
					if (pourRPT) {

						if (principal != null) {
							final NomPrenom nomPrenomP = tiersService.getDecompositionNomPrenom(principal, true);
							final String prenomP = nomPrenomP.getPrenom();
							final String nomP = nomPrenomP.getNom();
							addTiersRpt(menage.getNumero(), prenomP, nomP, "MC");
						}
						if (conjoint != null) {
							final NomPrenom nomPrenomC = tiersService.getDecompositionNomPrenom(conjoint, true);
							final String prenomC = nomPrenomC.getPrenom();
							final String nomC = nomPrenomC.getNom();
							addTiersRpt(menage.getNumero(), prenomC, nomC, "MC");
						}
					}


				}
			}
			else {
				addErrorManqueLiensMenage(menage);
			}
		}
		else if (ctb instanceof Entreprise) {
			final Entreprise pm = (Entreprise) ctb;
			final String raisonSociale = tiersService.getDerniereRaisonSociale(pm);

			if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
				addTiers(ctb.getNumero(), raisonSociale, null, adresse.getLignes());
			}
			else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
				final AdresseCourrierPourRF adresse = adresseService.getAdressePourRF(ctb, null);
				addContribuable(ctb.getNumero(), raisonSociale, null, adresse.getRueEtNumero(), adresse.getNpa(), adresse.getLocalite(), adresse.getPays());
			}
			else {
				addTiers(ctb.getNumero(), raisonSociale, null);
			}

		}
	}

	public void addDebiteurPrestationImposable(DebiteurPrestationImposable debiteur) throws Exception {
		final List<String> raisonSociale = tiersService.getRaisonSociale(debiteur);
		final String nom1;
		final String nom2;
		if (!raisonSociale.isEmpty()) {
			nom1 = raisonSociale.get(0);
			if (raisonSociale.size() > 1) {
				nom2 = raisonSociale.get(1);
			}
			else {
				nom2 = null;
			}
		}
		else {
			nom1 = debiteur.getComplementNom();
			nom2 = null;
		}

		if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
			final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(debiteur, null, TypeAdresseFiscale.COURRIER, false);
			addTiers(debiteur.getNumero(), nom1, nom2, adresse.getLignes());
		}
		else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
			addErrorNonConcerneParAdressesRF(debiteur);
		}
		else {
			addTiers(debiteur.getNumero(), nom1, nom2);
		}
	}

	private void addErrorNonConcerneParAdressesRF(Tiers tiers) {
		getListeErreurs().add(new Erreur(tiers.getNumero(), ErreurType.TIERS_NON_CONCERNE_PAR_LES_LISTES_RF, tiers.getClass().getSimpleName()));
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		String nom1Trouve = null;
		String nom2Trouve = null;
		try {
			final List<String> noms = adresseService.getNomCourrier(tiers, null, false);
			if (noms == null || noms.size() < 1) {
				nom1Trouve = NOM_INCONNU;
			}
			else if (noms.size() >= 1) {
				nom1Trouve = noms.get(0);
				if (noms.size() > 1) {
					nom2Trouve = noms.get(1);
				}
			}
		}
		catch (Exception e) {
			nom1Trouve = NOM_INCONNU;
			nom2Trouve = null;
		}
		if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
			String[] lignesAdresse = LIGNES_ADRESSE_VIDE;
			try {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
				lignesAdresse = adresse.getLignes();
			}
			catch (Exception ex) {
				// erreur absorbée... c'est juste pour le log, donc pas important ici
				// (si erreur il y a eu, elle a certainement déjà été vue plus haut)
			}
			addTiers(tiers.getNumero(), nom1Trouve, nom2Trouve, lignesAdresse);

		}
		else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
			String rueEtNumero = null;
			String npa = null;
			String localite = null;
			String pays = null;
			try {
				if (tiers instanceof Contribuable) {
					final AdresseCourrierPourRF adresse = adresseService.getAdressePourRF((Contribuable) tiers, null);
					if (adresse != null) {
						rueEtNumero = adresse.getRueEtNumero();
						npa = adresse.getNpa();
						localite = adresse.getLocalite();
						pays = adresse.getPays();
					}
				}
			}
			catch (Exception ex) {
				// erreur absorbée... c'est juste pour le log, donc pas important ici
				// (si erreur il y a eu, elle a certainement déjà été vue plus haut)
			}
			addContribuable(tiers.getNumero(), nom1Trouve, nom2Trouve, rueEtNumero, npa, localite, pays);

		}
		else {
			addTiers(tiers.getNumero(), nom1Trouve, nom2Trouve);
		}
	}

	private void addTiers(long numeroCtb, String nomPrenomPrincipal, @Nullable String nomPrenomConjoint) {
		this.tiers.add(new InfoTiers(numeroCtb, nomPrenomPrincipal, nomPrenomConjoint));
	}

	private void addTiers(long numeroCtb, String nomPrenomPrincipal, @Nullable String nomPrenomConjoint, String[] lignesAdresse) {
		this.tiers.add(new InfoTiersAvecAdresseFormattee(numeroCtb, nomPrenomPrincipal, nomPrenomConjoint, lignesAdresse));
	}

	private void addTiersRpt(long numeroCtb, String prenom, @Nullable String nom, String type) {
		this.tiersRpt.add(new InfoTiersRpt(numeroCtb, prenom, nom, type));
	}

	private void addContribuable(Long numero, String nomPrenomPrincipal, @Nullable String nomPrenomConjoint, String rueEtNumero, String npa, String localite, String pays) {
		this.tiers.add(new InfoTiersAvecAdresseStructureeRF(numero, nomPrenomPrincipal, nomPrenomConjoint, rueEtNumero, npa, localite, pays));
	}
//		private boolean isSelectionnable(PersonnePhysique pp){
//			final boolean isPasEncoreMort = pp.getDateDeces() == null || pp.getDateDeces().isAfter(RegDate.get(periodeFiscale, 1, 1));
//			final boolean isDejaNee = pp.getDateNaissance()==null || pp.getDateNaissance().isBefore(RegDate.get(periodeFiscale,12,31));
//			return isPasEncoreMort && isDejaNee;
//		}

	public List<InfoTiers> getListeTiers() {
		return this.tiers;
	}

	public int getNombreTiersTraites() {
		return this.tiers.size();
	}

	public List<InfoTiersRpt> getListTiersRpt() {
		return tiersRpt;
	}

	@Override
	public void addAll(ListesNominativesResults source) {
		super.addAll(source);
		this.tiers.addAll(source.tiers);
		this.tiersRpt.addAll(source.tiersRpt);
	}

	@Override
	public void sort() {
		super.sort();
		this.tiers.sort(Comparator.comparingLong(info -> info.numeroTiers));
		this.tiersRpt.sort(Comparator.comparingLong(info -> info.numeroTiers));
	}

	public TypeAdresse getTypeAdressesIncluses() {
		return typeAdressesIncluses;
	}

	public boolean isAvecContribuablesPP() {
		return avecContribuablesPP;
	}

	public boolean isAvecContribuablesPM() {
		return avecContribuablesPM;
	}

	public boolean isAvecDebiteurs() {
		return avecDebiteurs;
	}

	public boolean isPourRPT() {
		return pourRPT;
	}
}

