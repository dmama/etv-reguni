package ch.vd.uniregctb.evenement.regpp.view;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementCriteriaView;
import ch.vd.uniregctb.evenement.civil.EvenementCivilCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementRegPPCriteriaView extends EvenementCriteriaView<TypeEvenementCivil> {

	private static final long serialVersionUID = 1L;

	@Override
	protected Class<TypeEvenementCivil> getTypeClass() {
		return TypeEvenementCivil.class;
	}
}
