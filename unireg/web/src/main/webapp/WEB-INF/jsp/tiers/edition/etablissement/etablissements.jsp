<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Etablissements -->
<fieldset>
	<legend><span><fmt:message key="label.etablissements" /></span></legend>

	<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

	<table border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<unireg:raccourciAjouter link="../tiers/etablissement/create.do?numeroCtbAss=${command.tiers.numero}" tooltip="Ajouter &eacute;tablissement" display="label.bouton.ajouter"/>
			</td>
		</tr>
	</table>
	
	<jsp:include page="../../common/etablissements.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>	
</fieldset>
<!-- Fin Etablissements -->
