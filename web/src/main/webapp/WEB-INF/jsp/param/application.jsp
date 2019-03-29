<%@ taglib prefix="sf" uri="http://www.springframework.org/tags/form" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.param.application" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/parametrage-application.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="head">

  		<style type="text/css">
  			th.th1 {
  				width: 55%;
  			}
  			th.th2 {
  				width: 10%;
  			}
  			th.th3 {
  				width: 10%;
  			}
  			th.th4 {
  				width: 25%;
  			}
  			tr.separator {
  				height: 3px;
  			}
  			input.valeur {
  				width: 35px;
  				text-align: right;
  			}
  			input.valeur-longue {
  				width: 80px;
  				text-align: right;
  			}
  			.nombre, .date, td.valeur {
  				text-align: right;
  			}
			td {
				white-space: nowrap;
			}
  		</style>

	  	<script type="text/javascript">
		 $(document).ready(function() {
			 /*
			  * Event Handlers
			  */
				$("#save").click( function() {
					$('form').attr("action", "save.do")
				});
				$("#reset").click( function() {
					$('form').attr("action", "reset.do")
				});
		 });
		</script>
	</tiles:put>
  	<tiles:put name="body">
 		<sf:form method="post" modelAttribute="params">
		    <sf:input type="hidden" path="delaiImpressionCadev"/>
		    <sf:input type="hidden" path="delaiRetourQuestionnaireSNCRappele"/>
		    <sf:input type="hidden" path="delaiAccordeDemandeDelai"/>
 			<table>
 				<thead>
	                <tr>
	                    <th class="th1"><fmt:message key="label.param.entete.parametre"/></th>
	                    <th class="th3"><fmt:message key="label.param.entete.defaut"/></th>
	                    <th class="th2"><fmt:message key="label.param.entete.valeur"/></th>
						<th class="th4">&nbsp;</th>
	                </tr>
 				</thead>
			    <unireg:nextRowClass reset="0"/>
				<tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.premierePeriodeFiscale.pp"/></td>
		            <td class="nombre">NA</td>
		            <td class="valeur">
		                <sf:input path="premierePeriodeFiscalePersonnesPhysiques" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4" readonly="true"/>
		            </td>
					<td><sf:errors path="premierePeriodeFiscalePersonnesPhysiques" cssClass="erreur"/></td>
		        </tr>
				<tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.premierePeriodeFiscale.pm"/></td>
		            <td class="nombre">NA</td>
		            <td class="valeur">
		                <sf:input path="premierePeriodeFiscalePersonnesMorales" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4" readonly="true"/>
		            </td>
					<td><sf:errors path="premierePeriodeFiscalePersonnesMorales" cssClass="erreur"/></td>
		        </tr>
				<tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.premierePeriodeFiscaleDeclaration.pm"/></td>
		            <td class="nombre">NA</td>
		            <td class="valeur">
		                <sf:input path="premierePeriodeFiscaleDeclarationPersonnesMorales" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4" readonly="true"/>
		            </td>
					<td><sf:errors path="premierePeriodeFiscaleDeclarationPersonnesMorales" cssClass="erreur"/></td>
		        </tr>

		        <tr class="separator"><th colspan="4" ></th></tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.noel"/></td>
					<td class="date">NA</td>
		            <td class="valeur">
		                <sf:input path="noel" cssClass="valeur" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
		            </td>
					<td><sf:errors path="noel" cssClass="erreur"/></td> </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.nouvelAn"/></td>
		            <td class="date">NA</td>
		            <td class="valeur">
		                <sf:input path="nouvelAn" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
		            </td>
					<td><sf:errors path="nouvelAn" cssClass="erreur"/></td>
				</tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.lendemainNouvelAn"/></td>
					<td class="date">NA</td>
		            <td class="valeur">
		                <sf:input path="lendemainNouvelAn" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
		            </td>
					<td><sf:errors path="lendemainNouvelAn" cssClass="erreur"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.feteNationale"/></td>
				    <td class="date">NA</td>
		            <td class="valeur">
		                <sf:input path="feteNationale" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5" readonly="true"/>
		            </td>
					<td><sf:errors path="feteNationale" cssClass="erreur"/></td>
				</tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.nbMaxParListe"/></td>
					<%--@elvariable id="nbMaxParListeParDefaut" type="java.lang.String"--%>
		            <td class="nombre">${nbMaxParListeParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="nbMaxParListe" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
		            </td>
					<td><sf:errors path="nbMaxParListe" cssClass="erreur"/></td>
				</tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.nbMaxParPage"/></td>
					<%--@elvariable id="nbMaxParPageParDefaut" type="java.lang.String"--%>
		            <td class="nombre">${nbMaxParPageParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="nbMaxParPage" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="nbMaxParPage" cssClass="erreur"/></td>
				</tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiAttenteDeclarationImpotPersonneDecedee"/></td>
					<%--@elvariable id="delaiAttenteDeclarationImpotPersonneDecedeeParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiAttenteDeclarationImpotPersonneDecedeeParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiAttenteDeclarationImpotPersonneDecedee" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiAttenteDeclarationImpotPersonneDecedee" cssClass="erreur"/></td>
				</tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiRetourDeclarationImpotEmiseManuellement.pp"/></td>
		            <%--@elvariable id="delaiRetourDeclarationImpotPPEmiseManuellementParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiRetourDeclarationImpotPPEmiseManuellementParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiRetourDeclarationImpotPPEmiseManuellement" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiRetourDeclarationImpotPPEmiseManuellement" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiRetourDeclarationImpotEmiseManuellement.pm"/></td>
		            <%--@elvariable id="delaiRetourDeclarationImpotPMEmiseManuellementParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiRetourDeclarationImpotPMEmiseManuellementParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiRetourDeclarationImpotPMEmiseManuellement" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiRetourDeclarationImpotPMEmiseManuellement" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiCadevImpressionDeclarationImpot"/></td>
		            <%--@elvariable id="delaiCadevImpressionDeclarationImpotParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiCadevImpressionDeclarationImpotParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiCadevImpressionDeclarationImpot" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiCadevImpressionDeclarationImpot" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEnvoiSommationDeclarationImpot.pp"/></td>
					<%--@elvariable id="delaiEnvoiSommationDeclarationImpotPPParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEnvoiSommationDeclarationImpotPPParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEnvoiSommationDeclarationImpotPP" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiEnvoiSommationDeclarationImpotPP" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEnvoiSommationDeclarationImpot.pm"/></td>
					<%--@elvariable id="delaiEnvoiSommationDeclarationImpotPMParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEnvoiSommationDeclarationImpotPMParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEnvoiSommationDeclarationImpotPM" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiEnvoiSommationDeclarationImpotPM" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEcheanceSommationDeclarationImpot.pp"/></td>
					<%--@elvariable id="delaiEcheanceSommationDeclarationImpotPPParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEcheanceSommationDeclarationImpotPPParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEcheanceSommationDeclarationImpotPP" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiEcheanceSommationDeclarationImpotPP" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEcheanceSommationDeclarationImpot.pm"/></td>
					<%--@elvariable id="delaiEcheanceSommationDeclarationImpotPMParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEcheanceSommationDeclarationImpotPMParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEcheanceSommationDeclarationImpotPM" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiEcheanceSommationDeclarationImpotPM" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.jourDuMoisEnvoiListesRecapitulatives"/></td>
					<%--@elvariable id="jourDuMoisEnvoiListesRecapitulativesParDefaut" type="java.lang.String"--%>
					<td class="nombre">${jourDuMoisEnvoiListesRecapitulativesParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="jourDuMoisEnvoiListesRecapitulatives" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="2"/>
		            </td>
					<td><sf:errors path="jourDuMoisEnvoiListesRecapitulatives" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiCadevImpressionListesRecapitulatives"/></td>
		            <%--@elvariable id="delaiCadevImpressionListesRecapitulativesParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiCadevImpressionListesRecapitulativesParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiCadevImpressionListesRecapitulatives" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiCadevImpressionListesRecapitulatives" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiRetourListeRecapitulative"/></td>
		            <%--@elvariable id="delaiRetourListeRecapitulativeParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiRetourListeRecapitulativeParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiRetourListeRecapitulative" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiRetourListeRecapitulative" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEnvoiSommationListeRecapitulative"/></td>
					<%--@elvariable id="delaiEnvoiSommationListeRecapitulativeParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEnvoiSommationListeRecapitulativeParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEnvoiSommationListeRecapitulative" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiEnvoiSommationListeRecapitulative" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiRetourSommationListeRecapitulative"/></td>
		            <%--@elvariable id="delaiRetourSommationListeRecapitulativeParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiRetourSommationListeRecapitulativeParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiRetourSommationListeRecapitulative" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiRetourSommationListeRecapitulative" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEcheanceSommationListeRecapitualtive"/></td>
		            <%--@elvariable id="delaiEcheanceSommationListeRecapitualtiveParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEcheanceSommationListeRecapitualtiveParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEcheanceSommationListeRecapitualtive" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiEcheanceSommationListeRecapitualtive" cssClass="erreur"/></td>
		        </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiRetentionRapportTravailInactif"/></td>
		            <%--@elvariable id="delaiRetentionRapportTravailInactifParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiRetentionRapportTravailInactifParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiRetentionRapportTravailInactif" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiRetentionRapportTravailInactif" cssClass="erreur"/></td>
			    </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiMinimalRetourDeclarationImpotPM"/></td>
		            <%--@elvariable id="delaiMinimalRetourDeclarationImpotPMParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiMinimalRetourDeclarationImpotPMParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiMinimalRetourDeclarationImpotPM" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiMinimalRetourDeclarationImpotPM" cssClass="erreur"/></td>
		        </tr>
			    <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.dateLimiteEnvoiMasseDeclarationsUtilitePublique"/></td>
		            <%--@elvariable id="dateLimiteEnvoiMasseDeclarationsUtilitePubliqueParDefaut" type="java.lang.String"--%>
					<td class="nombre">${dateLimiteEnvoiMasseDeclarationsUtilitePubliqueParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="dateLimiteEnvoiMasseDeclarationsUtilitePublique" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5"/>
		            </td>
				    <td><sf:errors path="dateLimiteEnvoiMasseDeclarationsUtilitePublique" cssClass="erreur"/></td>
		        </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.delaiRetourQuestionnaireSNCEmisManuellement"/></td>
					    <%--@elvariable id="delaiRetourQuestionnaireSNCEmisManuellementParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${delaiRetourQuestionnaireSNCEmisManuellementParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="delaiRetourQuestionnaireSNCEmisManuellement" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
				    </td>
				    <td><sf:errors path="delaiRetourQuestionnaireSNCEmisManuellement" cssClass="erreur"/></td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.delaiCadevImpressionQuestionnaireSNC"/></td>
					    <%--@elvariable id="delaiCadevImpressionQuestionnaireSNCParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${delaiCadevImpressionQuestionnaireSNCParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="delaiCadevImpressionQuestionnaireSNC" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
				    </td>
				    <td><sf:errors path="delaiCadevImpressionQuestionnaireSNC" cssClass="erreur"/></td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.delaiEnvoiRappelQuestionnaireSNC"/></td>
					    <%--@elvariable id="delaiEnvoiRappelQuestionnaireSNCParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${delaiEnvoiRappelQuestionnaireSNCParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="delaiEnvoiRappelQuestionnaireSNC" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
				    </td>
				    <td><sf:errors path="delaiEnvoiRappelQuestionnaireSNC" cssClass="erreur"/></td>
			    </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.dateDebutEnvoiLettresBienvenue"/></td>
		            <%--@elvariable id="dateDebutEnvoiLettresBienvenueParDefaut" type="java.lang.String"--%>
					<td class="date">${dateDebutEnvoiLettresBienvenueParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="dateDebutEnvoiLettresBienvenue" cssClass="valeur-longue date" cssErrorClass="valeur-longue input-with-errors date" maxlength="10"/>
		            </td>
					<td><sf:errors path="dateDebutEnvoiLettresBienvenue" cssClass="erreur"/></td>
			    </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiRetourLettreBienvenue"/></td>
		            <%--@elvariable id="delaiRetourLettreBienvenueParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiRetourLettreBienvenueParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiRetourLettreBienvenue" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiRetourLettreBienvenue" cssClass="erreur"/></td>
			    </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiCadevImpressionLettreBienvenue"/></td>
		            <%--@elvariable id="delaiCadevImpressionLettreBienvenueParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiCadevImpressionLettreBienvenueParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiCadevImpressionLettreBienvenue" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
		            </td>
					<td><sf:errors path="delaiCadevImpressionLettreBienvenue" cssClass="erreur"/></td>
			    </tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.tailleTrouAssujettissementPourNouvelleLettreBienvenue"/></td>
		            <%--@elvariable id="tailleTrouAssujettissementPourNouvelleLettreBienvenueParDefaut" type="java.lang.String"--%>
					<td class="nombre">${tailleTrouAssujettissementPourNouvelleLettreBienvenueParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="tailleTrouAssujettissementPourNouvelleLettreBienvenue" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
		            </td>
					<td><sf:errors path="tailleTrouAssujettissementPourNouvelleLettreBienvenue" cssClass="erreur"/></td>
		        </tr>
			    <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.delaiEnvoiRappelLettreBienvenue"/></td>
		            <%--@elvariable id="delaiEnvoiRappelLettreBienvenueParDefaut" type="java.lang.String"--%>
					<td class="nombre">${delaiEnvoiRappelLettreBienvenueParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="delaiEnvoiRappelLettreBienvenue" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
		            </td>
					<td><sf:errors path="delaiEnvoiRappelLettreBienvenue" cssClass="erreur"/></td>
		        </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI"/></td>
					    <%--@elvariable id="dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICIParDefaut" type="java.lang.String"--%>
				    <td class="date">${dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICIParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI" cssClass="valeur-longue date" cssErrorClass="valeur-longue input-with-errors date" maxlength="10"/>
				    </td>
				    <td><sf:errors path="dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI" cssClass="erreur"/></td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.delaiRetourDemandeDegrevementICI"/></td>
					    <%--@elvariable id="delaiRetourDemandeDegrevementICIParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${delaiRetourDemandeDegrevementICIParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="delaiRetourDemandeDegrevementICI" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
				    </td>
				    <td><sf:errors path="delaiRetourDemandeDegrevementICI" cssClass="erreur"/></td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.delaiEnvoiRappelDemandeDegrevementICI"/></td>
					    <%--@elvariable id="delaiEnvoiRappelDemandeDegrevementICIParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${delaiEnvoiRappelDemandeDegrevementICIParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="delaiEnvoiRappelDemandeDegrevementICI" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="3"/>
				    </td>
				    <td><sf:errors path="delaiEnvoiRappelDemandeDegrevementICI" cssClass="erreur"/></td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.delaiCadevImpressionDemandeDegrevementICI"/></td>
					    <%--@elvariable id="delaiCadevImpressionDemandeDegrevementICIParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${delaiCadevImpressionDemandeDegrevementICIParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="delaiCadevImpressionDemandeDegrevementICI" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
				    </td>
				    <td><sf:errors path="delaiCadevImpressionDemandeDegrevementICI" cssClass="erreur"/></td>
			    </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.dateExclusionDecedeEnvoiDI"/></td>
		            <%--@elvariable id="dateExclusionDecedeEnvoiDIParDefaut" type="java.lang.String"--%>
					<td class="date">${dateExclusionDecedeEnvoiDIParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="dateExclusionDecedeEnvoiDI" cssClass="valeur date" cssErrorClass="valeur input-with-errors date" maxlength="5"/>
		            </td>
			        <td><sf:errors path="dateExclusionDecedeEnvoiDI" cssClass="erreur"/></td>
		        </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
		        <tr class="<unireg:nextRowClass/>">
		            <td><fmt:message key="label.param.anneeMinimaleForDebiteur"/></td>
		            <%--@elvariable id="anneeMinimaleForDebiteurParDefaut" type="java.lang.String"--%>
					<td class="nombre">${anneeMinimaleForDebiteurParDefaut}</td>
		            <td class="valeur">
		                <sf:input path="anneeMinimaleForDebiteur" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="4"/>
		            </td>
					<td><sf:errors path="anneeMinimaleForDebiteur" cssClass="erreur"/></td>
		        </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.ageRentierHomme"/></td>
					    <%--@elvariable id="ageRentierHommeParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${ageRentierHommeParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="ageRentierHomme" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="2"/>
				    </td>
				    <td><sf:errors path="ageRentierHomme" cssClass="erreur"/> </td>
			    </tr>
			    <tr class="<unireg:nextRowClass/>">
				    <td><fmt:message key="label.param.ageRentierFemme"/></td>
					    <%--@elvariable id="ageRentierFemmeParDefaut" type="java.lang.String"--%>
				    <td class="nombre">${ageRentierFemmeParDefaut}</td>
				    <td class="valeur">
					    <sf:input path="ageRentierFemme" cssClass="valeur nombre" cssErrorClass="valeur input-with-errors nombre" maxlength="2"/>
				    </td>
				    <td><sf:errors path="ageRentierFemme" cssClass="erreur"/></td>
			    </tr>

			    <tr class="separator"><th colspan="4" ></th></tr>
 			</table>
 			<input type="submit" id="save" value="<fmt:message key="label.param.action.save"/>"/>
 			<input type="submit" id="reset" value="<fmt:message key="label.param.action.reset"/>" />
 		</sf:form>
 	</tiles:put>
</tiles:insert>