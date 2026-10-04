# Movie Event Monitor

Sistema que acompanha filmes da TMDB usando Apache Kafka. O producer publica atualizações de filmes; consumers identificam avaliações e popularidade altas, novos filmes e tendências. O dashboard mostra os dados e oferece a aba **Recomendados**, com sugestões baseadas nos gêneros dos filmes curtidos.

## Como funciona

O `MovieProducer` consulta a TMDB e envia eventos `MOVIE_UPDATE` com título, avaliação, votos, popularidade e gêneros. Os consumers processam esses eventos em paralelo. O `EventAnalyzer` identifica tendências e publica eventos derivados. O `DashboardServer` consome os eventos e serve a interface web.

Na aba **Recomendados**, os likes ficam no `localStorage` do navegador, sem conta ou cadastro. Cada gênero em comum vale 1 ponto e filmes marcados como tendência recebem 1 ponto extra. Filmes curtidos são excluídos; sem likes, a aba mostra filmes em tendência. As sugestões usam apenas os filmes acompanhados pelo dashboard.

## Requisitos

Apenas **Docker** com Docker Compose (Linux, macOS ou Windows). Não é preciso instalar Java, JDK ou Maven: a compilação e os testes Java acontecem dentro do build da imagem Docker. Para consultar a TMDB, é necessária uma chave de API.

## Como rodar

Na raiz do projeto, crie o `.env` e preencha `TMDB_API_KEY` com a sua chave da TMDB:

```bash
cp .env.example .env
```

No Windows (PowerShell), use `Copy-Item .env.example .env`.

Compile e suba tudo:

```bash
docker compose up --build
```

O build compila o projeto e roda os testes Java; se algum teste falhar, a imagem não é gerada. Quando os containers estiverem no ar, abra <http://localhost:8080>.

O producer consulta até `TMDB_PAGES` páginas a cada `POLL_INTERVAL_MS`; cada página fornece até 20 filmes. Os limites para alertas e tendências também estão no `.env`. Depois de editar o `.env`, aplique as mudanças com:

```bash
docker compose up -d --force-recreate
```

## Containers

Cada componente roda em um container próprio, todos na rede do Docker Compose. Os cinco componentes de processamento, o producer e o dashboard usam a mesma imagem (`movie-monitor`); só muda a classe principal.

| Container | Função |
|---|---|
| `kafka-broker1`, `kafka-broker2`, `kafka-broker3` | Cluster Kafka (KRaft, 3 nós) |
| `topics-init` | Cria os tópicos `movie-events` e `movie-derived-events` (3 partições, replicação 3) e termina |
| `movie-producer` | `MovieProducer`: consulta a TMDB e publica eventos |
| `movie-rating`, `movie-popularity`, `movie-release` | Consumers que detectam avaliação alta, popularidade alta e filme novo |
| `movie-analyzer` | `EventAnalyzer`: consome eventos e publica eventos derivados (tendências) |
| `movie-action` | `ActionConsumer`: reage aos eventos derivados |
| `movie-dashboard` | `DashboardServer`: interface web em `http://localhost:8080` |

**Comunicação entre containers.** Dentro da rede do Compose, os componentes Java acessam o Kafka pelo listener interno `broker1:19092,broker2:19092,broker3:19092`, configurado no próprio `docker-compose.yml`. O valor de `KAFKA_BOOTSTRAP_SERVERS` do `.env` (`localhost:9092,...`) só vale para quem acessa os brokers a partir da máquina local. Os serviços só iniciam depois que os brokers estão saudáveis e os tópicos foram criados.

## Comandos úteis

```bash
docker compose ps                    # estado dos containers
docker compose logs -f producer      # logs de um componente (ex.: producer, analyzer, dashboard)
docker compose restart analyzer      # reinicia um componente
docker compose down                  # para tudo, mantendo os dados do Kafka
docker compose down -v               # para tudo e apaga os dados do Kafka (volumes)
```

- Os brokers também ficam acessíveis pela máquina local em `localhost:9092`, `localhost:9094` e `localhost:9096`.
- Se `TMDB_API_KEY` estiver vazia, o producer encerra após 5 tentativas. Preencha a chave e rode `docker compose up -d --force-recreate producer`.
- Depois de alterar o código-fonte, refaça o build com `docker compose up --build`.

## Testes

Os testes Java (JUnit) rodam automaticamente no build da imagem. Para rodá-los sem subir o sistema:

```bash
docker build --target build .
```

Os testes da lógica de recomendação e da interface usam Node.js e não fazem parte do build. Para executá-los sem instalar Node:

```bash
docker run --rm -v "$PWD":/app -w /app node:22-alpine node --test src/test/js/*.test.js
```

O arquivo `.env` contém a chave da API e não deve ser compartilhado ou enviado ao Git.
