<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
		<fmt:message key="title.selection.principal.affichage"/>
  	</tiles:put>
  	<tiles:put name="head">
	    <style type="text/css">
		    .principal {
			    font-weight: bold;
		    }
	    </style>
  	</tiles:put>
  	<tiles:put name="body">

	    <c:set var="titre"><fmt:message key="label.caracteristiques.proprietaire"/></c:set>

	    <%--@elvariable id="tiers" type="ch.vd.uniregctb.registrefoncier.communaute.TiersWithCommunauteView"--%>
	    <unireg:bandeauTiers numero="${tiers.ctbId}" titre="${titre}"
	                         showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

	    <fieldset>
	    <legend><span><fmt:message key="label.communautes.associees" /></span></legend>
		    <display:table name="tiers.modeles" id="modele" pagesize="25" class="display">
			    <display:column titleKey="label.numero.modele.communaute" href="showModele.do" paramId="id" paramProperty="id" >
				    <c:out value="${modele.id}"/>
			    </display:column>
			    <display:column  titleKey="label.membres.communaute">
				    <ol>
					    <c:forEach items="${modele.membres}" var="membre">
						    <c:set var="noctb">
							    <unireg:numCTB numero="${membre.ctbId}"/>
						    </c:set>
						    <c:set var="nomClass">
						        <c:if test="${membre.ctbId == modele.principalCourant.principal.ctbId}">principal</c:if>
						    </c:set>
						    <li>
							    <span class="${nomClass}"><c:out value="${membre.prenom}"/> <c:out value="${membre.nom}"/></span>
							    <c:if test="${membre.dateNaissance != null}">
								    (<unireg:regdate regdate="${membre.dateNaissance}" /><c:if test="${membre.dateDeces != null}">- <unireg:regdate regdate="${membre.dateDeces}" /></c:if>)
							    </c:if>
							    - <unireg:linkTo name="${noctb}" action="/registrefoncier/communaute/showTiers.do" params="{id:${membre.ctbId}}"/>
						    </li>
					    </c:forEach>
				    </ol>
			    </display:column>
			    <display:column titleKey="label.immeubles.concernes" >
				    <ol>
					    <c:forEach items="${modele.regroupements}" var="regroupement">
						    <li>
							    <c:out value="${regroupement.immeuble.nomCommune}"/> / <c:out value="${regroupement.immeuble.noParcelle}"/>
							    <c:if test="${regroupement.dateDebut != null}">
								    (<unireg:regdate regdate="${regroupement.dateDebut}" /><c:if test="${regroupement.dateFin != null}">- <unireg:regdate regdate="${regroupement.dateFin}" /></c:if>)
							    </c:if>
							    - <c:out value="${regroupement.immeuble.egrid}"/>
						    </li>
					    </c:forEach>
				    </ol>
			    </display:column>
		    </display:table>

	    </fieldset>

	</tiles:put>
</tiles:insert>