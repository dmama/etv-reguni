<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="listEvenementsEch" type="java.util.List<ch.vd.uniregctb.evenement.ech.view.EvenementCivilEchElementListeRechercheView>"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
    <tiles:put name="head">
        <script type="text/javascript">
             $(document).ready(function () {
                 $('#modeLotEvenement').change( function () {
                    if (this.checked) {
                        $('#tableForm tr.toggle').hide()
                    } else {
                        $('#tableForm tr.toggle').show()
                    }
                 }).change();

                 $('#rechercher').click( function () {
                 	$('#formRechercheEvenements').attr('action','rechercher.do');
                 });

                 $('#effacer').click( function () {
                 	window.location.href = 'effacer.do';
                 	return false;
                 });

             });
        </script>
    </tiles:put>

  	<tiles:put name="title"><fmt:message key="title.recherche.evenements.ech" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/evenements.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheEvenements" commandName="evenementEchCriteria">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

        <c:set var="sortable" value='${not evenementEchCriteria.modeLotEvenement}' scope="page" />
		<display:table 	name="listEvenementsEch" id="tableEvtsEch" pagesize="25" requestURI="/evenement/ech/nav-list.do" defaultsort="1" defaultorder="descending" sort="external" class="display_table" partialList="true" size="listEvenementsEchSize">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner"><fmt:message key="banner.un.evenement.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.evenements.trouves" /></span></display:setProperty>

			<!-- ID -->
			<display:column property="id" sortable ="${sortable}" titleKey="label.evenement" href="visu.do" paramId="id" paramProperty="id" sortName="id" />
			<!-- NO Individu + Conjoint -->
			<display:column sortable ="${sortable}" titleKey="label.individu" sortProperty="numeroIndividu" sortName="numeroIndividu">
				<a href="#" class="staticTip" id="tt-${tableEvtsEch.numeroIndividu}">
					<div id="tt-${tableEvtsEch.numeroIndividu}-tooltip" style="display:none;">
						<h3>Individu n°${tableEvtsEch.numeroIndividu}</h3>
						<fieldset>
							<unireg:nextRowClass reset="1"/>
							<table>
								<tr class="<unireg:nextRowClass/>">
									<td width="50%"><fmt:message key="label.numero.registre.habitant"/> :</td>
									<td width="50%">${tableEvtsEch.individu.numeroIndividu}</td>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td width="50%"><fmt:message key="label.nom.prenom"/> :</td>
									<c:if test="${tableEvtsEch.individu != null}">
										<td width="50%">${tableEvtsEch.individu.nom}&nbsp;${tableEvtsEch.individu.prenomUsuel}</td>
									</c:if>
								</tr>
								<tr class="<unireg:nextRowClass/>">
									<td width="50%"><fmt:message key="label.nouveau.numero.avs"/> :</td>
									<c:if test="${tableEvtsEch.individu != null}">
										<td width="50%"><unireg:numAVS numeroAssureSocial="${tableEvtsEch.individu.numeroAssureSocial}"></unireg:numAVS></td>
									</c:if>
								</tr>
								<c:if test="${tableEvtsEch.adresse.ligne1 != null}">
									<tr class="<unireg:nextRowClass/>">
										<td><fmt:message key="label.adresse.courrier.active"/>&nbsp;:</td>
										<td>${tableEvtsEch.adresse.ligne1}</td>
									</tr>
								</c:if>
								<c:if test="${tableEvtsEch.adresse.ligne2 != null }">
									<tr class="<unireg:nextRowClass/>">
										<td>&nbsp;</td>
										<td>${tableEvtsEch.adresse.ligne2}</td>
									</tr>
								</c:if>
								<c:if test="${tableEvtsEch.adresse.ligne3 != null }">
									<tr class="<unireg:nextRowClass/>">
										<td>&nbsp;</td>
										<td>${tableEvtsEch.adresse.ligne3}</td>
									</tr>
								</c:if>
								<c:if test="${tableEvtsEch.adresse.ligne4 != null }">
									<tr class="<unireg:nextRowClass/>">
										<td>&nbsp;</td>
										<td>${tableEvtsEch.adresse.ligne4}</td>
									</tr>
								</c:if>
								<c:if test="${tableEvtsEch.adresse.ligne5 != null}">
									<tr class="<unireg:nextRowClass/>">
										<td>&nbsp;</td>
										<td>${tableEvtsEch.adresse.ligne5}</td>
									</tr>
								</c:if>
								<c:if test="${tableEvtsEch.adresse.ligne6 != null}">
									<tr class="<unireg:nextRowClass/>">
										<td>&nbsp;</td>
										<td>${tableEvtsEch.adresse.ligne6}</td>
									</tr>
								</c:if>
								<tr class="<unireg:nextRowClass/>">
									<td><fmt:message key="label.date.naissance"/>&nbsp;:</td>
									<c:if test="${tableEvtsEch.individu != null}">
										<td><unireg:regdate regdate="${tableEvtsEch.individu.dateNaissance}"/></td>
									</c:if>
								</tr>
							</table>
						</fieldset>
					</div>
					<span>${tableEvtsEch.numeroIndividu}&nbsp;</span>
				</a>

			</display:column>
			<!-- NO CTB -->
			<display:column titleKey="label.numero.contribuable">
				<c:if test="${tableEvtsEch.numeroCTB != null}">
					<a href="<c:url value="../../tiers/visu.do"/>?id=${tableEvtsEch.numeroCTB}"><unireg:numCTB numero="${tableEvtsEch.numeroCTB}" /></a>
				</c:if>
			</display:column>
			<!-- Nom  /Prénom -->
			<display:column titleKey="label.prenom.nom">
				<c:out value="${tableEvtsEch.nom}" />
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.type.evenement" sortName="type">
				<fmt:message key="option.type.evenement.ech.${tableEvtsEch.type}" />
			</display:column>
			<!-- Type evt -->
			<display:column sortable ="${sortable}" titleKey="label.action.evenement" sortName="action">
				<fmt:message key="option.action.evenement.ech.${tableEvtsEch.action}" />
			</display:column>
			<!-- Date evenement -->
			<display:column sortable ="${sortable}" titleKey="label.date.evenement" sortName="dateEvenement">
				<unireg:regdate regdate="${tableEvtsEch.dateEvenement}" />
			</display:column>
			<!-- Date traitement -->
			<display:column property="dateTraitement" sortable ="${sortable}" titleKey="label.date.traitement" format="{0,date,dd.MM.yyyy}" sortName="dateTraitement" />
			<!-- Status evt -->
			<display:column sortable ="${sortable}" titleKey="label.etat.evenement" sortName="etat" >
				<fmt:message key="option.etat.evenement.${tableEvtsEch.etat}" />
			</display:column>
			<display:column titleKey="label.commentaire.traitement">
				<i><c:out value="${tableEvtsEch.commentaireTraitement}"/></i>
			</display:column>
			<display:column style="action">
				<a href="#" class="detail" title="Détails" onclick="open_details(<c:out value="${tableEvtsEch.id}"/>); return false;">&nbsp;</a>
			</display:column>
			<display:column style="action">
				<c:if test="${tableEvtsEch.id != null}">
					<unireg:consulterLog entityNature="EvenementEch" entityId="${tableEvtsEch.id}"/>
				</c:if>
		</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>
<script language="JavaScript">

	EvtCivil.list = [
		<c:forEach var="evt" items="${listEvenementsEch}" varStatus="evtStatus">
		${evt.id}
		<c:if test="${!evtStatus.last}">
		,
		</c:if>
		</c:forEach>
	]

</script>
<script language="javascript">

	function activate_static_evt_tooltips(obj) {
		$(".staticTip", obj).tooltip({
			                             items: "[id]",
			                             position: { my: "right top", at: "right bottom" },
			                             content: function() {
				                             // on détermine l'id de la div qui contient le tooltip à afficher
				                             var id = $(this).attr("id") + "-tooltip";
				                             id = id.replace(/\./g, '\\.'); // on escape les points

				                             // on récupère la div et on affiche son contenu
				                             var div = $("#" + id);
				                             return div.html();
			                             }
		                             });
	}


	/**
	 * Ouvrir le panneau de détail après avoir déterminé les ids de l'événement précédent et suivant.
	 *
	 * Selon la position de l'événement dans la liste, le précédent ou le suivant peut être null.
	 *
	 * Il faut gérer spécialement le cas où l'id de l'événement dont l'ouverture est demandé ne figure pas dans la liste, par exemple
	 * parce qu'il a été forcé. Dans ce cas, il faut prendre l'id conservé dans la variable nextId qui contient une copie de la dernière id suivante.
	 * Lorsqu'on ne trouve pas de nextId, on prend le premier de la liste comme suivant.
	 *
	 * @param evtId l'id de l'événement dont on souhaite afficher le détail.
	 * @returns {null}
	 */
	function open_details(evtId) {
		if (!EvtCivil.list || EvtCivil.list.length == 0) return null;
		var evtPrecedant = null;
		var evtSuivant = null;
		var indexEvt = EvtCivil.list.indexOf(evtId);
		if (indexEvt == -1) {
			if (EvtCivil.nextId) {
				evtSuivant = EvtCivil.nextId;
			}
			if (!evtSuivant) {
				evtSuivant = EvtCivil.list[0];
			}
		}
		else {
			if (indexEvt > 0) {
				evtPrecedant = EvtCivil.list[indexEvt - 1];
			}
			if (indexEvt < EvtCivil.list.length - 1) {
				evtSuivant = EvtCivil.list[indexEvt + 1];
			}
		}
		EvtCivil.nextId = evtSuivant;
		EvtCivil.open_details(evtId, evtPrecedant, evtSuivant)
	}

	EvtCivil.doRecycle = function(id) {
		$.ajax({
			       type: "POST",
			       url: "recyclerVersListe.do",
			       data: {"id": id},
			       success: function(data) {
				       if (data.message) {
					       alert(data.message);
				       }
				       open_details(id);
			       },
			       error: function(data) {
				       alert("L'opération de recyclage a échoué pour une raison inconnue.")
			       }
		       });
	};

	EvtCivil.doForce = function(id) {
		if (confirm('Voulez-vous réellement forcer l\'état de cet événement civil ?')) {
			$.ajax({
				       type: "POST",
				       url: "forcerVersListe.do",
				       data: {"id": id},
				       success: function(data) {
					       open_details(id);
				       },
				       error: function(data) {
					       alert("L'opération de forçage a échoué pour une raison inconnue.")
				       }
			       });
		}
	};

	activate_static_evt_tooltips();

</script>