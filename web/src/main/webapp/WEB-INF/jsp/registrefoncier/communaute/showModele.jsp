<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
		<fmt:message key="title.selection.principal.communaute"/>
  	</tiles:put>
  	<tiles:put name="head">
	    <style type="text/css">
		    .communityLeader {
			    font-weight: bold;
		    }
		    .nonRapproche {
			    font-style: italic;
		    }
	    </style>
  	</tiles:put>
  	<tiles:put name="body">

	    <%--@elvariable id="modele" type="ch.vd.unireg.registrefoncier.communaute.ModeleCommunauteView"--%>
	    <fieldset class="information">
		    <legend><span><fmt:message key="label.caracteristiques.modele.communaute" /></span></legend>
		    <table cellspacing="0" cellpadding="5" border="0" class="display_table">
			    <tr class="odd">
				    <td width="15%" nowrap="">N° du modèle de communauté&nbsp;:&nbsp;</td>
				    <td width="50%">${modele.id}</td>
				    <td width="35%">&nbsp;</td>
			    </tr>
		    </table>
	    </fieldset>

	    <fieldset>
	    <legend><span><fmt:message key="label.membres.communaute" /></span></legend>
		    <display:table name="modele.membres" id="membre" pagesize="25" class="display" requestURI="/registrefoncier/communaute/showModele.do" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			    <c:set var="classNom"><c:if test="${membre.ctbId == modele.principalCourant.principal.ctbId}">communityLeader</c:if><c:if test="${membre.ctbId == null}">nonRapproche</c:if></c:set>
			    <display:column style="width:10px">
				    <c:if test="${membre.ctbId == modele.principalCourant.principal.ctbId}">
					    <img src="<c:url value="/css/x/checkmark.png"/>" title="Ce membre est le principal courant" />
				    </c:if>
			    </display:column>
			    <display:column titleKey="label.numero.contribuable">
				    <c:set var="noctb">
					    <unireg:numCTB numero="${membre.ctbId}"/>
				    </c:set>
				    <unireg:linkTo link_class="${classNom}" name="${noctb}" action="/registrefoncier/communaute/showTiers.do" params="{id:${membre.ctbId}}"/>
			    </display:column>
			    <display:column titleKey="label.role">
				    <span class="${classNom}"><c:out value="${membre.role}"/></span>
			    </display:column>
			    <display:column titleKey="label.nom.raison">
				    <span class="${classNom}"><c:out value="${membre.nom}"/></span>
			    </display:column>
			    <display:column titleKey="label.prenoms">
				    <span class="${classNom}"><c:out value="${membre.prenom}"/></span>
			    </display:column>
			    <display:column  titleKey="label.date.naissance.ou.rc">
		            <span class="${classNom}"><unireg:regdate regdate="${membre.dateNaissance}" /></span>
			    </display:column>
			    <display:column  titleKey="label.date.deces">
				    <span class="${classNom}"><unireg:regdate regdate="${membre.dateDeces}" /></span>
			    </display:column>
			    <display:column  titleKey="label.for.principal">
				    <span class="${classNom}"><c:out value="${membre.forPrincipal}"/></span>
			    </display:column>
			    <display:column  titleKey="label.action">
				    <c:if test="${membre.id != null && membre.ctbId != null && membre.ctbId != modele.principalCourant.principal.ctbId}">
				        <unireg:buttonTo name="Choisir comme principal" action="/registrefoncier/communaute/addPrincipal.do" method="get" params="{modeleId:${modele.id},membreId:${membre.id}}"/>
				    </c:if>
				    <c:if test="${membre.ctbId == null}">
					    <span class="${classNom}"><fmt:message key="label.tiers.non-rapproche"/></span>
				    </c:if>
			    </display:column>
		    </display:table>
	    </fieldset>

	    <fieldset>
	    <legend><span><fmt:message key="label.histo.principaux.communaute" /></span></legend>
		    <display:table name="modele.principaux" id="principal" pagesize="25" class="display" requestURI="/registrefoncier/communaute/showModele.do" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			    <display:column titleKey="label.par.defaut">
				    <input type="checkbox" disabled="disabled" <c:if test="${principal.parDefaut}">checked</c:if>/>
			    </display:column>
			    <display:column titleKey="label.date.debut">
				    <unireg:regdate regdate="${principal.dateDebut}" />
			    </display:column>
			    <display:column titleKey="label.date.fin">
				    <unireg:regdate regdate="${principal.dateFin}" />
			    </display:column>
			    <display:column titleKey="label.numero.contribuable">
				    <c:set var="noctb">
					    <unireg:numCTB numero="${principal.principal.ctbId}"/>
				    </c:set>
				    <unireg:linkTo name="${noctb}" action="/registrefoncier/communaute/showTiers.do" params="{id:${principal.principal.ctbId}}"/>
			    </display:column>
			    <display:column titleKey="label.nom.raison">
				    <c:out value="${principal.principal.nom}"/>
			    </display:column>
			    <display:column titleKey="label.prenoms">
				    <c:out value="${principal.principal.prenom}"/>
			    </display:column>
			    <display:column  titleKey="label.action">
				    <c:if test="${!principal.parDefaut && !principal.annule}">
					    <unireg:linkTo name="" action="/registrefoncier/communaute/cancelPrincipal.do" method="POST" params="{id:${principal.id}}"
					                   link_class="delete" title="Annulation du principal" confirm="Voulez-vous vraiment annuler ce principal ?"/>
				    </c:if>
				    <c:if test="${!principal.parDefaut}">
				        <unireg:consulterLog entityNature="PrincipalCommunauteRF" entityId="${principal.id}"/>
				    </c:if>
			    </display:column>
		    </display:table>
	    </fieldset>

	    <fieldset>
	    <legend><span><fmt:message key="label.immeubles.concernes" /></span></legend>
		    <display:table name="modele.regroupements" id="regroupement" pagesize="25" class="display" requestURI="/registrefoncier/communaute/showModele.do" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			    <display:column titleKey="label.date.debut">
				    <unireg:regdate regdate="${regroupement.dateDebut}" />
			    </display:column>
			    <display:column titleKey="label.date.fin">
				    <unireg:regdate regdate="${regroupement.dateFin}" />
			    </display:column>
			    <display:column titleKey="label.commune">
				    <c:out value="${regroupement.immeuble.nomCommune}"/>
			    </display:column>
			    <display:column  titleKey="label.parcelle">
				    <c:out value="${regroupement.immeuble.noParcelle}"/>
			    </display:column>
			    <display:column  titleKey="label.egrid">
				    <c:out value="${regroupement.immeuble.egrid}"/>
			    </display:column>
		    </display:table>
			<input type="button" name="retourPersonneCommunaute" value="<fmt:message key="label.bouton.retour" />" onclick="Navigation.backTo(['/registrefoncier/communaute/showTiers.do'], '/rapports-prestation/edit.do', 'id=${membre.ctbId}');" />
	    </fieldset>

	</tiles:put>
</tiles:insert>