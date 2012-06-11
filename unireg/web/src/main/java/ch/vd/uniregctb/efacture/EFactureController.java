package ch.vd.uniregctb.efacture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

@Controller
@RequestMapping(value = "/efacture")
public class EFactureController {

	private static final String CTB = "ctb";
	private static final String ID_DEMANDE = "idDemande";

	@ResponseBody
	@RequestMapping(value = "/histo.do", method = RequestMethod.GET)
	public HistoriqueDestinataire histo(@RequestParam(value = CTB, required = true) long ctbId) {

		if (!SecurityProvider.isAnyGranted(Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour visualiser l'historique e-facture d'un contribuable");
		}

		return buildHistoDestinataire(ctbId);
	}

	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String edit(Model model, @RequestParam(value = CTB, required = true) long ctbId) {

		if (!SecurityProvider.isAnyGranted(Role.GEST_EFACTURE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour interagir avec les états e-facture d'un contribuable");
		}

		final HistoriqueDestinataire histo = buildHistoDestinataire(ctbId);
		if (histo == null) {
			throw new ActionException("Le contribuable ne possède aucun état e-facture avec lequel il est possible d'interagir");
		}

		model.addAttribute("histo", histo);
		return "tiers/edition/efacture/edit";
	}

	@RequestMapping(value = "/suspend.do", method = RequestMethod.POST)
	public String suspend(@RequestParam(value = CTB, required = true) long ctbId) {
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/activate.do", method = RequestMethod.POST)
	public String activate(@RequestParam(value = CTB, required = true) long ctbId) {
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/validate.do", method = RequestMethod.POST)
	public String validate(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) long idDemande) {
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/refuse.do", method = RequestMethod.POST)
	public String refuse(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) long idDemande) {
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/wait-signature.do", method = RequestMethod.POST)
	public String waitForSignature(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) long idDemande) {
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@RequestMapping(value = "/wait-contact.do", method = RequestMethod.POST)
	public String waitForContact(@RequestParam(value = CTB, required = true) long ctbId, @RequestParam(value = ID_DEMANDE, required = true) long idDemande) {
		// TODO jde à faire
		return String.format("redirect:/tiers/visu.do?id=%d", ctbId);
	}

	@Nullable
	private HistoriqueDestinataire buildHistoDestinataire(long ctbId) {

		// TODO jde : ramolir un peu ce code...

		// pour le moment, tous les numéros de contribuables pairs ont de la e-facture, les impairs non
		if (ctbId % 2 == 1) {
			return null;
		}

		// construction de la structure de base
		final HistoriqueDestinataire destinataire = new HistoriqueDestinataire();
		destinataire.setCtbId(ctbId);

		// les états du destinataire lui-même
		// un contribuable sur deux qui touche à la e-facture n'est pas encore inscrit
		if (ctbId % 4 == 0) {
			destinataire.setEtats(Arrays.asList(new EtatDestinataire(RegDate.get(2012, 9, 12), null, null, "Inscrit suspendu"),
			                                    new EtatDestinataire(RegDate.get(2012, 9, 13), "Demande de confirmation envoyée", null, "En attente de confirmation"),
			                                    new EtatDestinataire(RegDate.get(2012, 9, 23), null, null, "Inscrit")));
		}
		else {
			destinataire.setEtats(Arrays.asList(new EtatDestinataire(RegDate.get(2012, 9, 12), null, null, "Inscrit suspendu"),
			                                    new EtatDestinataire(RegDate.get(2012, 9, 13), "Demande de confirmation envoyée", null, "En attente de confirmation")));
		}

		// ses demandes et leurs états
		final List<HistoriqueDemande> demandes = new ArrayList<HistoriqueDemande>();
		{
			final HistoriqueDemande demande = new HistoriqueDemande();
			demande.setIdDemande(1);
			demande.setDateDemande(RegDate.get(2012, 9, 11));

			// un contribuable sur deux a une demande en attente de confirmation signature
			if (ctbId % 4 == 0) {
				demande.setEtats(Arrays.asList(new EtatDemande(RegDate.get(2012, 9, 12), null, null, "Reçue", TypeEtatDemande.RECUE),
				                               new EtatDemande(RegDate.get(2012, 9, 13), "Demande de confirmation envoyée", new ArchiveKey(TypeDocumentEditique.E_FACTURE_CONFIRMATION_INSCRIPTION.name(), "153677   lKDFG"), "Validation en cours", TypeEtatDemande.EN_ATTENTE_SIGNATURE),
				                               new EtatDemande(RegDate.get(2012, 9, 25), null, null, "Acceptée", TypeEtatDemande.VALIDEE)));
			}
			else {
				demande.setEtats(Arrays.asList(new EtatDemande(RegDate.get(2012, 9, 12), null, null, "Reçue", TypeEtatDemande.RECUE),
				                               new EtatDemande(RegDate.get(2012, 9, 13), "Demande de confirmation envoyée", new ArchiveKey(TypeDocumentEditique.E_FACTURE_CONFIRMATION_INSCRIPTION.name(), "153677   lKDFG"), "Validation en cours", TypeEtatDemande.EN_ATTENTE_SIGNATURE)));
			}
			demandes.add(demande);
		}
		{
			final HistoriqueDemande demande = new HistoriqueDemande();
			demande.setIdDemande(2);
			demande.setDateDemande(RegDate.get(2012, 9, 13));
			demande.setEtats(Arrays.asList(new EtatDemande(RegDate.get(2012, 9, 14), null, null, "Reçue", TypeEtatDemande.RECUE),
			                               new EtatDemande(RegDate.get(2012, 9, 14), "Autre demande déjà en cours", null, "Refusée", TypeEtatDemande.REFUSEE)));
			demandes.add(demande);
		}
		destinataire.setDemandes(demandes);


		//
		// !!! ne pas oublier d'inverser l'ordre chronologique des éléments !!!
		//
		destinataire.setEtats(revertList(destinataire.getEtats()));
		destinataire.setDemandes(revertList(destinataire.getDemandes()));
		for (HistoriqueDemande demande : destinataire.getDemandes()) {
			demande.setEtats(revertList(demande.getEtats()));
		}
		return destinataire;
	}

	private static <T> List<T> revertList(List<T> source) {
		if (source == null || source.isEmpty()) {
			return source;
		}
		else {
			final List<T> dest = new ArrayList<T>(source.size());
			final ListIterator<T> iterator = source.listIterator(source.size());
			while (iterator.hasPrevious()) {
				dest.add(iterator.previous());
			}
			return dest;
		}
	}
}
