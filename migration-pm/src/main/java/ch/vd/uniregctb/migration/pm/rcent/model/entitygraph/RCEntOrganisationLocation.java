package ch.vd.uniregctb.migration.pm.rcent.model.entitygraph;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntCommercialRegisterData;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntEntity;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntEntityHistory;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntFunction;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntIdentitier;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntKindOfLocation;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntName;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntNogaCode;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntOtherNames;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntSwissMunicipality;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntUidRegisterData;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntVatRegisterData;
import ch.vd.uniregctb.migration.pm.rcent.model.entitygraph.RCEntIdentification;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryElement;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntHistoryList;
import ch.vd.uniregctb.migration.pm.rcent.model.history.RCEntOrganisationLocationHistoryElement;

public class RCEntOrganisationLocation extends RCEntEntity implements RCEntEntityHistory {

    final RCEntHistoryList<RCEntOrganisationLocationHistoryElement> history;
    /**
     * Identifiant cantonal - "Clé primaire"
     */
    final private Long cantonalId;

    final private RCEntName name;
    private RCEntIdentitier identifier;
    private RCEntOtherNames otherNames;
    private RCEntKindOfLocation kindOfLocation;
    private RCEntSwissMunicipality seat;
    private RCEntCommercialRegisterData commercialRegisterData;
    private RCEntFunction function;
    private RCEntNogaCode nogaCode;
    private RCEntUidRegisterData uidRegisterData;
    private RCEntVatRegisterData vatRegisterData;
    private RCEntIdentification replacedBy;
    private RCEntIdentification inPreplacementOf;

    public RCEntOrganisationLocation(RCEntHistoryList<RCEntOrganisationLocationHistoryElement> history, Long cantonalId, RCEntName name) {
        this.history = history;
        this.cantonalId = cantonalId;
        this.name = name;
    }


    public Long getCantonalId() {
        return cantonalId;
    }

    @NotNull
    @Override
    public RCEntHistoryList<? extends RCEntHistoryElement> getHistory() {
        return history;
    }

    @Override
    public Object getCurrent() {
        return null;
    }
}
