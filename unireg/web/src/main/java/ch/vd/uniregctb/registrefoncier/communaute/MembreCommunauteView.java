package ch.vd.uniregctb.registrefoncier.communaute;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class MembreCommunauteView {

	/**
	 * Id technique de l'ayant-droit
	 */
	@Nullable
	private final Long id;
	private final Long ctbId;
	private final String role;
	private final String nom;
	private final String prenom;
	private final RegDate dateNaissance;
	private final RegDate dateDeces;
	private final String forPrincipal;

	public MembreCommunauteView(@Nullable Long id, @Nullable Contribuable ctb, @NotNull TiersService tiersService) {
		this.id = id;
		if (ctb == null) {
			this.ctbId = null;
			this.role = null;
			this.nom = null;
			this.prenom = null;
			this.dateNaissance = null;
			this.dateDeces = null;
			this.forPrincipal = null;
		}
		else {
			this.ctbId = ctb.getNumero();
			this.role = ctb.getRoleLigne1();
			if (ctb instanceof PersonnePhysique) {
				final NomPrenom decompo = tiersService.getDecompositionNomPrenom((PersonnePhysique) ctb, false);
				this.nom = decompo.getNom();
				this.prenom = decompo.getPrenom();
				this.dateNaissance = tiersService.getDateNaissance((PersonnePhysique) ctb);
				this.dateDeces = tiersService.getDateDeces((PersonnePhysique) ctb);
			}
			else {
				this.nom = tiersService.getNomRaisonSociale(ctb);
				this.prenom = null;
				this.dateNaissance = null;
				this.dateDeces = null;
			}
			this.forPrincipal = Stream.concat(Stream.of(ctb.getForFiscalPrincipalAt(null)), tiersService.getForsFiscauxVirtuels(ctb, true).stream())
					.filter(Objects::nonNull)
					.filter(f -> f.isValidAt(null))
					.findFirst()
					.map(tiersService::getLocalisationAsString)
					.orElse(null);
		}
	}

	public MembreCommunauteView(@NotNull AyantDroitRF ayantDroit, @NotNull TiersService tiersService) {
		// on va chercher le contribuable rapproché au tiers RF
		this(ayantDroit.getId(), Optional.of(ayantDroit)
				     .filter(TiersRF.class::isInstance)
				     .map(TiersRF.class::cast)
				     .map(TiersRF::getCtbRapproche)
				     .orElse(null), tiersService);

	}

	@Nullable
	public Long getId() {
		return id;
	}

	public Long getCtbId() {
		return ctbId;
	}

	public String getRole() {
		return role;
	}

	public String getNom() {
		return nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public String getForPrincipal() {
		return forPrincipal;
	}
}
