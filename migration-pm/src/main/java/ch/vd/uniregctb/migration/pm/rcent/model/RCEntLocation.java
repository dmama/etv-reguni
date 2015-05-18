package ch.vd.uniregctb.migration.pm.rcent.model;

import ch.vd.evd0022.v1.Function;
import ch.vd.evd0022.v1.Identification;
import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.KindOfLocation;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.evd0022.v1.UidRegisterData;
import ch.vd.evd0022.v1.VatRegisterData;
import ch.vd.uniregctb.migration.pm.historizer.container.ValuesDateRanges;
import ch.vd.uniregctb.migration.pm.historizer.container.SingleValueDateRanges;

public class RCEntLocation {

    /**
     * Identifiant cantonal - "Clé primaire"
     */
    private Long cantonalId;
    private ValuesDateRanges<Identifier> identifier;
    private SingleValueDateRanges<String> name;
    private ValuesDateRanges<String> otherName;
    private SingleValueDateRanges<KindOfLocation> kindOfLocation;
    private SingleValueDateRanges<SwissMunicipality> headoffice;
    private RCEntCommercialRegisterData commercialRegisterData;
    private SingleValueDateRanges<Function> function;
    private SingleValueDateRanges<String> nogaCode;
    private SingleValueDateRanges<UidRegisterData> uidRegisterData;
    private SingleValueDateRanges<VatRegisterData> vatRegisterData;
    private SingleValueDateRanges<Identification> replacedBy;
    private SingleValueDateRanges<Identification> inPreplacementOf;


    public Long getCantonalId() {
        return cantonalId;
    }

}
