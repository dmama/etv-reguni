package ch.vd.unireg.registrefoncier.communaute;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.TiersService;

/**
 * Informations sur un modèle de communauté.
 */
public class ModeleCommunauteView {

	private final long id;
	private final List<MembreCommunauteView> membres;
	private final PrincipalCommunauteRFView principalCourant;
	/**
	 * Les principaux triés du plus récent ou plus vieux.
	 */
	private final List<PrincipalCommunauteRFView> principaux;
	/**
	 * Les regroupements triés du plus récent ou plus vieux.
	 */
	private final List<RegroupementRFView> regroupements;

	public ModeleCommunauteView(@NotNull ModeleCommunauteRF modele,
	                            @NotNull TiersService tiersService,
	                            @NotNull RegistreFoncierService registreFoncierService) {
		this.id = modele.getId();
		this.membres = modele.getMembres().stream()
				.map(m -> new MembreCommunauteView(m, tiersService, registreFoncierService))
				.collect(Collectors.toList());
		this.principaux = registreFoncierService.buildPrincipalHisto(modele,
		                                                             true,  // on veut voir les principaux annulés
		                                                             false) // on veut différencier les principaux explicites du principal par défaut
				.stream()
				.sorted(new AnnulableHelper.AnnulableDateRangeComparator<>(true)) // du plus récent au plus vieux
				.map(p -> new PrincipalCommunauteRFView(p, tiersService, registreFoncierService))
				.collect(Collectors.toList());
		this.principalCourant = this.principaux.stream()
				.filter(AnnulableHelper::nonAnnule) // [SIFISC-30135] il faut ignorer les principaux annulés
				.findFirst()
				.orElse(null);
		this.regroupements = modele.getRegroupements().stream()
				.filter(AnnulableHelper::nonAnnule)
				.sorted(new DateRangeComparator<>().reversed()) // du plus récent au plus vieux
				.map(r -> new RegroupementRFView(r, registreFoncierService))
				.collect(Collectors.toList());
	}

	public long getId() {
		return id;
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

	public List<RegroupementRFView> getRegroupements() {
		return regroupements;
	}
}
