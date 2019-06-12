package ch.vd.unireg.tiers.view;

import java.math.BigDecimal;
import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;
import ch.vd.unireg.tiers.AllegementFiscalCommune;
import ch.vd.unireg.tiers.AllegementFiscalConfederation;

public class AllegementFiscalView implements DateRange, Annulable {

	private final long pmId;
	private final long id;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final AllegementFiscal.TypeImpot typeImpot;
	private final AllegementFiscal.TypeCollectivite typeCollectivite;
	private final BigDecimal pourcentage;
	private final boolean annule;
	private final boolean actif;

	private final Integer noOfsCommune;
	private final AllegementFiscalCantonCommune.Type typeICC;
	private final AllegementFiscalConfederation.Type typeIFD;

	private static <T extends Comparable<T>> int compareNullable(@Nullable T o1, @Nullable T o2, boolean nullAtEnd) {
		if (o1 == o2) {
			return 0;
		}

		if (o1 == null) {
			return nullAtEnd ? 1 : -1;
		}
		if (o2 == null) {
			return nullAtEnd ? -1 : 1;
		}
		return o1.compareTo(o2);
	}

	/**
	 * Comparateur pour le tri par défaut des allègements fiscaux affichés
	 */
	public static final Comparator<AllegementFiscalView> DEFAULT_COMPARATOR = new AnnulableHelper.AnnulesApresWrappingComparator<>((o1, o2) -> {
		int comparison = - NullDateBehavior.LATEST.compare(o1.getDateFin(), o2.getDateFin());
		if (comparison == 0) {
			comparison = - NullDateBehavior.EARLIEST.compare(o1.getDateDebut(), o2.getDateDebut());
			if (comparison == 0) {
				comparison = compareNullable(o1.getTypeCollectivite(), o2.getTypeCollectivite(), true);
				if (comparison == 0) {
					comparison = compareNullable(o1.getTypeImpot(), o2.getTypeImpot(), true);
					if (comparison == 0) {
						comparison = compareNullable(o1.getNoOfsCommune(), o2.getNoOfsCommune(), false);
					}
				}
			}
		}
		return comparison;
	});

	public AllegementFiscalView(AllegementFiscal af) {
		this.pmId = af.getEntreprise().getNumero();
		this.id = af.getId();
		this.dateDebut = af.getDateDebut();
		this.dateFin = af.getDateFin();
		this.typeImpot = af.getTypeImpot();
		this.typeCollectivite = af.getTypeCollectivite();
		this.pourcentage = af.getPourcentageAllegement();
		this.annule = af.isAnnule();
		this.actif = af.isValidAt(RegDate.get());

		if (af instanceof AllegementFiscalCommune) {
			this.noOfsCommune = ((AllegementFiscalCommune) af).getNoOfsCommune();
		}
		else {
			this.noOfsCommune = null;
		}
		if (af instanceof AllegementFiscalCantonCommune) {
			this.typeICC = ((AllegementFiscalCantonCommune) af).getType();
		}
		else {
			this.typeICC = null;
		}
		if (af instanceof AllegementFiscalConfederation) {
			this.typeIFD = ((AllegementFiscalConfederation) af).getType();
		}
		else {
			this.typeIFD = null;
		}
	}

	public long getPmId() {
		return pmId;
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

	public AllegementFiscal.TypeImpot getTypeImpot() {
		return typeImpot;
	}

	public AllegementFiscal.TypeCollectivite getTypeCollectivite() {
		return typeCollectivite;
	}

	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public AllegementFiscalCantonCommune.Type getTypeICC() {
		return typeICC;
	}

	public AllegementFiscalConfederation.Type getTypeIFD() {
		return typeIFD;
	}

	public BigDecimal getPourcentage() {
		return pourcentage;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public boolean isActif() {
		return actif;
	}
}
