package ch.vd.uniregctb.migration.pm.rcent.model;

import ch.vd.uniregctb.migration.pm.historizer.container.ValuesDateRanges;
import ch.vd.uniregctb.migration.pm.historizer.container.SingleValueDateRanges;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.Etablissement;

public class RCEntOrganisation {

    /**
     * Identifiant cantonal - "Clé primaire"
     */
    private Long cantonalId;

    /**
     * Nom de l'entreprise
     * Au moins une entrée ouverte!
     */
    private SingleValueDateRanges<String> name;

    /**
     * Siège social de l'entreprise
     */
    private SingleValueDateRanges<Etablissement> headoffice;

    /**
     * Etablissements secondaires
     */
    private ValuesDateRanges<Etablissement> etab;



    public Long getId() {
        return cantonalId;
    }
}
