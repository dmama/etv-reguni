package ch.vd.uniregctb.listes.listesnominatives;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseCourrierPourRF;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.*;

import java.util.*;

/**
 * Tous les tiers sont placés dans la liste des tiers traités
 * avec la meilleure estimation possible du nom.
 * <p/>
 * Parmis les traités, certains sont en erreur (ils sont alors aussi présents dans la
 * liste des erreurs, avec une explication plus précise).
 */
public class ListesNominativesResults extends ListesResults<ListesNominativesResults> {

    private static final String[] LIGNES_ADRESSE_VIDE = {null, null, null, null, null, null};

    private final List<InfoTiers> tiers = new LinkedList<InfoTiers>();

    private final TypeAdresse typeAdressesIncluses;

	private final boolean avecContribuables;

	private final boolean avecDebiteurs;

	public ListesNominativesResults(RegDate dateTraitement, int nombreThreads, TypeAdresse typeAdressesIncluses, boolean avecContribuables, boolean avecDebiteurs, TiersService tiersService, AdresseService adresseService) {
        super(dateTraitement, nombreThreads, tiersService, adresseService);
        this.typeAdressesIncluses = typeAdressesIncluses;
	    this.avecContribuables = avecContribuables;
	    this.avecDebiteurs = avecDebiteurs;
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
			final String nomPrenom = tiersService.getNomPrenom((PersonnePhysique) ctb);
			if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(ctb, null);
				addTiers(ctb.getNumero(), nomPrenom, null, adresse.getLignes());
			}
			else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
				final AdresseCourrierPourRF adresse = adresseService.getAdressePourRF(ctb, null);
				addContribuable(ctb.getNumero(), nomPrenom, null, adresse.getRueEtNumero(), adresse.getNpa(), adresse.getLocalite(), adresse.getPays());
			}
			else {
				addTiers(ctb.getNumero(), nomPrenom, null);
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
					final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(ctb, null);
					addTiers(ctb.getNumero(), nomPrenomPrincipal, nomPrenomConjoint, adresse.getLignes());
				}
				else if (typeAdressesIncluses == TypeAdresse.STRUCTUREE_RF) {
					final AdresseCourrierPourRF adresse = adresseService.getAdressePourRF(ctb, null);
					addContribuable(ctb.getNumero(), nomPrenomPrincipal, nomPrenomConjoint, adresse.getRueEtNumero(), adresse.getNpa(), adresse.getLocalite(), adresse.getPays());
				}
				else {
					addTiers(menage.getNumero(), nomPrenomPrincipal, nomPrenomConjoint);
				}
			}
			else {
				addErrorManqueLiensMenage(menage);
			}
		}
	}

	public void addDebiteurPrestationImposable(DebiteurPrestationImposable debiteur) throws Exception {
        String nom1 = debiteur.getNom1();
        String nom2 = debiteur.getNom2();
        if ((nom1 == null || nom1.trim().length() == 0) && (nom2 == null || nom2.trim().length() == 0)) {
            nom1 = debiteur.getComplementNom();
            nom2 = null;
        }

        if (typeAdressesIncluses == TypeAdresse.FORMATTEE) {
            final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(debiteur, null);
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
            final List<String> noms = adresseService.getNomCourrier(tiers, null);
            if (noms == null || noms.size() < 1) {
                nom1Trouve = NOM_INCONNU;
            } else if (noms.size() >= 1) {
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
                final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, null);
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

    private void addTiers(long numeroCtb, String nomPrenomPrincipal, String nomPrenomConjoint) {
        this.tiers.add(new InfoTiers(numeroCtb, nomPrenomPrincipal, nomPrenomConjoint));
    }

    private void addTiers(long numeroCtb, String nomPrenomPrincipal, String nomPrenomConjoint, String[] lignesAdresse) {
        this.tiers.add(new InfoTiersAvecAdresseFormattee(numeroCtb, nomPrenomPrincipal, nomPrenomConjoint, lignesAdresse));
    }

	private void addContribuable(Long numero, String nomPrenomPrincipal, String nomPrenomConjoint, String rueEtNumero, String npa, String localite, String pays) {
		this.tiers.add(new InfoTiersAvecAdresseStructureeRF(numero, nomPrenomPrincipal, nomPrenomConjoint, rueEtNumero, npa, localite, pays));
	}

    public List<InfoTiers> getListeTiers() {
        return this.tiers;
    }

    public int getNombreTiersTraites() {
        return this.tiers.size();
    }

    @Override
    public void addAll(ListesNominativesResults source) {
        this.tiers.addAll(source.tiers);
        this.tiersErreur.addAll(source.tiersErreur);
    }

    @Override
    public void sort() {
        super.sort();
        Collections.sort(this.tiers, new Comparator<InfoTiers>() {
            public int compare(InfoTiers o1, InfoTiers o2) {
                final long numero1 = o1.numeroTiers;
                final long numero2 = o2.numeroTiers;
                return numero1 == numero2 ? 0 : (numero1 < numero2 ? -1 : 1);
            }
        });
    }

	public TypeAdresse getTypeAdressesIncluses() {
		return typeAdressesIncluses;
	}

	public boolean isAvecContribuables() {
		return avecContribuables;
	}

	public boolean isAvecDebiteurs() {
		return avecDebiteurs;
	}
}

