<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapports Prestations -->
<fieldset>
	<legend><span><fmt:message key="label.rapports.prestation" /></span></legend>
	<table border="0">
		<tr>
			<td>
				<unireg:raccourciAjouter link="../rt/list-sourcier.do?numeroDpi=${command.tiers.numero}" tooltip="Ajouter rapport" display="label.bouton.ajouter"/>
			</td>
		</tr>
	</table>
	<jsp:include page="../../common/rapports-prestation.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>
<!-- Fin Rapports Prestations -->
