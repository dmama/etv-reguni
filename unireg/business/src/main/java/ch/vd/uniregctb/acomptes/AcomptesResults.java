package ch.vd.uniregctb.acomptes;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.declaration.ordinaire.ForsList;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.Indigent;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;

public class AcomptesResults extends ListesResults<AcomptesResults> {

    private final Integer anneeFiscale;

    private final List<InfoContribuableAssujetti> contribuablesAssujettis = new LinkedList<InfoContribuableAssujetti>();

	private final List<InfoContribuableIgnore> contribuablesIgnores = new LinkedList<InfoContribuableIgnore>();

	public static class InfoContribuableAssujetti {
        private final long numeroCtb;
        private final String nom;
        private final String prenom;
        private final InfoAssujettissementContribuable icc;
        private final InfoAssujettissementContribuable ifd;

        public InfoContribuableAssujetti(long numeroCtb, String nom, String prenom, InfoAssujettissementContribuable icc,
                                         InfoAssujettissementContribuable ifd) {
            this.numeroCtb = numeroCtb;
            this.nom = nom;
            this.prenom = prenom;
            this.icc = icc;
            this.ifd = ifd;
        }

        public long getNumeroCtb() {
            return numeroCtb;
        }

        public String getNom() {
            return nom;
        }

        public String getPrenom() {
            return prenom;
        }

        public InfoAssujettissementContribuable getAssujettissementIcc() {
            return icc;
        }

        public InfoAssujettissementContribuable getAssujettissementIfd() {
            return ifd;
        }
    }
	
	public static class InfoContribuableIgnore {
		private final long numeroCtb;
		private final int anneeFiscale;

		public InfoContribuableIgnore(long numeroCtb, int anneeFiscale) {
			this.numeroCtb = numeroCtb;
			this.anneeFiscale = anneeFiscale;
		}

		public long getNumeroCtb() {
			return numeroCtb;
		}

		public int getAnneeFiscale() {
			return anneeFiscale;
		}

		@Override
		public final String toString() {
			final String cause = getCauseIgnorance();
			if (StringUtils.isEmpty(cause)) {
				return String.format("Contribuable %d ignoré pour l'année fiscale %d", numeroCtb, anneeFiscale);
			}
			else {
				return String.format("Contribuable %d ignoré pour l'année fiscale %d (%s)", numeroCtb, anneeFiscale, cause);
			}
		}

		protected String getCauseIgnorance() {
			return null;
		}
	}

	public static class InfoContribuableNonAssujetti extends InfoContribuableIgnore {

		public InfoContribuableNonAssujetti(long numeroCtb, int anneeFiscale) {
			super(numeroCtb, anneeFiscale);
		}

		@Override
		protected String getCauseIgnorance() {
			return "non-assujetti en fin d'année fiscale";
		}
	}

	public static class InfoContribuableNonSoumisAuxAcomptes extends InfoContribuableIgnore {
		private final String descriptionAssujettissement;

		public InfoContribuableNonSoumisAuxAcomptes(long numeroCtb, int anneeFiscale, String descriptionAssujettissement) {
			super(numeroCtb, anneeFiscale);
			this.descriptionAssujettissement = descriptionAssujettissement;
		}

		@Override
		protected String getCauseIgnorance() {
			return descriptionAssujettissement;
		}
	}

    public AcomptesResults(RegDate dateTraitement, int nombreThreads, Integer anneeFiscale, TiersService tiersService) {
        super(dateTraitement, nombreThreads, tiersService);
        this.anneeFiscale = anneeFiscale;
    }

    /**
     * Seuls les PersonnesPhysiques et les MenagesCommuns sont traités
     */
    @Override
    public void addContribuable(Contribuable ctb) {

        if (ctb instanceof PersonnePhysique) {
	        final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom((PersonnePhysique) ctb);
	        if (nomPrenom != null) {
                addContribuable(ctb, nomPrenom.getNom(), nomPrenom.getPrenom());
	        }
	        else {
		        addContribuable(ctb, NOM_INCONNU, null);
	        }

        } else if (ctb instanceof MenageCommun) {
            final MenageCommun menage = (MenageCommun) ctb;
            final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(menage, anneeFiscale);

	        // si le ménage a été fermé l'année d'avant, il n'apparaîtra pas dans cet ensemble, mais
	        // aura pu être sélectionné quand même dans la requête de départ en raison de la présence
	        // d'un for fiscal cette année là.
	        final PersonnePhysique principal;
	        if (ensembleTiersCouple.getPrincipal() == null) {
		        final EnsembleTiersCouple ensembleAnneePrecedente = tiersService.getEnsembleTiersCouple(menage, anneeFiscale - 1);
		        principal = ensembleAnneePrecedente.getPrincipal();
	        }
	        else {
		        principal = ensembleTiersCouple.getPrincipal();
	        }
            if (principal != null) {
	            final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(principal);
	            if (nomPrenom != null) {
                    addContribuable(ctb, nomPrenom.getNom(), nomPrenom.getPrenom());
	            }
	            else {
		            addContribuable(ctb, NOM_INCONNU, null);
	            }
            } else {
                addErrorManqueLiensMenage(menage);
            }
        }
    }

	public static class InfoAssujettissementContribuable {
		public final TypeContribuable typeContribuable;
		public final Integer noOfsForPrincipal;
		public final Integer noOfsForGestion;
		public final Set<Integer> ofsForsSecondaires;
		public final int anneeFiscale;

		private InfoAssujettissementContribuable(TypeContribuable typeContribuable, Integer noOfsForPrincipal, Integer noOfsForGestion, Set<Integer> ofsForsSecondaires, int anneeFiscale) {
			this.typeContribuable = typeContribuable;
			this.noOfsForPrincipal = noOfsForPrincipal;
			this.noOfsForGestion = noOfsForGestion;
			this.ofsForsSecondaires = ofsForsSecondaires == null ? Collections.<Integer>emptySet() : Collections.unmodifiableSet(ofsForsSecondaires);
			this.anneeFiscale = anneeFiscale;
		}
	}

	protected InfoAssujettissementContribuable calculerInfoAssujettissement(Contribuable ctb, int anneeFiscale) {

		TypeContribuable typeContribuable;
		Integer noOfsForPrincipal = null;
		final Integer noOfsForGestion;

		final RegDate dateFinPeriode = RegDate.get(anneeFiscale, 12, 31);
		final ForFiscalPrincipal forFiscalPrincipal = ctb.getForFiscalPrincipalAt(dateFinPeriode);
		if (forFiscalPrincipal != null) {
			if (forFiscalPrincipal.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				noOfsForPrincipal = forFiscalPrincipal.getNumeroOfsAutoriteFiscale();
			}
		}
		else {
			addContribuableIgnorePasAssujetti(ctb, anneeFiscale);
			return null;
		}

		Set<Integer> ofsForsSecondaires = null;
		final ForGestion forGestion = this.tiersService.getForGestionActif(ctb, dateFinPeriode);
		if (		(forGestion != null)
				&& 	(forGestion.getSousjacent() != null)
				&& 	(forGestion.getSousjacent().getMotifRattachement() != null)
				&& 	(forGestion.getSousjacent().getMotifRattachement() != MotifRattachement.DIPLOMATE_SUISSE)) {
			noOfsForGestion = forGestion.getNoOfsCommune();
			try {
				final List<Assujettissement> assujettissements = Assujettissement.determine(ctb, anneeFiscale);
				if (assujettissements != null) {
					final Assujettissement assujettissement = assujettissements.get(assujettissements.size() - 1);
					if ((assujettissement instanceof VaudoisOrdinaire) || (assujettissement instanceof Indigent)) {
						typeContribuable = TypeContribuable.VAUDOIS_ORDINAIRE;
					}
					else if (assujettissement instanceof HorsSuisse) {
						typeContribuable = TypeContribuable.HORS_SUISSE;
					}
					else if (assujettissement instanceof HorsCanton) {
						typeContribuable = TypeContribuable.HORS_CANTON;
					}
					else if (assujettissement instanceof VaudoisDepense) {
						typeContribuable = TypeContribuable.VAUDOIS_DEPENSE;
					}
					else if (assujettissement instanceof DiplomateSuisse && assujettissement.getFors().secondairesDansLaPeriode.contains(MotifRattachement.IMMEUBLE_PRIVE)) {
						typeContribuable = TypeContribuable.DIPLOMATE_SUISSE;
					}
					else {
						addContribuableIgnoreNonSoumisAuxAcomptes(ctb, anneeFiscale, assujettissement.getDescription());
						return null;
					}

					// information sur les communes des fors secondaires
					final ForsList<ForFiscalSecondaire> forsSecondaires = assujettissement.getFors().secondaires;
					if (!forsSecondaires.isEmpty()) {
						ofsForsSecondaires = new TreeSet<Integer>();
						for (ForFiscalSecondaire ffs : forsSecondaires) {
							ofsForsSecondaires.add(ffs.getNumeroOfsAutoriteFiscale());
						}
					}
				}
				else {
					addContribuableIgnorePasAssujetti(ctb, anneeFiscale);
					return null;
				}
			}
			catch (AssujettissementException e) {
				addErrorException(ctb, e);
				return null;
			}
		}
		else {
			return null;
		}

		return new InfoAssujettissementContribuable(typeContribuable, noOfsForPrincipal, noOfsForGestion, ofsForsSecondaires, anneeFiscale);
	}

	private void addContribuable(Contribuable ctb, String nom, String prenom) {

		// ICC
	    final InfoAssujettissementContribuable icc = calculerInfoAssujettissement(ctb, anneeFiscale);

		// IFD : attention, les HC ne sont pas soumis à l'IFD
	    InfoAssujettissementContribuable ifd = calculerInfoAssujettissement(ctb, anneeFiscale - 1);
		if (ifd != null && ifd.typeContribuable == TypeContribuable.HORS_CANTON) {
			ifd = null;
		}

		if (icc != null || ifd != null) {
			this.contribuablesAssujettis.add(new InfoContribuableAssujetti(ctb.getNumero(), nom, prenom, icc, ifd));
		}
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// on ne fait rien en particulier...
	}

	public int getNombreContribuablesAssujettisTraites() {
        return this.contribuablesAssujettis.size();
    }

	private void addContribuableIgnorePasAssujetti(Contribuable ctb, int anneeFiscale) {
		this.contribuablesIgnores.add(new InfoContribuableNonAssujetti(ctb.getNumero(), anneeFiscale));
	}

	public void addContribuableIgnoreNonSoumisAuxAcomptes(Contribuable ctb, int anneeFiscale, String assujettissement) {
	    this.contribuablesIgnores.add(new InfoContribuableNonSoumisAuxAcomptes(ctb.getNumero(), anneeFiscale, assujettissement));
    }

    @Override
    public void addAll(AcomptesResults source) {
	    super.addAll(source);
        this.contribuablesAssujettis.addAll(source.contribuablesAssujettis);
	    this.contribuablesIgnores.addAll(source.contribuablesIgnores);
    }

    @Override
    public void sort() {
        super.sort();

        Collections.sort(this.contribuablesAssujettis, new Comparator<InfoContribuableAssujetti>() {
            @Override
            public int compare(InfoContribuableAssujetti o1, InfoContribuableAssujetti o2) {
                final long numero1 = o1.getNumeroCtb();
                final long numero2 = o2.getNumeroCtb();
                return numero1 == numero2 ? 0 : (numero1 < numero2 ? -1 : 1);
            }
        });

	    Collections.sort(this.contribuablesIgnores, new Comparator<InfoContribuableIgnore>() {
		    @Override
		    public int compare(InfoContribuableIgnore o1, InfoContribuableIgnore o2) {
			    final long numero1 = o1.getNumeroCtb();
			    final long numero2 = o2.getNumeroCtb();
			    return numero1 == numero2 ? 0 : (numero1 < numero2 ? -1 : 1);
		    }
	    });
    }

    public TiersService getTiersService() {
        return tiersService;
    }

    public List<InfoContribuableAssujetti> getListeContribuablesAssujettis() {
        return this.contribuablesAssujettis;
    }

	public List<InfoContribuableIgnore> getContribuablesIgnores() {
		return contribuablesIgnores;
	}
}
