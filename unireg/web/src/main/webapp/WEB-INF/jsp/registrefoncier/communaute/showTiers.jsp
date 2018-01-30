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
		    table.rf {
			    width: 100%;
			    border: none;
			    margin: 0;
			    padding: 0 0 10px;
		    }
		    .immeubles, .membres {
			    vertical-align:top
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
		    <authz:authorize ifAnyGranted="ROLE_TESTER, ROLE_ADMIN">
		    <div style="float: right">
		        <unireg:buttonTo name="Recalculer les regroupements" action="/registrefoncier/communaute/recalculRegroupement.do?id=${tiers.ctbId}" method="post"
		        title="Recalcule les regroupements entre les communautés RF et les modèles de communauté Unireg à partir des droits de propriété du tiers. Tous les regroupements sur toutes les communautés de tous les immeubles du tiers sont recalculés."/>
		    </div>
		    </authz:authorize>
		    <display:table name="tiers.modeles" id="modele" pagesize="25" class="display" requestURI="/registrefoncier/communaute/showTiers.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			    <display:column titleKey="label.numero.modele.communaute" style="width:100px" href="showModele.do" paramId="id" paramProperty="id" >
				    <c:out value="${modele.id}"/>
			    </display:column>
			    <display:column  titleKey="label.membres.communaute" class="membres">
				    <table class="rf">
					    <tr>
						    <th></th>
						    <th><fmt:message key="label.numero.tiers"/></th>
						    <th><fmt:message key="label.nom.raison"/></th>
						    <th style="width:130px"><fmt:message key="label.date.naissance.ou.rc"/></th>
						    <th><fmt:message key="label.date.deces"/></th>
					    </tr>
					    <c:forEach items="${modele.membres}" var="membre" varStatus="loop">
						    <c:set var="noctb">
							    <unireg:numCTB numero="${membre.ctbId}"/>
						    </c:set>
						    <c:set var="nomClass">
							    <c:if test="${membre.ctbId == modele.principalCourant.principal.ctbId}">principal</c:if>
						    </c:set>
						    <tr class="${nomClass}">
							    <td style="text-align:right">${loop.index + 1}</td>
							    <td><unireg:linkTo name="${noctb}" action="/registrefoncier/communaute/showTiers.do" params="{id:${membre.ctbId}}"/></td>
							    <td><c:out value="${membre.prenom}"/> <c:out value="${membre.nom}"/></td>
							    <td><unireg:regdate regdate="${membre.dateNaissance}" /></td>
							    <td><unireg:regdate regdate="${membre.dateDeces}" /></td>
						    </tr>
					    </c:forEach>
				    </table>
			    </display:column>
			    <display:column titleKey="label.immeubles.concernes" class="immeubles">
				    <table class="rf">
					    <tr>
						    <th></th>
						    <th><fmt:message key="label.immeuble"/></th>
						    <th><fmt:message key="label.egrid"/></th>
						    <th><fmt:message key="label.date.debut"/><span class="jTip formInfo" title="<c:url value="/htm/debutRegroupementCommunaute.htm?width=375"/>" id="forPrincipalActif2">?</span></th>
						    <th><fmt:message key="label.date.fin"/><span class="jTip formInfo" title="<c:url value="/htm/finRegroupementCommunaute.htm?width=375"/>" id="forPrincipalActif2">?</span></th>
					    </tr>
					    <c:forEach items="${modele.regroupements}" var="regroupement" varStatus="loop">
						    <tr>
							    <td style="text-align:right">${loop.index + 1}</td>
							    <td><c:out value="${regroupement.immeuble.nomCommune}"/> / <c:out value="${regroupement.immeuble.noParcelle}"/></td>
							    <td><c:out value="${regroupement.immeuble.egrid}"/></td>
							    <td><unireg:regdate regdate="${regroupement.dateDebut}" /></td>
							    <td><unireg:regdate regdate="${regroupement.dateFin}" /></td>
						    </tr>
					    </c:forEach>
				    </table>
			    </display:column>
		    </display:table>

	    </fieldset>

	    <script>
		    $(function() {
			    Tooltips.activate_ajax_tooltips();
		    });
	    </script>

    </tiles:put>
</tiles:insert>