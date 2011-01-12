<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Debiteur Prestation Imposable -->

<fieldset>
	<legend><span><fmt:message key="label.debiteur.is" /></span></legend>
	
	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<unireg:raccourciAjouter link="../tiers/edit.do?nature=DebiteurPrestationImposable&amp;numeroCtbAss=${command.tiers.numero}" tooltip="Ajouter d&eacute;biteur" display="label.bouton.ajouter"/>
			</td>
		</tr>
	</table>
	<jsp:include page="../../common/debiteur.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
</fieldset>
<!-- Fin Debiteur Prestation Imposable -->