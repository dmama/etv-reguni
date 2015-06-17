package ch.vd.unireg.interfaces.organisation.data;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

public class Personne {

    private Integer cantonalId;
    private SourceDonnees sourceDesDonnees;
    private Long noAvs;
    @NotNull
    private String nom;
    private String prenom;
    private Sexe sexe;
    /**
     * Date de naissance. Peut être partielle:
     *   AAAA-MM-JJ, par exemple 1999-05-01 ;
     *   AAAA-MM, par exemple 1999-05 ;
     *   AAAA, par exemple 1999.
     */
    private RegDate dateDeNaissance;


    public Personne(Integer cantonalId, RegDate dateDeNaissance, Long noAvs, @NotNull String nom, String prenom, Sexe sexe,
                    SourceDonnees sourceDesDonnees) {
        this.cantonalId = cantonalId;
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

    public enum Sexe {
        MASCULIN,
        FEMININ
    }
}
