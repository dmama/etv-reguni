package ch.vd.uniregctb.registrefoncier.communaute;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Information d'un modèle de communauté restreinte à un tiers donné.
 */
public class ModeleCommunauteForTiersView {

	private final long id;
	private final long ctbId;
	private final List<MembreCommunauteView> membres;
	private final PrincipalCommunauteRFView principalCourant;
	/**
	 * Les principaux triés du plus récent ou plus vieux.
	 */
	private final List<PrincipalCommunauteRFView> principaux;
	private final List<RegroupementRFView> regroupements = new ArrayList<>();

	public ModeleCommunauteForTiersView(long ctbId,
	                                    @NotNull ModeleCommunauteRF modele,
	                                    @NotNull TiersService tiersService,
	                                    @NotNull RegistreFoncierService registreFoncierService) {
		this.id = modele.getId();
		this.membres = modele.getMembres().stream()
				.map(m -> new MembreCommunauteView(m, tiersService, registreFoncierService))
				.collect(Collectors.toList());
		this.principaux = registreFoncierService.buildPrincipalHisto(modele).stream()
				.sorted(new DateRangeComparator<>().reversed()) // du plus récent au plus vieux
				.map(p -> new PrincipalCommunauteRFView(p, tiersService, registreFoncierService))
				.collect(Collectors.toList());
		this.principalCourant = this.principaux.isEmpty() ? null : this.principaux.get(0);
		this.ctbId = ctbId;
	}

	public long getId() {
		return id;
	}

	public long getCtbId() {
		return ctbId;
	}

	public List<MembreCommunauteView> getMembres() {
		return membres;
	}

	public PrincipalCommunauteRFView getPrincipalCourant() {
		return principalCourant;
	}

	public List<PrincipalCommunauteRFView> getPrincipaux() {
		return principaux;
	}

	public void addRegroupement(RegroupementRFView regroupement) {
		this.regroupements.add(regroupement);
	}

	public List<RegroupementRFView> getRegroupements() {
		return regroupements;
	}
}
