package ch.vd.uniregctb.efacture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;

@Controller
@RequestMapping(value = "/efacture")
public class EFactureController {

	@ResponseBody
	@RequestMapping(value = "/histo.do", method = RequestMethod.GET)
	public HistoriqueDestinataire histo(@RequestParam(value = "ctb") long ctbId) {

		if (!SecurityProvider.isAnyGranted(Role.VISU_ALL)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour visualiser l'historique e-facture d'un contribuable");
		}

		return buildHistoDestinataire(ctbId);
	}

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
		final List<EtatDestinataire> etats = Arrays.asList(new EtatDestinataire(RegDate.get(2012, 9, 12), null, null, "Inscrit suspendu"),
		                                                   new EtatDestinataire(RegDate.get(2012, 9, 13), "Demande de confirmation envoyée", null, "En attente de confirmation"),
		                                                   new EtatDestinataire(RegDate.get(2012, 9, 23), null, null, "Inscrit"));
		destinataire.setEtats(etats);

		// ses demandes et leurs états
		final List<HistoriqueDemande> demandes = new ArrayList<HistoriqueDemande>();
		{
			final HistoriqueDemande demande = new HistoriqueDemande();
			demande.setIdDemande(1);
			demande.setDateDemande(RegDate.get(2012, 9, 11));
			demande.setEtats(Arrays.asList(new EtatDemande(RegDate.get(2012, 9, 12), null, null, "Reçue"),
			                               new EtatDemande(RegDate.get(2012, 9, 13), "Demande de confirmation envoyée", "153677   lKDFG", "Validation en cours"),
			                               new EtatDemande(RegDate.get(2012, 9, 25), null, null, "Acceptée")));
			demandes.add(demande);
		}
		{
			final HistoriqueDemande demande = new HistoriqueDemande();
			demande.setIdDemande(2);
			demande.setDateDemande(RegDate.get(2012, 9, 13));
			demande.setEtats(Arrays.asList(new EtatDemande(RegDate.get(2012, 9, 14), null, null, "Reçue"),
			                               new EtatDemande(RegDate.get(2012, 9, 14), "Autre demande déjà en cours", null, "Refusée")));
			demandes.add(demande);
		}
		destinataire.setDemandes(demandes);

		return destinataire;
	}
}
