# Movie Event Monitor

Sistema que acompanha filmes da TMDB usando Apache Kafka. O producer publica atualizações de filmes; consumers identificam avaliações e popularidade altas, novos filmes e tendências. O dashboard mostra os dados e oferece a aba **Recomendados**, com sugestões baseadas nos gêneros dos filmes curtidos.

## Como funciona

O `MovieProducer` consulta a TMDB e envia eventos `MOVIE_UPDATE` com título, avaliação, votos, popularidade e gêneros. Os consumers processam esses eventos em paralelo. O `EventAnalyzer` identifica tendências e publica eventos derivados. O `DashboardServer` consome os eventos e serve a interface web.

Na aba **Recomendados**, os likes ficam no `localStorage` do navegador, sem conta ou cadastro. Cada gênero em comum vale 1 ponto e filmes marcados como tendência recebem 1 ponto extra. Filmes curtidos são excluídos; sem likes, a aba mostra filmes em tendência. As sugestões usam apenas os filmes acompanhados pelo dashboard.

## Requisitos

Java 17+, Maven e Docker Compose. Para consultar a TMDB, é necessária uma chave de API.

## Como rodar

Na raiz do projeto, crie `.env` e adicione sua chave da TMDB:

```bash
cp .env.example .env
```

Inicie o Kafka e crie os tópicos:

```bash
docker compose up -d
sh scripts/create-topics.sh
```

Compile e execute os testes Java:

```bash
mvn clean test
```

Abra um terminal para cada processo Java. Em cada terminal, carregue as variáveis do `.env` antes de executar o componente:

```bash
set -a
source .env
set +a
```

Inicie os consumers, o analyzer e o dashboard:

```bash
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.RatingConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.PopularityConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.ReleaseConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.EventAnalyzer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.ActionConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.dashboard.DashboardServer
```

Em outro terminal, carregue `.env` e inicie o producer:

```bash
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.producer.MovieProducer
```

Abra <http://localhost:8080>. O producer consulta até `TMDB_PAGES` páginas a cada `POLL_INTERVAL_MS`; cada página da consulta fornece até 20 filmes. Os limites para alertas e tendências estão configurados no `.env`.

Os testes da lógica de recomendação e da interface podem ser executados com:

```bash
node --test src/test/js/*.test.js
```

O arquivo `.env` contém a chave da API e não deve ser compartilhado ou enviado ao Git.
