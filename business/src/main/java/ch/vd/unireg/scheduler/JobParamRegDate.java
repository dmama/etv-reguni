package ch.vd.uniregctb.scheduler;

import java.text.ParseException;

import ch.vd.registre.base.date.PartialDateException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Type de paramètre RegDate.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobParamRegDate extends JobParamType {

	private boolean allowPartial = false;

	public JobParamRegDate() {
		super(RegDate.class);
	}

	public JobParamRegDate(boolean allowPartial) {
		super(RegDate.class);
		this.allowPartial = allowPartial;
	}

	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		try {
			return RegDateHelper.StringFormat.DISPLAY.fromString(s, allowPartial);
		}
		catch (PartialDateException e) {
			throw new IllegalArgumentException(e);
		}
		catch (ParseException e) {
			// on supporte aussi le format dash pour des raisons de compatiblité avec les batches
			try {
				return RegDateHelper.StringFormat.DASH.fromString(s, allowPartial);
			}
			catch (PartialDateException ee) {
				throw new IllegalArgumentException(ee);
			}
			catch (ParseException ee) {
				throw new IllegalArgumentException(e); // <- on renvoie l'exception initiale, par soucis de cohérence
			}
		}
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		if (o == null) {
			return null;
		}
		if (!(o instanceof RegDate)) {
			throw new IllegalArgumentException("L'objet n'est pas de classe RegDate");
		}
		return RegDateHelper.dateToDisplayString((RegDate) o);
	}

}
