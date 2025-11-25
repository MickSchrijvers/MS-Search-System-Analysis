# MS-Search-Systems-Analysis

### Demo video: `https://www.youtube.com/watch?v=KFdOBwfnuew`

### Datasets: `https://drive.google.com/file/d/1Kz5glfW02aZ2qv479yK_1Lkh0uRYiIK0/view?usp=sharing`

## Benodigdheden Elasticsearch:
* De .json bestanden
  * Plaats deze in de resources folder, te vinden vanaf de repository root in `Elasticsearch/src/main/resources`
  * De files heten `http.json`, `taxi.json` en `wikipedia.json`
* Docker
* Vernieuwde environment variabelen:
  * Open een cmd en draai het Elasticsearch start-local script `curl -fsSL https://elastic.co/start-local | sh`
  * In `"C:\Users\*user*\elastic-start-local"` staat een file `.env`
  * Kopieer deze `.env` naar de directory van `Elasticsearch` in dit project


### Elasticsearch container draaien:
* Open een terminal, zorg dat je in de `Elasticsearch` staat, vanuit de repository root is dat `cd .\Elasticsearch\`
* Dan moet er een Elasticsearch-container draaien `docker compose up`
  * Deze is succesvol zodra `es-local-dev` en `kibana-local-dev` beide draaien, `kibana_settings` gaat na het opstarten uit, dat hoort zo

### Elasticsearch gebruiken:
* Vervolgens ga je via een browser naar het Kibana scherm op `http://localhost:5601/`, de gebruikersnaam is `elastic` en het wachtwoord staat in de `.env` onder de key `ES_LOCAL_PASSWORD`
* Open Elasticsearch door op de gele `Elasticsearch` card te drukken onder de tekst `Welcome home`
* Rechtsbovenin staat dan `Endpoints & API keys`
  * Klik daarna op het tabje `API key`
  * Vul iets willekeurigs in `local-test` bijvoorbeeld
  * Klik op `Create API key`
  * Kopieer de key en ga naar application.properties, te vinden vanaf de repository root in `Elasticsearch/src/main/resources/application.properties`
  * Wijzig `elasticsearch.api-key`, zodat deze de nieuwe key gebruikt
* Nu kun je de `ElasticsearchApplication` opstarten via je een IDE. 
* Als deze opgestart is, kun je naar de Swagger-ui op `http://localhost:8080/swagger-ui/index.html`


## Benodigdheden Vespa.ai:
* De .json bestanden
  * Plaats deze in de resources folder, te vinden vanaf de repository root in `Vespa-config/src/main/resources`
  * De files heten `http.json`, `taxi.json` en `wikipedia.json`
* De Vespa CLI, te downloaden op `https://github.com/vespa-engine/vespa/releases`
* Docker of podman

### Vespa container draaien:
* De dependencies in de pom.xml moeten gecomment zijn. 
* De methode aanroepen in de main klasse moeten gecomment zijn. 
* Zorg dat je terminal in de `Vespa-config` directory staat, vanuit de repository root is dat `cd .\Vespa-config\` 
* Build de application package met `mvn install`
* Dan moet er een Vespa-container draaien `docker compose up`
* Daarna deploy je de application package naar de draaiende container met `vespa deploy --wait 300`
* Als dit allemaal gelukt is draait de application package in de Vespa container


#### HTTP dataset 
* In `Vespa-config/src/main/java/Main.java` staat een methode waarmee je van reguliere .json het vespa-formaat krijgt voor de http dataset. Hiervoor moet ook in de pom.xml een dependency actief gemaakt worden.
* Open een terminal in de repository root
* Gebruik dit commando om de http data te indexeren:
`java -jar .\tools\vespa-feed-client-cli\vespa-feed-client-cli-jar-with-dependencies.jar --endpoint http://localhost:8080/ --file Vespa-config/src/main/resources/http-vespa-format.json --connections 4`
* Als de http data succesvol geïndexeerd is, kun je een query uitvoeren met de vespa cli, bijvoorbeeld: `vespa query "select * from http where clientip contains '40.135.0.0'"`, dit zou dan een resultaat geven met status 200.

#### TAXI dataset
* In `Vespa-config/src/main/java/Main.java` staat een methode waarmee je van reguliere .json het vespa-formaat krijgt voor de taxi dataset. Hiervoor moet ook in de pom.xml een dependency actief gemaakt worden.
* Open een terminal in de repository root
* Gebruik dit commando om de taxi data te indexeren:
  `java -jar .\tools\vespa-feed-client-cli\vespa-feed-client-cli-jar-with-dependencies.jar --endpoint http://localhost:8080/ --file Vespa-config/src/main/resources/taxi-vespa-format.json --connections 4`
* Als de taxi data succesvol geïndexeerd is, kun je een query uitvoeren met de vespa cli, bijvoorbeeld: `vespa query "select * from taxi where pickup_location contains '142'"`, dit zou dan een resultaat geven met status 200.
