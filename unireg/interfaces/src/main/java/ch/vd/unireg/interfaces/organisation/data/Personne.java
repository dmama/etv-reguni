package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.Sexe;

public class Personne {

    private final SourceDonnees sourceDesDonnees;
    private final Long noAvs;
    @NotNull
    private final String nom;
    private final String prenom;
    private final Sexe sexe;

    /**
     * Date de naissance. Peut être partielle:
     *   AAAA-MM-JJ, par exemple 1999-05-01 ;
     *   AAAA-MM, par exemple 1999-05 ;
     *   AAAA, par exemple 1999.
     */
    private RegDate dateDeNaissance;

    public Personne(RegDate dateDeNaissance, Long noAvs, @NotNull String nom, String prenom, Sexe sexe,
                    SourceDonnees sourceDesDonnees) {
        this.dateDeNaissance = dateDeNaissance;
        this.noAvs = noAvs;
        this.nom = nom;
        this.prenom = prenom;
        this.sexe = sexe;
        this.sourceDesDonnees = sourceDesDonnees;
    }

    public enum SourceDonnees {
        BRUTES_RCENT,
        ENRICHIES_RCPERS,
        BRUTES_RCENT_AVEC_RCPERS_INDISPONIBLE;
    }

	public RegDate getDateDeNaissance() {
		return dateDeNaissance;
	}

	public Long getNoAvs() {
		return noAvs;
	}

	@NotNull
	public String getNom() {
		return nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public SourceDonnees getSourceDesDonnees() {
		return sourceDesDonnees;
	}
}
