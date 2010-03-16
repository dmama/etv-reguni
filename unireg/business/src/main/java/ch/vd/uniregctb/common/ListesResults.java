package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

import java.util.*;

public abstract class ListesResults<T extends ListesResults<T>> extends JobResults {

    protected static final String NOM_INCONNU = "Nom inconnu";

    protected final RegDate dateTraitement;

	protected final int nombreThreads;

    protected final TiersService tiersService;

    protected final AdresseService adresseService;

    protected final List<Erreur> tiersErreur = new LinkedList<Erreur>();

    protected boolean interrompu = false;

    public abstract void addContribuable(Contribuable ctb) throws Exception;

    public abstract void addTiersEnErreur(Tiers tiers);

	public enum ErreurType {
        MANQUE_LIENS_MENAGE("Le contribuable ménage n'a pas de lien vers des personnes physiques"), // -------
		TIERS_NON_CONCERNE_PAR_LES_LISTES_RF("Le tiers n'est ni une personne physique ni un ménage commun"), 
        EXCEPTION(EXCEPTION_DESCRIPTION); // -----------------------------------------------------------------

        private final String description;

        private ErreurType(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    public static class Erreur {
        public final ErreurType raison;
        public final long noCtb;
        public final String details;

        public Erreur(long noCtb, ErreurType raison, String details) {
            this.noCtb = noCtb;
            this.raison = raison;
            this.details = details;
        }

        public String getDescriptionRaison() {
            return raison.description;
        }
    }

    public ListesResults(RegDate dateTraitement, int nombreThreads, TiersService tiersService, AdresseService adresseService) {
        this.dateTraitement = dateTraitement;
	    this.nombreThreads = nombreThreads;
	    this.tiersService = tiersService;
        this.adresseService = adresseService;
    }

    public List<Erreur> getListeErreurs() {
        return this.tiersErreur;
    }

    public boolean isInterrompu() {
        return interrompu;
    }

    public void setInterrompu(boolean interrompu) {
        this.interrompu = interrompu;
    }

    public RegDate getDateTraitement() {
        return dateTraitement;
    }

    public void addErrorException(Tiers tiers, Exception e) {
	    addErrorException(tiers.getNumero(), e);
        addTiersEnErreur(tiers);
    }

	public void addErrorException(Long id, Exception e) {
		final String message = buildErrorMessage(e);
		this.tiersErreur.add(new Erreur(id, ErreurType.EXCEPTION, message));
	}

    protected final String buildErrorMessage(Exception e) {
        final String message;
        if (e.getMessage() != null && e.getMessage().trim().length() > 0) {
            message = String.format("%s - %s", e.getClass().getName(), e.getMessage().trim());
        } else {
            message = String.format("%s - %s", e.getClass().getName(), Arrays.toString(e.getStackTrace()));
        }
        return message;
    }

    public void addErrorManqueLiensMenage(MenageCommun menage) {
        this.tiersErreur.add(new Erreur(menage.getNumero(), ErreurType.MANQUE_LIENS_MENAGE, null));
        addTiersEnErreur(menage);
    }

    public void sort() {

        Collections.sort(this.tiersErreur, new Comparator<Erreur>() {
            public int compare(Erreur o1, Erreur o2) {
                final long numero1 = o1.noCtb;
                final long numero2 = o2.noCtb;
                return numero1 == numero2 ? 0 : (numero1 < numero2 ? -1 : 1);
            }
        });
    }

	public int getNombreThreads() {
		return nombreThreads;
	}

	public abstract void addAll(T sources);
}
