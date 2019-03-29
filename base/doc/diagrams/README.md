Ce dossier contient les fichiers utilisés pour générer les schémas d'architecture 
d'Unireg visibles dans le Wiki [ici](https://portail.etat-de-vaud.ch/outils/dsiwiki/x/X50bAQ).

#### Très courte introduction à C4

[C4](https://c4model.com) est un système de modélisation pour les composants logiciels.

C4 propose d'utiliser 4 niveaux de diagrammes pour décrire une application :
 1. Le niveau sytème (= applications)
 2. Le niveau container (= unités de déploiement)
 3. Le niveau des composants (= modules, beans, ...)
 4. Code
 
 L'idée est de fournir plusieurs niveaux de détails et de laisser le lecteur choisir celui qui lui convient.
 
 Plus de détails sur [le site de C4](https://c4model.com).
 

#### Installation de l'environnement PlantUML + C4
	
 - installer Graphviz : https://www.graphviz.org/download/
 - assurez-vous que l'exécutable dot soit bien dans le PATH :
``` 
$ export PATH=~/graphviz-2.38/release/bin:$PATH
```
 - installer PlantUML : http://plantuml.com/download. Il s'agit seulement du fichier plantuml.jar à placer dans le répertoire courant.
 
**Note:** il existe un plugin PlantUML pour IntelliJ IDEA qui fonctionne plutôt bien.
  
#### Compilation d'un diagramme

``` 
$ java -jar plantuml.jar -charset UTF-8 01_unireg_system_context.puml
```
