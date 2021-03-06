package ch.vd.unireg.registrefoncier.immeuble;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.immeuble.graph.AyantDroit;
import ch.vd.unireg.registrefoncier.immeuble.graph.Droit;
import ch.vd.unireg.registrefoncier.immeuble.graph.Immeuble;
import ch.vd.unireg.registrefoncier.immeuble.graph.ImmeubleGraph;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityCheck;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;

@Controller
@RequestMapping(value = "/registrefoncier/immeuble")
public class ImmeubleRFController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits de visualisation des données du Registre Foncier";

	private TiersDAO tiersDAO;
	private TiersService tiersService;
	private AdresseService adresseService;
	private ImmeubleRFDAO immeubleRFDAO;
	private RegistreFoncierService registreFoncierService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setImmeubleRFDAO(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	public void setRegistreFoncierService(RegistreFoncierService registreFoncierService) {
		this.registreFoncierService = registreFoncierService;
	}

	@SecurityCheck(rolesToCheck = {Role.VISU_IMMEUBLES}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "graph.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String graph(@RequestParam(required = false) String idRF,
	                       @RequestParam(required = false) String egrid,
	                       @RequestParam(required = false) Long id,
	                       @RequestParam(required = false) Long ctbId,
	                       @RequestParam(required = false) Long commId,
	                       @RequestParam(required = false) String elementKey,
	                       @RequestParam(required = false, defaultValue = "false") boolean showEstimations,
	                       Model model) {

		final ImmeubleGraph graph = new ImmeubleGraph();

		final String title;
		final String sourceKey;

		// on résoud les ids à partir de la clé de l'élément d3 si possible
		egrid = (egrid == null ? resolveEgrid(elementKey) : egrid);
		ctbId = (ctbId == null ? resolveCtbId(elementKey) : ctbId);
		commId = (commId == null ? resolveCommunauteId(elementKey) : ctbId);

		if (ctbId != null) {
			// on affiche le graphe des droits et immeubles propriété de ce seul contribuable
			final Tiers tiers = tiersDAO.get(ctbId);
			if (!(tiers instanceof Contribuable)) {
				throw new IllegalArgumentException("Le tiers n°" + ctbId + " n'est pas un contribuable");
			}

			final Contribuable ctb = (Contribuable) tiers;
			final List<DroitRF> droits = registreFoncierService.getDroitsForCtb(ctb, true, false, false);
			final List<DroitProprieteRF> droitsPropriete = droits.stream()
					.filter(DroitProprieteRF.class::isInstance)
					.map(DroitProprieteRF.class::cast)
					.collect(Collectors.toList());

			graph.process(droitsPropriete);

			sourceKey = ctb.getRapprochementsRF().stream()
					.filter(AnnulableHelper::nonAnnule)
					.max(DateRangeComparator::compareRanges)
					.map(RapprochementRF::getTiersRF)
					.map(AyantDroit::buildKey)
					.orElse(null);
			title = "Immeubles du contribuable n°" + FormatNumeroHelper.numeroCTBToDisplay(ctbId);
		}
		else if (commId != null) {
			// on affiche le graphe des droits et immeubles propriété de cette communauté

			final CommunauteRF communaute = registreFoncierService.getCommunaute(commId);
			if (communaute == null) {
				throw new ObjectNotFoundException("La communauté avec l'id=" + commId + " n'existe pas.");
			}

			// on s'intéresse aux droits de la propriété *et* aux droits de tous les membres de la communauté
			final List<DroitProprieteRF> droitsPropriete = Stream.concat(communaute.getDroitsPropriete().stream(),
			                                                             communaute.getMembres().stream())
					.filter(AnnulableHelper::nonAnnule)
					.collect(Collectors.toList());

			graph.process(droitsPropriete);

			sourceKey = AyantDroit.buildKey(communaute);
			title = "Immeubles de la communauté n°" + commId;
		}
		else {
			// on affiche le graphe des droits, immeubles et propriétaires de toute la grappe
			final ImmeubleRF immeuble;
			if (id != null) {
				immeuble = immeubleRFDAO.get(id);
			}
			else if (StringUtils.isNotBlank(idRF)) {
				immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF), null);
			}
			else if (StringUtils.isNotBlank(egrid)) {
				immeuble = immeubleRFDAO.findByEgrid(egrid);
			}
			else {
				throw new IllegalArgumentException("Aucun paramètre d'identification de l'immeuble n'a été donné.");
			}

			if (immeuble == null) {
				return null;
			}

			graph.process(immeuble, true);

			sourceKey = Immeuble.buildKey(immeuble);
			title = "Propriétaires et propriétés de l'immeuble " + getSituation(immeuble);
		}

		model.addAttribute("dot", graph.toDot(showEstimations));
		model.addAttribute("selected", sourceKey);
		model.addAttribute("title", title);

		return "registrefoncier/immeuble/graph";
	}

	/**
	 * Analyse la clé de l'élément spécifié et retourne l'id de l'immeuble correspond s'il existe.
	 */
	@Nullable
	private static String resolveEgrid(@Nullable String elementKey) {
		if (StringUtils.isBlank(elementKey)) {
			return null;
		}

		return Immeuble.parseKey(elementKey);
	}

	/**
	 * Analyse la clé de l'élément spécifié et retourne l'id de contribuable correspond s'il existe.
	 */
	@Nullable
	private Long resolveCtbId(@Nullable String elementKey) {

		if (StringUtils.isBlank(elementKey)) {
			return null;
		}

		final Long ayantDroitId = AyantDroit.parseKey(elementKey);
		if (ayantDroitId == null) {
			// pas un ayant-droit
			return null;
		}

		final AyantDroitRF ayantDroit = registreFoncierService.getAyantDroit(ayantDroitId);
		if (ayantDroit == null) {
			// l'ayant-droit n'existe pas
			return null;
		}

		if (!(ayantDroit instanceof TiersRF)) {
			// l'ayant-droit n'est pas un tiers
			return null;
		}

		final TiersRF tiers = (TiersRF) ayantDroit;
		final Contribuable contribuable = tiers.getCtbRapproche();
		if (contribuable == null) {
			// le tiers n'est pas rapproché
			return null;
		}

		return contribuable.getNumero();
	}

	/**
	 * Analyse la clé de l'élément spécifié et retourne l'id de la communauté corresponde s'elle existe.
	 */
	@Nullable
	private Long resolveCommunauteId(@Nullable String elementKey) {

		if (StringUtils.isBlank(elementKey)) {
			return null;
		}

		final Long ayantDroitId = AyantDroit.parseKey(elementKey);
		if (ayantDroitId == null) {
			// pas un ayant-droit
			return null;
		}

		final AyantDroitRF ayantDroit = registreFoncierService.getAyantDroit(ayantDroitId);
		if (ayantDroit == null) {
			// l'ayant-droit n'existe pas
			return null;
		}

		if (!(ayantDroit instanceof CommunauteRF)) {
			// l'ayant-droit n'est pas une communauté
			return null;
		}

		final CommunauteRF communaute = (CommunauteRF) ayantDroit;
		return communaute.getId();
	}

	@NotNull
	private static String getSituation(ImmeubleRF immeuble) {
		final Immeuble i = new Immeuble(immeuble);
		return i.getCommune() + " / " + i.getParcelle();
	}

	@SecurityCheck(rolesToCheck = {Role.VISU_IMMEUBLES}, accessDeniedMessage = ACCESS_DENIED_MESSAGE)
	@RequestMapping(value = "details.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String details(@RequestParam String elementKey, Model model) {

		final Long ayantDroitId = AyantDroit.parseKey(elementKey);
		final String egrid = Immeuble.parseKey(elementKey);
		final Long linkId = Droit.parseKey(elementKey);

		if (ayantDroitId != null) {
			final AyantDroitRF ayantDroit = registreFoncierService.getAyantDroit(ayantDroitId);
			if (ayantDroit != null) {
				if (ayantDroit instanceof CommunauteRF) {
					final CommunauteRF communaute = (CommunauteRF) ayantDroit;
					model.addAttribute("type", CommunauteRF.class.getSimpleName());
					model.addAttribute("communauteId", ayantDroitId);
					model.addAttribute("typeCommunaute", communaute.getType());
				}
				else if (ayantDroit instanceof TiersRF) {
					final TiersRF tiers = (TiersRF) ayantDroit;
					final Contribuable contribuable = tiers.getCtbRapproche();
					if (contribuable == null) {
						model.addAttribute("type", TiersRF.class.getSimpleName());  // un tiers RF non-rapproché
						model.addAttribute("tiersId", ayantDroitId);
						model.addAttribute("noRF", tiers.getNoRF());
						if (tiers instanceof CollectivitePubliqueRF) {
							model.addAttribute("typeTiersRF", "Collectivité publique");
							model.addAttribute("raisonSociale", ((CollectivitePubliqueRF) tiers).getRaisonSociale());
						}
						else if (tiers instanceof PersonneMoraleRF) {
							model.addAttribute("typeTiersRF", "Personne morale");
							model.addAttribute("raisonSociale", ((PersonneMoraleRF)tiers).getRaisonSociale());
							model.addAttribute("numeroRC", ((PersonneMoraleRF)tiers).getNumeroRC());
						}
						else if (tiers instanceof PersonnePhysiqueRF) {
							model.addAttribute("typeTiersRF", "Personne physique");
							model.addAttribute("prenom", ((PersonnePhysiqueRF) tiers).getPrenom());
							model.addAttribute("nom", ((PersonnePhysiqueRF) tiers).getNom());
							model.addAttribute("dateNaissance", ((PersonnePhysiqueRF) tiers).getDateNaissance());
						}
					}
					else {
						model.addAttribute("type", Contribuable.class.getSimpleName());
						model.addAttribute("ctbId", contribuable.getNumero());
						final StringBuilder role = new StringBuilder(HtmlUtils.htmlEscape(contribuable.getRoleLigne1()));
						final String assujettissement = tiersService.getRoleAssujettissement(contribuable, RegDate.get());
						if (StringUtils.isNotBlank(assujettissement)) {
							role.append("<br>").append(HtmlUtils.htmlEscape(assujettissement));
						}
						model.addAttribute("role", role.toString());
						model.addAttribute("adresse", buildAdresse(contribuable));
					}
				}
				else {
					throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + ayantDroit.getClass() + "]");
				}
			}
		}
		else if (egrid != null) {
			final ImmeubleRF immeuble = registreFoncierService.getImmeuble(egrid);
			if (immeuble != null) {
				model.addAttribute("type", ImmeubleRF.class.getSimpleName());
				model.addAttribute("immeuble", new Immeuble(immeuble));
			}
		}
		else if (linkId != null) {
			final DroitRF droit = registreFoncierService.getDroit(linkId);
			if (droit != null) {
				model.addAttribute("type", DroitRF.class.getSimpleName());
				model.addAttribute("dateDebutMetier", droit.getDateDebutMetier());
				model.addAttribute("dateFinMetier", droit.getDateFinMetier());
				model.addAttribute("motifDebut", droit.getMotifDebut());
				model.addAttribute("motifFin", droit.getMotifFin());
				if (droit instanceof DroitProprieteRF) {
					final DroitProprieteRF droitPropriete =(DroitProprieteRF) droit;
					model.addAttribute("typeDroit", "Droit de propriété");
					model.addAttribute("part", droitPropriete.getPart().toString());
					model.addAttribute("regime", droitPropriete.getRegime());
				}
				else if (droit instanceof ServitudeRF) {
					final ServitudeRF servitude =(ServitudeRF) droit;
					model.addAttribute("identifiantDroit", servitude.getIdentifiantDroit().toString());
					model.addAttribute("numeroAffaire", Optional.of(servitude).map(ServitudeRF::getNumeroAffaire).map(IdentifiantAffaireRF::toString).orElse(null));
					if (servitude instanceof UsufruitRF) {
						model.addAttribute("typeDroit", "Usufruit");
					}
					else {
						model.addAttribute("typeDroit", "Droit d'habitation");
					}
				}
			}
		}
		else {
			// élément inconnu
		}

		return "registrefoncier/immeuble/details";
	}

	@NotNull
	private String buildAdresse(Contribuable contribuable) {

		// Adresse envoi
		try {
			final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(contribuable, null, TypeAdresseFiscale.COURRIER, false);

			return String.join("<br>", Stream.of(adresse.getLignes())
					.filter(StringUtils::isNotBlank)
					.map(HtmlUtils::htmlEscape)
					.collect(Collectors.toList()));
		}
		catch (Exception e) {
			return "<span class=\"error\">" + HtmlUtils.htmlEscape(e.getMessage()) + "</span>";
		}
	}
}