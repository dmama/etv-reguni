<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<table>

	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.numero.tiers" />&nbsp;:</td>
		<td colspan="2">
			<form:input  path="noCtbFormatte" id="noCtbFormatte" cssClass="number"/>
			<form:errors path="noCtbFormatte" cssClass="error"/>
		</td>
		<td>&nbsp;</td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.date.mouvement.du"/>&nbsp;:</td>
        <td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateMouvementMin" />
				<jsp:param name="id" value="dateMouvementMin" />
			</jsp:include>
        </td>
        <td width="25%"><fmt:message key="label.date.mouvement.au"/>&nbsp;:</td>
        <td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateMouvementMax" />
				<jsp:param name="id" value="dateMouvementMax" />
			</jsp:include>
        </td>
    </tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.type.mouvement"/>&nbsp;:</td>
        <td width="25%">
            <form:select id="typeMouvement" path="typeMouvement" onchange="onChangeTypeMouvement(this)">
                <form:option value=""/>
                <c:forEach items="${typesMouvement}" var="type">
                    <form:option value="${type.key}">${type.value}</form:option>
                </c:forEach>
            </form:select>
        </td>
        <td width="25%"><fmt:message key="label.etat.mouvement"/>&nbsp;:</td>
        <td width="25%">
            <form:select id="etatMouvement" path="etatMouvement">
                <form:option value=""/>
                <c:forEach items="${etatsMouvement}" var="etat">
                    <form:option value="${etat.key}">${etat.value}</form:option>
                </c:forEach>
            </form:select>
        </td>
    </tr>

    <%-- On gèle la classe de la ligne car cette ligne et les deux lignes du dessous sont mutuellement exclusives --%>
	<tr id="choixDestinataireEnvoi" class="<unireg:nextRowClass frozen='true'/>" >
        <td><fmt:message key="label.envoi.colladm.destinataire"/>&nbsp;:</td>
        <td>
            <form:input path="collAdmDestinataire" id="collAdmDestinataire" />
            <form:hidden path="noCollAdmDestinataire" id="noCollAdmDestinataire"  />
			<script>
				$(function() {
					Autocomplete.infra('collectiviteAdministrative', '#collAdmDestinataire', true, function(item) {
						$('#noCollAdmDestinataire').val(item ? item.id1 : null); // le numéro de collectivité
					});
				});
			</script>
        </td>
        <td><fmt:message key="label.envoi.utilisateur"/>&nbsp;:</td>
        <td>
            <form:input path="individuDestinataire" id="individuDestinataire" />
            <form:hidden path="noIndividuDestinataire" id="noIndividuDestinataire"  />
			<script>
				$(function() {
					Autocomplete.security('user', '#individuDestinataire', false, function(item) {
						if (item) {
							$('#noIndividuDestinataire').val(item.id2); // le numéro technique
						}
						else {
							$('#individuDestinataire').val(null);
							$('#noIndividuDestinataire').val(null);
						}
					});
				});
			</script>
        </td>
	</tr>

    <%-- On gèle la classe de la ligne car cette ligne et les deux lignes immédiatement dessus et dessous sont mutuellement exclusives --%>
	<tr id="ligneVide" class="<unireg:nextRowClass frozen='true'/>">
	    <td colspan=4>&nbsp;</td>
	</tr>

	<tr id="choixLocalisationReception" class="<unireg:nextRowClass/>" >
	    <td width="25%"><fmt:message key="label.reception.type"/>&nbsp;:</td>
	    <td width="25%">
            <form:select id="localisationReception" path="localisationReception" onchange="onChangeLocalisationReception(this)">
                <form:option value=""/>
                <c:forEach items="${localisations}" var="type">
                    <form:option value="${type.key}">${type.value}</form:option>
                </c:forEach>
            </form:select>
	    </td>
	    <td id="labelUtilisateurReception" width="25%"><fmt:message key="label.reception.utilisateur"/>&nbsp;:</td>
	    <td id="choixUtilisateurReception" width="25%">
            <form:input path="individuReception" id="individuReception" />
            <form:hidden path="noIndividuReception" id="noIndividuReception"  />
			<script>
				$(function() {
					Autocomplete.security('user', '#individuReception', false, function(item) {
						if (item) {
							$('#noIndividuReception').val(item.id2); // le numéro technique
						}
						else {
							$('#individuReception').val(null);
							$('#noIndividuReception').val(null);
						}
					});
				});
			</script>
	    </td>
	    <td id="localisationNonUtilisateur" width="50%" colspan=2>&nbsp;</td>
    </tr>

	<tr class="<unireg:nextRowClass/>" >
	    <td><fmt:message key="label.inclure.mouvements.seulement.derniers"/>&nbsp;:</td>
        <td width="25%"><form:checkbox id="seulementDernierMouvement" path="seulementDernierMouvementDossiers" onchange="onChangeChoixSeulementDerniersMouvements(this)"/></td>
        <td colspan=2>&nbsp;</td>
	</tr>

    <%-- On gèle la classe de la ligne car cette ligne et la ligne du dessous sont mutuellement exclusives --%>
	<tr id="choixMouvementsAnnules" class="<unireg:nextRowClass frozen='true'/>" >
	    <td><fmt:message key="label.inclure.mouvements.annules"/>&nbsp;:</td>
        <td width="25%"><form:checkbox path="mouvementsAnnulesInclus"/></td>
        <td colspan=2>&nbsp;</td>
    </tr>

    <tr id="ligneVide2" class="<unireg:nextRowClass/>" >
    	<td colspan=4>&nbsp;</td>
    </tr>

    <script type="text/javascript">
        function fixDisplay(idElement, visible) {
            var elt = document.getElementById(idElement);
            if (elt != null) {
                if (visible) {
                    elt.style.display = "";
                }
                else {
                    elt.style.display = "none";
                }
            }
        }

        function onChangeTypeMouvement(selected) {
            var showDestinataire = false;
            var showReception = false;
            if (selected.value == "EnvoiDossier") {
                showDestinataire = true;
            }
            else if (selected.value == "ReceptionDossier") {
                showReception = true;
            }
            fixDisplay("choixDestinataireEnvoi", showDestinataire);
            fixDisplay("choixLocalisationReception", showReception);
            fixDisplay("ligneVide", !showDestinataire && !showReception);
        }

        function onChangeLocalisationReception(selected) {
            var showUtilisateur = false;
            if (selected.value == 'PERSONNE') {
                showUtilisateur = true;
            }
            fixDisplay("labelUtilisateurReception", showUtilisateur);
            fixDisplay("choixUtilisateurReception", showUtilisateur);
            fixDisplay("localisationNonUtilisateur", !showUtilisateur);
        }

        function onChangeChoixSeulementDerniersMouvements(box) {
        	fixDisplay("choixMouvementsAnnules", !box.checked);
        	fixDisplay("ligneVide2", box.checked);
       	}

        onChangeTypeMouvement(document.getElementById("typeMouvement"));
        onChangeLocalisationReception(document.getElementById("localisationReception"));
        onChangeChoixSeulementDerniersMouvements(document.getElementById("seulementDernierMouvement"));
    </script>

</table>

<!-- pour finir, les boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="50%" style="text-align: center;">
			<input type="submit" value="<fmt:message key='label.bouton.rechercher'/>"/>
			&nbsp;
			<input type="button" value="<fmt:message key='label.bouton.effacer'/>" onclick="document.location.href='consulter.do?action=EFFACER'"/>
		</td>
		<td width="25%">
			<c:if test="${montrerExport}">
				<input type="button" value="<fmt:message key='label.bouton.exporter'/>" onclick="document.location.href='consulter.do?action=EXPORTER'"/>
			</c:if>
			&nbsp;
		</td>
	</tr>
</table>
