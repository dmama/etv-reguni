package ch.vd.uniregctb.xml.party.v4;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.corporation.v4.CorporationFlag;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class CorporationFlagBuilder {

	private static final Set<TypeFlagEntreprise> TYPES_EXPOSES = EnumSet.of(TypeFlagEntreprise.APM_SOC_IMM_SUBVENTIONNEE,
	                                                                        TypeFlagEntreprise.SOC_IMM_ACTIONNAIRES_LOCATAIRES,
	                                                                        TypeFlagEntreprise.SOC_IMM_CARACTERE_SOCIAL,
	                                                                        TypeFlagEntreprise.SOC_IMM_ORDINAIRE,
	                                                                        TypeFlagEntreprise.SOC_IMM_SUBVENTIONNEE,
	                                                                        TypeFlagEntreprise.SOC_SERVICE,
	                                                                        TypeFlagEntreprise.UTILITE_PUBLIQUE);

	@Nullable
	public static CorporationFlag newFlag(FlagEntreprise flag) {
		if (TYPES_EXPOSES.contains(flag.getType())) {
			final CorporationFlag cf = new CorporationFlag();
			cf.setDateFrom(DataHelper.coreToXMLv2(flag.getDateDebut()));
			cf.setDateTo(DataHelper.coreToXMLv2(flag.getDateFin()));
			cf.setType(EnumHelper.coreToXMLv4(flag.getType()));
			return cf;
		}
		else {
			return null;
		}
	}
}
