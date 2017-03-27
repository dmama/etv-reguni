package ch.vd.uniregctb.registrefoncier;

import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.rf.GenrePropriete;

public class DroitView implements DateRange {

	private static final Map<Class<? extends DroitRF>, String> DROIT_DISPLAY_STRING = buildTypeDroitDisplayStrings();

	private static Map<Class<? extends DroitRF>, String> buildTypeDroitDisplayStrings() {
		final Map<Class<? extends DroitRF>, String> map = new HashMap<>();
		map.put(DroitHabitationRF.class, "Droit d'habitation");
		map.put(DroitProprieteCommunauteRF.class, "Propriété");
		map.put(DroitProprietePersonneMoraleRF.class, "Propriété");
		map.put(DroitProprietePersonnePhysiqueRF.class, "Propriété");
		map.put(UsufruitRF.class, "Usufruit");
		return map;
	}

	private final Long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Class<? extends DroitRF> classe;
	private final GenrePropriete regimePropriete;
	private final Fraction part;

	public DroitView(DroitRF droit) {
		this.id = droit.getId();
		this.dateDebut = droit.getDateDebutMetier();
		this.dateFin = droit.getDateFinMetier();
		this.classe = droit.getClass();
		if (droit instanceof DroitProprieteRF) {
			this.regimePropriete = ((DroitProprieteRF) droit).getRegime();
			this.part = ((DroitProprieteRF) droit).getPart();
		}
		else {
			this.regimePropriete = null;
			this.part = null;
		}
	}

	public Long getId() {
		return id;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public String getType() {
		return DROIT_DISPLAY_STRING.get(this.classe);
	}

	public GenrePropriete getRegimePropriete() {
		return regimePropriete;
	}

	public Fraction getPart() {
		return part;
	}
}
