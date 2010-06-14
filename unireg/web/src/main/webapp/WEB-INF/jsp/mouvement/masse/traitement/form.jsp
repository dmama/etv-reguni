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
        <td width="25%"><fmt:message key="label.envoi.colladm.destinataire"/>&nbsp;:</td>
        <td width="25%">
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
        <td width="25%"><fmt:message key="label.pour.archives"/>&nbsp;:</td>
        <td width="25%"><form:checkbox path="mouvementsPourArchives"/></td>
        </td>
	</tr>

	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.etat.mouvement"/>&nbsp;:</td>
        <td width="50%" colspan=2>
            <form:checkbox path="wantATraiter"/>&nbsp;<fmt:message key="option.etat.mouvement.A_TRAITER"/>&nbsp;&nbsp;&nbsp;
            <form:checkbox path="wantAEnvoyer"/>&nbsp;<fmt:message key="option.etat.mouvement.A_ENVOYER"/>&nbsp;&nbsp;&nbsp;
            <form:checkbox path="wantRetire"/>&nbsp;<fmt:message key="option.etat.mouvement.RETIRE"/>
        </td>
        <td width="25%">&nbsp;</td>
    </tr>

</table>

<!-- pour finir, les boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
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
