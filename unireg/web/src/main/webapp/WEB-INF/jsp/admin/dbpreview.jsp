<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String"></tiles:put>
	<tiles:put name="title" type="String">Preview des données de la base</tiles:put>
	<tiles:put name="connected" type="String"></tiles:put>
	<tiles:put name="body" type="String">

		Les tiers suivants sont présents dans la base de données (100 premiers tiers uniquement) :
		<table>
			<c:forEach items="${command.infoTiers}" var="i">
			<tr class="<unireg:nextRowClass/>" >
				<td><a href="../tiers/visu.do?id=${i.numero}"><unireg:numCTB numero="${i.numero}"/></a></td>
				<td><c:out value="${i.nomsPrenoms}"/></td>
				<td><c:out value="${i.type}"/></td>
			</tr>
			</c:forEach>
		</table>

	</tiles:put>
</tiles:insert>