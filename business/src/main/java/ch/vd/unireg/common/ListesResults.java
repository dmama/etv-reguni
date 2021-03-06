package ch.vd.unireg.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

public abstract class ListesResults<T extends ListesResults<T>> extends JobResults<Long, T> {

    protected static final String NOM_INCONNU = "Nom inconnu";

    protected final RegDate dateTraitement;

	protected final int nombreThreads;

    protected final TiersService tiersService;

    protected final List<Erreur> tiersErreur = new LinkedList<>();

    protected boolean interrompu = false;

    public abstract void addContribuable(Contribuable ctb) throws Exception;

    public abstract void addTiersEnErreur(Tiers tiers);

	public enum ErreurType {
        MANQUE_LIENS_MENAGE("Le contribuable ménage n'a pas de lien vers des personnes physiques"), // -------
		TIERS_NON_CONCERNE_PAR_LES_LISTES_RF("Le tiers n'est ni une personne physique ni un ménage commun"), 
        EXCEPTION(EXCEPTION_DESCRIPTION); // -----------------------------------------------------------------

        private final String description;

        ErreurType(String description) {
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
	    super(tiersService, adresseService);
        this.dateTraitement = dateTraitement;
	    this.nombreThreads = nombreThreads;
	    this.tiersService = tiersService;
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

	@Override
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
        this.tiersErreur.sort(Comparator.comparingLong(e -> e.noCtb));
    }

	public int getNombreThreads() {
		return nombreThreads;
	}

	@Override
	public void addAll(T sources) {
		this.tiersErreur.addAll(sources.tiersErreur);
	}
}
