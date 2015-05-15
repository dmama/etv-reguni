package ch.vd.uniregctb.migration.pm.rcent.model;

import java.util.List;

import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.container.MultipleValuesDateRanges;
import ch.vd.uniregctb.migration.pm.historizer.container.SingleValueDateRanges;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.Etablissement;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementPrincipal;
import ch.vd.uniregctb.migration.pm.historizer.extractor.organization.EtablissementSecondaire;

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
    private MultipleValuesDateRanges<Etablissement> etab;



    public Long getId() {
        return cantonalId;
    }
}
