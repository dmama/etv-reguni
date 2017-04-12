<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="resultsHidingCause" type="ch.vd.uniregctb.lr.view.SearchResultsHidingCause"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.lr" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/listes-recapitulatives.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheLR" commandName="critereRechercheListesRecapitulatives" action="list.do">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>
	    
	    <c:choose>
		    <c:when test="${resultsHidingCause != null}">
			    <span style="font-style: italic;"><fmt:message key="${resultsHidingCause.messageKey}"/></span>
		    </c:when>
		    <c:otherwise>

			    <display:table name="lrs" id="lr" pagesize="25" requestURI="/lr/list.do" class="display_table" sort="external" partialList="true" size="resultSize" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
				    <display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.lr.trouvee" /></span></display:setProperty>
				    <display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.lr.trouvee" /></span></display:setProperty>
				    <display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>
				    <display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.lrs.trouvees" /></span></display:setProperty>

				    <display:column sortable="true" titleKey="label.numero.debiteur" sortProperty="numero" sortName="tiers.numero" >
					    <c:choose>
						    <c:when test="${!lr.annule}">
							    <a href="../tiers/visu.do?id=${lr.idDebiteur}"><unireg:numCTB numero="${lr.idDebiteur}"/></a>
						    </c:when>
						    <c:otherwise>
							    <unireg:numCTB numero="${lr.idDebiteur}" />
						    </c:otherwise>
					    </c:choose>
				    </display:column>
				    <display:column titleKey="label.nom.raison">
					    <unireg:multiline lines="${lr.nomCourrier}"/>
				    </display:column>
				    <display:column sortable ="true" titleKey="label.debiteur.is" sortName="categorieImpotSource">
					    <fmt:message key="option.categorie.impot.source.${lr.categorieImpotSource}" />
				    </display:column>
				    <display:column sortable ="true" titleKey="label.mode.communication" sortName="modeCommunication">
					    <fmt:message key="option.mode.communication.${lr.modeCommunication}" />
				    </display:column>
				    <display:column sortable ="true" titleKey="label.periode" sortName="dateDebut">
					    <unireg:regdate regdate="${lr.dateDebutPeriode}"/>&nbsp;-&nbsp;<unireg:regdate regdate="${lr.dateFinPeriode}"/>
				    </display:column>
				    <display:column titleKey="label.date.retour">
					    <unireg:regdate regdate="${lr.dateRetour}"/>
				    </display:column>
				    <display:column titleKey="label.date.delai.accorde">
					    <unireg:regdate regdate="${lr.delaiAccorde}"/>
				    </display:column>
				    <display:column titleKey="label.etat.avancement" >
					    <fmt:message key="option.etat.avancement.${lr.etat}" />
				    </display:column>
				    <display:column>
					    <c:if test="${!lr.annule}">
						    <unireg:raccourciModifier link="edit-lr.do?id=${lr.idListe}" tooltip="LR"/>
					    </c:if>
				    </display:column>
			    </display:table>

		    </c:otherwise>
	    </c:choose>

	</tiles:put>
</tiles:insert>
