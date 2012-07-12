package ch.vd.uniregctb.type;

import java.util.HashMap;
import java.util.Map;

public enum ActionEvenementCivilEch {

	PREMIERE_LIVRAISON(1,1),
	ANNULATION(3,3),
	CORRECTION(4,2);

	private final int echCode;
	private final int priorite;

	private static final Map<Integer, ActionEvenementCivilEch> typesByCode;

	static {
		typesByCode = new HashMap<Integer, ActionEvenementCivilEch>(ActionEvenementCivilEch.values().length);
		for (ActionEvenementCivilEch mod : ActionEvenementCivilEch.values()) {
			final ActionEvenementCivilEch old = typesByCode.put(mod.echCode, mod);
			if (old != null) {
				throw new IllegalArgumentException(String.format("Code %d utilisé plusieurs fois!", old.echCode));
			}
		}
	}

	private ActionEvenementCivilEch(int code, int priorite) {
		this.echCode = code;
		this.priorite = priorite;
	}

	public static ActionEvenementCivilEch fromEchCode(int code) {
		return typesByCode.get(code);
	}

	public static ActionEvenementCivilEch fromEchCode(String code) {
		return fromEchCode(Integer.valueOf(code));
	}

	public int getEchCode() {
		return echCode;
	}

	public int getPriorite() {
		return priorite;
	}

}
