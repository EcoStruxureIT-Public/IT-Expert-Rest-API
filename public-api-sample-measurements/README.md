# Measurements, an EcoStruxure IT Expert API sample program

Please make sure you also read the [general EcoStruxure IT Expert API README](../README.html).

# Executing the sample program

Run the sample with this command:

`java -jar public-api-sample-measurements-1.0.0-SNAPSHOT.jar --apiKey=<your-api-key> --organizationId=<your-organization-id>`

After having started the program, open the [H2 Console](http://localhost:8080/h2-console/) in your browser. Make sure that `JDBC URL` is set to `jdbc:h2:mem:testdb` unless you changed the database settings yourself. This allows you to perform SQL queries against the database used for storing measurements.

Note that by default, the sample does not replay missing measurements which is considered to be an advanced use case. You can enable replay by adding `--replayEnabled=true` to the command above.

Also note that by default, the sample stores data in memory only and does not persist data to disk. The file `src/main/resources/application.properties` explains how to enable writing data to disk to keep the data across restarts.

# Design and implementation

**Important!** This sample program is for demonstration purposes only. The [H2 database](https://www.h2database.com/) used is not recommended for long-term storage of measurements.

Code that handles reading from and writing to the database tables is located in repository classes. The repository classes contain the SQL required to query the database and also handles mapping between table rows and Java objects.

* `MeasurementRepository` handles persistence of instances of the `Measurement` class. The source code for `Measurement` is generated and is located in the `target/generated-sources/swagger/src/gen/java/main/generated/dto` directory.

* `ReplayRepository` handles persistence of instances of the `Replay` class.

The sample encapsulates the REST communication with the API in the `ApiClient` class. This class uses the `apiKey` and `organizationId` values you have provided.

The central piece of the sample is `FetchEngine`. Its relationship with the other core classes is shown in the illustration below:

```
FetchTimer ---> FetchEngine ---+---> ApiClient
                               |
                               +---> MeasurementLiveService -----+
                               |                                 |
                               |                                 +---> MeasurementRepository
                               |                                 |
                               +---> MeasurementReplayService ---+
                                                            |
                                                            +--------> ReplayRepository
```

`FetchTimer` runs a background thread and ensures that `FetchEngine` is retrieving data from the API while respecting rate limits.

`FetchEngine` uses `ApiClient` to consume a stream of measurements which is then persisted using `MeasurementLiveService`. Additionally, `FetchEngine` uses `MeasurementReplayService` to keep track of periods where the application have been disconnected from the live measurements stream. Based on the information stored by `MeasurementReplayService`, `FetchEngine` then decides whether or not to perform replays.
