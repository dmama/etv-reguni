package ch.vd.uniregctb.registrefoncier;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.tiers.Contribuable;

public class RegistreFoncierServiceImpl implements RegistreFoncierService {

	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	public void setBatimentRFDAO(BatimentRFDAO batimentRFDAO) {
		this.batimentRFDAO = batimentRFDAO;
	}

	public void setAyantDroitRFDAO(AyantDroitRFDAO ayantDroitRFDAO) {
		this.ayantDroitRFDAO = ayantDroitRFDAO;
	}

	@NotNull
	@Override
	public List<DroitRF> getDroitsForCtb(@NotNull Contribuable ctb) {
		return ctb.getRapprochementsRF().stream()
				.filter(r -> r.isValidAt(null))         // on ne prend que les rapprochements valides
				.map(RapprochementRF::getTiersRF)       // on général, il n'y a qu'un tiers RF, mais le modèle permet d'en avoir plusieurs
				.flatMap(r -> r.getDroits().stream())   // si on a plusieurs tiers rapprochés, on prend l'ensemble des droits
				.sorted(new DateRangeComparator<>(DateRangeComparator.CompareOrder.ASCENDING))
				.collect(Collectors.toList());
	}

	@Nullable
	@Override
	public ImmeubleRF getImmeuble(long immeubleId) {
		return immeubleRFDAO.get(immeubleId);
	}

	@Nullable
	@Override
	public BatimentRF getBatiment(long batimentId) {
		return batimentRFDAO.get(batimentId);
	}

	@Nullable
	@Override
	public CommunauteRFInfo getCommunauteInfo(long communauteId) {
		return ayantDroitRFDAO.getCommunauteInfo(communauteId);
	}
}
