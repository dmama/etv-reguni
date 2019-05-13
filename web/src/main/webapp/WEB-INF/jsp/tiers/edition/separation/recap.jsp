<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.separation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onclick="ouvrirAide('<c:url value='/docs/creation-separation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	    <spring:bind path="separationCommand">
		    <c:if test="${not empty status.errorMessages}">
			    <table class="action_error" cellspacing="0" cellpadding="0" border="0">
				    <tr><td class="heading"><fmt:message key="label.action.problemes.detectes"/></td></tr>
				    <tr><td class="details">
					    <ul>
						    <c:forEach items="${status.errorMessages}" var="error">
							    <li class="error"><c:out value="${error}"/></li>
						    </c:forEach>
					    </ul>
				    </td></tr>
			    </table>
		    </c:if>
	    </spring:bind>

	    <unireg:bandeauTiers numero="${idMenage}" showValidation="true" showEvenementsCivils="true" showLinks="false"/>
	  	<form:form method="post" id="formRecapSeparation"  name="formRecapSeparation" modelAttribute="separationCommand" action="commit.do">
		    <form:hidden path="idMenage"/>
			<jsp:include page="rapport.jsp" />
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onclick="return confirm('Voulez-vous vraiment séparer ces deux personnes ?');" />
			<!-- Fin Boutons -->
		</form:form>
	</tiles:put>
</tiles:insert>