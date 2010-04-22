<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<table>

	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td><fmt:message key="label.numero.tiers" />&nbsp;:</td>
		<td colspan="2">
			<form:input  path="noCtbFormatte" id="noCtbFormatte" cssClass="number"/>
			<form:errors path="noCtbFormatte" cssClass="error"/>
		</td>
		<td>&nbsp;</td>
	</tr>

	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
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

	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
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

	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr id="choixDestinataireEnvoi" class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
        <td><fmt:message key="label.envoi.colladm.destinataire"/>&nbsp;:</td>
        <td>
            <form:input path="collAdmDestinataire" id="collAdmDestinataire" />
            <form:hidden path="noCollAdmDestinataire" id="noCollAdmDestinataire"  />
            <script type="text/javascript">
            function libCollAdmDestinataire_onChange(row) {
                document.forms["formRechercherMouvementsMasse"].noCollAdmDestinataire.value = (row ? row.noColAdm : "");
            }
            </script>
            <jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
                <jsp:param name="inputId" value="collAdmDestinataire" />
                <jsp:param name="dataValueField" value="nomCourt" />
                <jsp:param name="dataTextField" value="{nomCourt}" />
                <jsp:param name="dataSource" value="selectionnerCollectiviteAdministrative" />
                <jsp:param name="onChange" value="libCollAdmDestinataire_onChange" />
                <jsp:param name="autoSynchrone" value="false"/>
            </jsp:include>
        </td>
        <td><fmt:message key="label.envoi.utilisateur"/>&nbsp;:</td>
        <td>
            <form:input path="individuDestinataire" id="individuDestinataire" />
            <form:hidden path="noIndividuDestinataire" id="noIndividuDestinataire"  />
            <script type="text/javascript">
                    function individuDestinataire_onChange(row) {
                        var form = document.forms["formRechercherMouvementsMasse"];
                        form.noIndividuDestinataire.value = ( row ? row.individuNoTechnique : "");
                    }
            </script>
            <jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
                <jsp:param name="inputId" value="individuDestinataire" />
                <jsp:param name="dataValueField" value="visaOperateur" />
                <jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
                <jsp:param name="dataSource" value="selectionnerUtilisateur" />
                <jsp:param name="onChange" value="individuDestinataire_onChange" />
                <jsp:param name="autoSynchrone" value="false"/>
            </jsp:include>
        </td>
	</tr>

    <!-- On ne fait pas cet incrément du numéro de ligne car cette ligne et la ligne
         du dessus sont mutuellement exclusives (ou pas là du tout -->
	<!--c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" /-->

	<tr id="ligneVide" class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>">
	    <td colspan=4>&nbsp;</td>
	</tr>

	<tr id="choixLocalisationReception" class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
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
            <script type="text/javascript">
                    function individuReception_onChange(row) {
                        var form = document.forms["formRechercherMouvementsMasse"];
                        form.noIndividuReception.value = ( row ? row.individuNoTechnique : "");
                    }
            </script>
            <jsp:include page="/WEB-INF/jsp/include/autocomplete.jsp">
                <jsp:param name="inputId" value="individuReception" />
                <jsp:param name="dataValueField" value="visaOperateur" />
                <jsp:param name="dataTextField" value="{nom} {prenom} ({visaOperateur})" />
                <jsp:param name="dataSource" value="selectionnerUtilisateur" />
                <jsp:param name="onChange" value="individuReception_onChange" />
                <jsp:param name="autoSynchrone" value="false"/>
            </jsp:include>
	    </td>
	    <td id="localisationNonUtilisateur" width="50%" colspan=2>&nbsp;</td>
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

        onChangeTypeMouvement(document.getElementById("typeMouvement"));
        onChangeLocalisationReception(document.getElementById("localisationReception"));
    </script>

	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
	    <td><fmt:message key="label.inclure.mouvements.annules"/>&nbsp;:</td>
        <td width="25%"><form:checkbox path="mouvementsAnnulesInclus"/></td>
        <td colspan=2>&nbsp;<td>
    </tr>

</table>

<!-- pour finir, les boutons -->
<table border="0">
	<c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	<tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher"/></div>
		</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.effacer"/>" name="effacer" /></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
