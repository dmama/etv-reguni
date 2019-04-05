<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateLight.jsp">
	<tiles:put name="title" type="String">Veuillez sélectionner un OID de travail</tiles:put>
	<tiles:put name="body" type="String">
		<%--@elvariable id="command" type="ch.vd.unireg.security.ChooseOIDView"--%>
		<table id="choixCollectivites" style="padding: 40px">
			<c:forEach items="${command.officesImpot}" var="oid" varStatus="counter">
				<tr style="text-align:center; height:24px">
					<td>
						<unireg:linkTo name="${oid.nomCourt}" title="${oid.nomCourt}"
						               action="/chooseOID.do" method="post"
						               params="{selectedOID:${oid.noColAdm}, initialUrl:'${command.initialUrl}'}"/>
					</td>
				</tr>
			</c:forEach>
		</table>
	</tiles:put>
</tiles:insert>
