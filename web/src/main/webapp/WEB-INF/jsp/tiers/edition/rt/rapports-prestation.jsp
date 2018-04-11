<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Rapports Prestations -->
<fieldset>
	<legend><span><fmt:message key="label.rapports.prestation" /></span></legend>
	<table border="0">
		<tr>
			<td>
				<unireg:linkTo name="Ajouter" action="/rapports-prestation/search-sourcier.do" params="{numeroDebiteur:${command.tiers.numero}}" link_class="add" title="Ajouter rapport de travail"/>
			</td>
		</tr>
	</table>
	<jsp:include page="../../common/rapports-prestation.jsp">
		<jsp:param name="page" value="edit"/>
	</jsp:include>
	
</fieldset>
<!-- Fin Rapports Prestations -->
