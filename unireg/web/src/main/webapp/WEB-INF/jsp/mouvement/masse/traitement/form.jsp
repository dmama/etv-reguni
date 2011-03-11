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
			<script>
				$(function() {
					autocomplete_infra('collectiviteAdministrative', '#collAdmDestinataire', function(item) {
						if (item) {
							$('#noCollAdmDestinataire').val(item.id1); // le numéro de collectivité
						}
						else {
							$('#collAdmDestinataire').val(null);
							$('#noCollAdmDestinataire').val(null);
						}
					});
				});
			</script>
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
		<td width="50%" style="text-align: center;">
			<input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher"/>
			&nbsp;
			<input type="submit" value="<fmt:message key="label.bouton.effacer"/>" name="effacer" />
			<c:if test="${command.resultSize > 0}">
				&nbsp;
				<input type="submit" value="<fmt:message key="label.bouton.exporter"/>" name="exporter"/>
			</c:if>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>
