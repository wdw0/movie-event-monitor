# Movie Event Monitor

Sistema de monitoramento orientado a eventos para filmes. O produtor consulta a API pública da TMDB e publica atualizações no Apache Kafka. Consumers independentes identificam filmes com avaliação ou popularidade altas e novas observações; o Event Analyzer compara observações do mesmo filme e publica `MOVIE_TRENDING` quando pelo menos duas métricas crescem além dos limites configurados. O Action Consumer exibe o alerta. O dashboard é uma projeção visual independente, alimentada por seu próprio consumer Kafka.

## Arquitetura

```text
TMDB API
   ↓
MovieProducer ──→ movie-events (3 partições, RF 3)
                       ├── movie-rating-monitor
                       ├── movie-popularity-monitor
                       ├── movie-release-monitor
                       ├── movie-event-analyzer ──→ movie-derived-events (3 partições, RF 3)
                       │                                  └── movie-action-consumer
                       └── movie-dashboard ──→ estado em memória ──→ REST + dashboard web
```

O Event Analyzer e cada consumer de interesse usam grupos independentes. O dashboard também tem seu grupo (`movie-dashboard`), portanto recebe uma cópia dos eventos sem competir com os consumidores de negócio. Ele não participa do caminho de processamento principal.

## Kafka e processamento

O Compose mantém três brokers Kafka em modo KRaft, sem ZooKeeper. Os listeners internos atendem comunicação entre brokers e containers; as portas externas `9092`, `9094` e `9096` atendem aplicações na máquina host. O script cria os dois tópicos com três partições e fator de replicação 3. Cada partição é replicada nos três brokers.

O producer usa o ID do filme como chave, mantendo atualizações desse filme na mesma partição e em ordem. Cada grupo mantém offsets próprios. Os consumers executam o ciclo `poll → processar → commitSync`; falhas antes do commit permitem reprocessamento do lote. O envio do evento derivado é confirmado antes do commit do lote primitivo, então uma falha nessa janela pode causar reenvio (semântica pelo menos uma vez).

Regras primitivas:

* `HIGH_RATING`: avaliação maior ou igual a `HIGH_RATING_THRESHOLD`;
* `HIGH_POPULARITY`: popularidade maior ou igual a `HIGH_POPULARITY_THRESHOLD`;
* `NEW_MOVIE`: primeira observação do ID no estado do consumer.

O Event Analyzer compara rating, popularidade e contagem de votos entre duas observações consecutivas do mesmo ID. Produz `MOVIE_TRENDING` se duas ou mais métricas ultrapassarem seus deltas configuráveis. O estado de comparação é mantido em memória; após reiniciar, o Analyzer precisa receber uma nova observação de cada filme para voltar a compará-lo. O dashboard também mantém histórico em memória e o perde quando reinicia.

## Requisitos

Java 17+, Maven e Docker com Docker Compose. Para publicar dados da TMDB, é necessária uma chave de API. Sem essa chave, os consumers, Analyzer e dashboard podem ser executados com eventos publicados manualmente para demonstração.

## Configuração e execução

Na raiz do projeto:

```bash
cp .env.example .env
```

Preencha `TMDB_API_KEY` no `.env`. O arquivo `.env` é ignorado pelo Git. Carregue as variáveis no terminal onde serão iniciados os processos Java:

```bash
set -a
. ./.env
set +a
```

Inicie os brokers e crie/inspecione os tópicos:

```bash
docker compose up -d
docker compose ps
sh scripts/create-topics.sh
```

Compile e rode a suíte unitária:

```bash
mvn clean test
```

Abra terminais separados, carregue `.env` em cada um e inicie os componentes nesta ordem (o produtor por último):

```bash
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.RatingConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.PopularityConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.ReleaseConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.EventAnalyzer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.ActionConsumer
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.producer.MovieProducer
```

`MovieProducer` consulta TMDB a cada `POLL_INTERVAL_MS` e envia até `TMDB_PAGES` páginas. O cliente chama `/3/discover/movie` ordenado por popularidade. Os thresholds e deltas estão em `.env.example`.

## Dashboard

Com brokers, tópicos e classes compiladas, inicie outro processo (com `.env` carregado):

```bash
mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.dashboard.DashboardServer
```

Abra <http://localhost:8080>. O dashboard consulta a API a cada três segundos. O backend usa `movie-dashboard`, expõe dados em `/api/snapshot` e consulta metadados Kafka reais para brokers, tópicos, partições, réplicas e grupos observados pelo coordinator. Não exibe lag nem quantidade processada pelos grupos de negócio, pois esses números não são calculados nesta versão. O estado de filmes/eventos no dashboard começa a ser acumulado quando seu processo é iniciado.

## Como demonstrar o projeto

1. Mostre o Compose em execução (`docker compose ps`) e descreva os tópicos (`sh scripts/create-topics.sh`).
2. Inicie os três consumers de interesse, Event Analyzer e Action Consumer; mostre seus grupos com `kafka-consumer-groups.sh`.
3. Inicie o DashboardServer e abra o dashboard.
4. Inicie MovieProducer com a chave da TMDB e observe eventos primitivos nos consumers e no dashboard.
5. Para demonstrar tendência deterministicamente, publique duas observações para a mesma chave no tópico `movie-events`, com pelo menos duas métricas crescendo além dos deltas. Um exemplo para os defaults (use em um terminal com a imagem Kafka):

   ```bash
   docker compose exec -T broker1 /opt/kafka/bin/kafka-console-producer.sh \
     --bootstrap-server broker1:19092 --topic movie-events \
     --property parse.key=true --property key.separator=:
   ```

   Digite cada linha e pressione Enter antes da próxima:

   ```text
   900001:{"eventType":"MOVIE_UPDATE","movieId":900001,"title":"Demo Film","releaseDate":"2026-01-01","rating":8.0,"voteCount":100,"popularity":80.0,"timestamp":"2026-01-01T12:00:00Z"}
   900001:{"eventType":"MOVIE_UPDATE","movieId":900001,"title":"Demo Film","releaseDate":"2026-01-01","rating":8.6,"voteCount":100,"popularity":86.0,"timestamp":"2026-01-01T12:01:00Z"}
   ```

   Com os valores padrão, a primeira linha aciona `NEW_MOVIE`, `HIGH_RATING` e `HIGH_POPULARITY`. Na segunda, rating cresce 0,6 e popularidade cresce 6, então o Event Analyzer publica `MOVIE_TRENDING`; o Action Consumer e o dashboard mostram o evento.

6. Inspecione as mensagens derivadas e os offsets:

   ```bash
   docker compose exec broker1 /opt/kafka/bin/kafka-console-consumer.sh \
     --bootstrap-server broker1:19092 --topic movie-derived-events --from-beginning
   docker compose exec broker1 /opt/kafka/bin/kafka-consumer-groups.sh \
     --bootstrap-server broker1:19092 --describe --group movie-event-analyzer
   ```

## Segurança e arquivos locais

Não inclua chaves TMDB no código nem no Git. `.env`, `target/` e `data/` (volumes persistentes dos brokers) são ignorados. `.env.example` contém apenas valores de exemplo.

## Extensão futura

Um consumer de recomendação e um tópico `recommendation-events` são possibilidades futuras. A recomendação não faz parte desta entrega.
