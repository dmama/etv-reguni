<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.uniregctb.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.mandataires.liens"/></span></legend>

	<c:if test="${not empty command.liensMandataires}">
		<display:table name="${command.liensMandataires}" id="lien" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable ="true" titleKey="label.date.debut" sortProperty="regDateDebut">
				<unireg:regdate regdate="${lien.regDateDebut}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.fin" sortProperty="regDateFin">
				<unireg:regdate regdate="${lien.regDateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<fmt:message key="option.mandat.type.${lien.typeMandat}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.numero.contribuable" sortProperty="tiersLie.numero">
				<unireg:numCTB numero="${lien.numero}" link="true"/>
			</display:column>
			<display:column titleKey="label.nom.raison">
				<unireg:multiline lines="${lien.nomCourrier}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.complement.numeroIBAN">
				${lien.iban}
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="RapportEntreTiers" entityId="${lien.id}"/>
			</display:column>
		</display:table>
	</c:if>

</fieldset>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.mandataires.adresses"/></span></legend>

	<c:if test="${not empty command.adressesMandataires}">
		<display:table name="${command.adressesMandataires}" id="adresseMandataire" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<unireg:regdate regdate="${adresseMandataire.dateDebut}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${adresseMandataire.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type">
				<fmt:message key="option.mandat.type.${adresseMandataire.typeMandat}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.nom.raison">
				${adresseMandataire.nomDestinataire}
			</display:column>
			<display:column sortable="true" titleKey="label.adresse.complement">
				<c:out value="${adresseMandataire.complements}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.rueCasePostale">
				<c:out value="${adresseMandataire.rue}"/>
				<c:if test="${not empty adresseMandataire.formattedCasePostale}">
					<br/><c:out value="${adresseMandataire.formattedCasePostale}"/>
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.localite" >
				<c:out value="${adresseMandataire.localite}"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.pays" >
				<c:if test="${adresseMandataire.paysOFS != null }">
					<unireg:pays ofs="${adresseMandataire.paysOFS}" displayProperty="nomCourt" date="${adresseMandataire.dateDebut}"/>
				</c:if>
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="AdresseMandataire" entityId="${adresseMandataire.id}"/>
			</display:column>
		</display:table>
	</c:if>

</fieldset>
