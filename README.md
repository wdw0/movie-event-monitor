# Monitoramento de filmes com Kafka

Este projeto monitora filmes usando dados da TMDB. Um produtor consulta a API e publica atualizações no Kafka. Consumidores independentes verificam avaliação, popularidade e filmes novos. Um analisador compara as atualizações e publica um evento quando identifica uma tendência; outro consumidor exibe o alerta.

## Como rodar

Você precisa ter Java 17 ou superior com o JDK instalado, Maven e Docker Compose.

1. Na pasta do projeto, inicie o Kafka:

   ```bash
   docker compose up -d
   ```

2. Crie os tópicos:

   ```bash
   sh scripts/create-topics.sh
   ```

3. Copie `.env.example` para `.env`, coloque sua chave da TMDB em `TMDB_API_KEY` e carregue as variáveis no terminal:

   ```bash
   cp .env.example .env
   ```

   Edite `.env` e execute:

   ```bash
   set -a
   . ./.env
   set +a
   ```

4. Compile o projeto:

   ```bash
   mvn clean package
   ```

5. Abra um terminal para cada componente e execute os comandos abaixo. Deixe o produtor rodando para continuar consultando a TMDB.

   ```bash
   mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.RatingConsumer
   ```

   ```bash
   mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.PopularityConsumer
   ```

   ```bash
   mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.ReleaseConsumer
   ```

   ```bash
   mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.EventAnalyzer
   ```

   ```bash
   mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.consumer.ActionConsumer
   ```

   ```bash
   mvn exec:java -Dexec.mainClass=br.ufes.moviemonitor.producer.MovieProducer
   ```

Os consumidores usam grupos Kafka diferentes para que cada um receba os eventos de forma independente. A frequência de consulta, os limites de avaliação e popularidade e os aumentos necessários para detectar tendência podem ser alterados pelas variáveis do arquivo `.env.example`.
