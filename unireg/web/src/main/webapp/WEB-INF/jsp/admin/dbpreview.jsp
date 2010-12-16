<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String"></tiles:put>
	<tiles:put name="title" type="String">Preview des données de la base</tiles:put>
	<tiles:put name="connected" type="String"></tiles:put>
	<tiles:put name="body" type="String">


		<style>
			.ui-tabs .ui-tabs-hide {
				 display: none;
			}
			.ui-tabs-nav {
				display: inline-block; /* autrement les tabs prennent toute la hauteur du menu de gauche ... */
			}
		</style>

		<script>
			$(function() {
				$("#jtabs").tabs();
			});
		</script>

		<p>Les tiers suivants sont présents dans la base de données (affichage limité aux 100 premiers de chaque catégorie) :</p>

		<div id="jtabs">
			<ul>
				<c:forEach var="entry" varStatus="i" items="${command.tiersTypes}">
					<li><a href="#tabs-${i.index}"><c:out value="${entry.simpleName}"/></a></li>
				</c:forEach>
			</ul>

			<c:forEach var="entry" varStatus="i" items="${command.tiersTypes}">
				<div id="tabs-${i.index}">
					<unireg:nextRowClass reset="0"/>
					<table>
						<c:forEach items="${command.infoTiers[entry]}" var="i">
							<tr class="<unireg:nextRowClass/>" >
								<td><a href="../tiers/visu.do?id=${i.numero}"><unireg:numCTB numero="${i.numero}"/></a></td>
								<td><c:out value="${i.nomsPrenoms}"/></td>
								<td><c:out value="${i.type}"/></td>
							</tr>
						</c:forEach>
					</table>
				</div>
			</c:forEach>
		</div>
		

	</tiles:put>
</tiles:insert>