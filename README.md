# EcoStruxure IT Expert REST API

For a general, non-technical introduction to the API, please read the
[EcoStruxure IT Expert API documentation](https://community.se.com/t5/EcoStruxure-IT-Help-Center/ct-p/ecostruxure-it-help-center-categories?category=ecostruxure-it-expert&board=ecostruxure-it-expert-api)
where you will find information about the requirements for using the API as well as how to manage API keys which are
used to access the API.

This document is intended for developers that want to understand and use the API. Please read the documentation mentioned above first.

## Basics

### Deprecation policy

If a breaking change is required, a new version of the API will be added. For a duration of 3 months we will strive to make both the new and the old versions of the API available. Thereafter the old version of the API will be removed. Note that we do not consider the following to be breaking changes:

* Adding new operations to an existing API version

* Adding additional properties to existing data types (so configure your JSON parser to ignore unknown properties)

* Adding new device and location types (however, we do consider adding a new alarm severity to be a breaking change)

* Changing rate limits

**Important!** Fixing security issues or bugs may cause breaking changes that will not result in a new API version.

### Rate limits

An API operation may return the [HTTP 429 response status code](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429) if you sent too many requests recently. The HTTP 429 response may include a [Retry-After HTTP header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After) which you should respect. If it does not, you should retry using [exponential backoff](https://en.wikipedia.org/wiki/Exponential_backoff).

Note that measurement operations that stream data may disconnect a client that has already started streaming data due to reaching the rate limit. If this happens, a message will be written to the stream before it is closed. For more information, see the description of the measurement operations.

### OpenAPI and trying out the API

The API complies with the [OpenAPI Specification](https://www.openapis.org/) when possible (the notable exception being the measurements part of the API). You can find Swagger UI and the OpenAPI specification for the API at [https://api.ecostruxureit.com/rest/](https://api.ecostruxureit.com/rest/). This is also where you will find an up-to-date list of the operations supported by the API and additional details like rate limits.

We suggest using the OpenAPI specification and [Swagger Codegen](https://github.com/swagger-api/swagger-codegen) to generate code representing the data types returned by the API.

### Test environment

No test environment exists at this time, however, we provide the possibility to generate a demo API key to avoid affecting the rate limits for your own organization.

## Using the API

### How to authenticate using your API key

The API uses [bearer authentication](https://swagger.io/docs/specification/authentication/bearer-authentication/) which means you must ensure that the [Authorization HTTP header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Authorization) is set to `bearer your-api-key` when executing API requests.

Here is an example using [curl](https://curl.haxx.se/):

```
curl --header 'Authorization: bearer your-api-key' https://api.ecostruxureit.com/rest/v1/organizations
```

### How to find your organization ID

The API enables you to list the organizations that your API key allows you to access using the [List organization IDs for API key](https://api.ecostruxureit.com/rest/#/organization/listOrganizations) operation.

Alternatively, as a customer, you can extract the organization ID from the URL when you are logged into the EcoStruxure IT Expert web application. The organization ID is the first UUID in the URL, e.g., `95d696c3-842d-495c-b78e-178c87a7ce3f` if the URL is `https://app.ecostruxureit.com/manage/auth/c/customer/95d696c3-842d-495c-b78e-178c87a7ce3f/inventory/asset/54f8bc5b-449c-431f-965e-428295a4df7d`.

Alternatively, as a partner, you can extract the organization ID of your customer from the URL when you are logged into the EcoStruxure IT Expert for Partners web application. The organization ID is the first UUID in the URL, e.g., `95d696c3-842d-495c-b78e-178c87a7ce3f` if the URL is `https://app.ecostruxureit.com/manage/auth/p/customer/95d696c3-842d-495c-b78e-178c87a7ce3f/inventory/asset/54f8bc5b-449c-431f-965e-428295a4df7d`.

### Sample programs

To make it easier to get started, we have developed a number of sample programs which will be referred to later. To run the sample programs, you must have Java version 17 or later installed. Several implementations of Java exists, e.g., [AdoptOpenJDK](https://adoptopenjdk.net/).

The samples programs use [Spring Boot](https://spring.io/projects/spring-boot) which provides dependency injection, transaction support using annotations, an embedded web server, support for writing tests, etc. You do not need to understand Spring Boot nor other third-party libraries in order to understand the gist of the sample programs.

As a build tool, the sample programs use [Maven](https://maven.apache.org/) which are made available through the use of a wrapper that means you do not need to download Maven yourself. Instead, you can use `mvnw` or `mvnw.cmd` in the sample program directories.

## Inventory, alarms, and sensors

The inventory, alarms, and sensors API enables you to infrequently get all items and to frequently poll for the changes that have occurred since your last data retrieval. This design supports two use cases:

1. Listening for and reacting to changes. Continuously polling for alarm changes could be used to forward alarm data to a ticketing system. This use case is demonstrated by the [Alarm Follower sample program](public-api-sample-alarm-follower/README.html).

2. Building and keeping a local database up-to-date which enables advanced use of the data. Since the database is yours, you can design it at your discretion and use it as you see fit. This effectively means that you should be able to build any integration you would like without requiring custom API operations. This use case is demonstrated by the [Synced Database sample program](public-api-sample-synced-database/README.html).

For the local database approach, note that when trying to keep data in two systems in sync with each other based on changes, even a small bug in your application may cause your data copy to drift further and further away from the original data. Consider listing all data with a certain interval, e.g., once a day or once a week, and use the returned data to update your database.

**Important!** The data you receive is eventually consistent. This means that you may receive an alarm that occurred on a device that you have not received yet. Another example is that you could receive a device that references a location that you have not received yet. This of course means that if you use the local database approach, you should not use foreign key constraints in your database.

### Data format

The responses of the "list" and "get changes" operations mentioned below include the `offset` property. When getting changes, the offset is used to ensure that you only receive changes that have happened after that offset.

When getting changes, the result does not differentiate between new entities or updated entities. If you must know whether some entity is new or just changed, you must query your own database to see if it already existed or not.

**Important!** Timestamps are not guaranteed to increase monotonically and you may receive duplicate measurements.

**Important!** The API does not include all changes over time. It is recommended to use offsets from requests that are at most 24 hours old. If you use an offset that is too old, you will receive whatever data is available without any indication that you may have missed changes. It is valid to get changes based on an offset of 0 which basically just means "give me changes as far back as possible".

### Inventory

The [List inventory objects](https://api.ecostruxureit.com/rest/#/inventory/listInventory) and [Get inventory object changes after offset](https://api.ecostruxureit.com/rest/#/inventory/getInventoryChangesAfterOffset) operations return inventory objects, including the devices discovered by the EcoStruxure IT Gateway. Both operations show the inventory object schema. Additionally, the [Get device](https://api.ecostruxureit.com/rest/#/inventory/getDevice) operation returns a specific device and shows the device schema.

An organization contains the following inventory objects:

* Exactly one organization object which contains basic information about the organization

* Device objects which are discovered by the EcoStruxure IT Gateway

* Location objects, defined in the EcoStruxure IT Expert web application, that group devices and other locations

The relationship between inventory objects can be thought of as a tree as shown below:

```
                     Organization A
                           |
                   +-------+-------+
                   |               |
               Location B       Device F
                   |
           +-------+-------+
           |               |
       Location C       Device E
           |
        Device D
           |
   +-------+-------+
   |               |
Device G        Device H
                   |
                Device I
```

In the illustration above, `Device D`, `Device G`, `Device H` and `Device I` can all be seen as part of the same composite device. A composite device could be a rack PDU which contains banks and plugs as child devices.

Note that if you use the local database approach you should consider marking inventory objects as deleted instead of actually deleting them from your database. If you decide to keep old, cleared alarms in your database, they may end up referencing devices that no longer exist.

### Alarms

The [List alarms](https://api.ecostruxureit.com/rest/#/alarms/listAlarms) and [Get alarm changes after offset](https://api.ecostruxureit.com/rest/#/alarms/getAlarmChangesAfterOffset) operations return the alarms detected by the EcoStruxure IT Gateway. Both operations show the alarm schema.

An alarm is attached to a device. In the inventory objects illustration shown previously, imagine each device having 0 or more alarms.

Note that several alarm properties may change over time. On the fundamental level, an alarm starts out being active and later it clears, i.e., it gets a cleared time. However, properties like `severity`, `label` and `text` may also change. The API only gives you the *current* version of the alarm. It may be useful to keep previous versions of the changing properties, e.g., the initial version of the label or the worst severity during the lifetime of the alarm.

Note that the EcoStruxure IT Expert web and mobile applications hide some of the alarms based on their severity. At this time, those applications do not show alarms with `OK` or `INFO` severities. Additionally, those applications map `UNKNOWN`, `INITIALIZE`, `FAIL`, `CRITICAL`, and `ERROR` severities to a "critical alarm".

### Sensors

The [List sensors](https://api.ecostruxureit.com/rest/#/sensors/listSensors) and [Get sensor changes after offset](https://api.ecostruxureit.com/rest/#/sensors/getSensorChangesAfterOffset) operations return the sensors discovered by the EcoStruxure IT Gateway. Both operations show the sensor schema.

A sensor is attached to a device. In the inventory objects illustration shown previously, imagine each device having 0 or more sensors. Each sensor reports measurements that are discussed separately later.

## Measurements

Each sensor discovered by the EcoStruxure IT Gateway may report measurements. Depending on the amount of sensors in your organization, you may receive thousands of measurements per second.

Since measurements are streamed, it is important that clients are able to process measurements fast enough to keep up with the incoming data (or risk being disconnected). In some cases this could mean writing the measurements to a message queue, e.g., [Kafka](https://kafka.apache.org/), for subsequent processing by another system. Clients must be able to handle a persistent connection where data will flow continuously. Specifically, clients must not wait for a complete response to become available before starting processing. Streaming measurements is performed by the client making a normal HTTP request. However, the response will not be a single JSON document as is usually the case. Instead, the response will be a continuous stream of JSON documents. From a technical perspective, this is done using [HTTP Chunked Transfer Encoding](https://en.wikipedia.org/wiki/Chunked_transfer_encoding).

To fully take advantage of measurements, most implementations will need to synchronize sensor data (and possibly also inventory data) as well. Otherwise, you only have timestamps and values for a sensor without knowing what the sensor actually measures.

Note that when using measurement operations, you must ensure that the [Accept-Encoding HTTP header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Encoding) is set to `gzip`. We do not support streaming measurements uncompressed.

### Data format

Measurement operations return measurements as [line-delimited JSON](https://en.wikipedia.org/wiki/JSON_streaming#Line-delimited_JSON) which means that a line contains one JSON document or the line is empty. Empty lines are used as heartbeats to keep the connection alive.

The [Stream live measurements](https://api.ecostruxureit.com/rest/#/measurements/liveMeasurements) operation shows the measurement schema.

Here is an example with 4 lines of output (note that line numbers are not included in actual output):

```
01 {"sensorId":"1c9c9e54-6f68-454b-aede-d45f0034a2be","timestamp":"2019-11-25T12:43:54.811Z","stringValue":"cvikvebrccnhkgfyys"}
02 {"sensorId":"2abeb263-65eb-4c5f-a62f-cca5a924fcd1","timestamp":"2019-11-25T12:43:55.391Z","numericValue":569.1753540039062}
03
04 {"sensorId":"e9e0da40-0962-4961-9295-bb6d299a58a8","timestamp":"2019-11-25T12:44:16.238Z","numericValue":369.090087890625,"offset":"TEG3T3VssqD3..."}
```

* Lines 1, 2, and 4 contain measurements
  * Line 1 contains a string value
  * Lines 2 and 4 contain a numeric value
  * Line 3 is a heartbeat to keep the connection alive
  * Line 4 contains an offset that may optionally be stored (more on that later)

The `sensorId` property may be used to look up the sensor that reported the measurement using the sensor API, which in turn enables you to look up the device using the inventory API.

**Important!** You may receive the same measurement more than once. Also note that timestamps are not guaranteed to increase monotonically.

### Live measurements

Receiving measurements as they become available (also referred to as "live measurements") is possible using the [Stream live measurements](https://api.ecostruxureit.com/rest/#/measurements/liveMeasurements) operation. This operation works by keeping the connection alive for as long as possible. If a client is disconnected for some reason, it should reconnect as soon as possible to continue the stream.

**Important!** You will only receive measurements from the point in time when you connected.

### Measurement simulator

The [Stream simulated live measurements](https://api.ecostruxureit.com/rest/#/measurements/simulatedLiveMeasurements) operation works as the [Stream live measurements](https://api.ecostruxureit.com/rest/#/measurements/liveMeasurements) operation except that it returns fake data.

Consider using the simulator to validate that you are able to process measurements correctly and efficiently.

By default, the EcoStruxure IT Gateway sends measurements for each discovered sensor to EcoStruxure IT Expert once every 5 minutes. You can set the `measurementsPerSecond` and `sensorCount` query parameters to match your expected amount of measurements.

Here is an example using [curl](https://curl.haxx.se/):

```
curl --compressed --header 'Accept-Encoding: gzip' --header 'Authorization: bearer your-api-key' 'https://api.ecostruxureit.com/rest/v1/organizations/6f27f336-be07-4d6c-9391-6b8fe70595d4/measurements/live-simulator?measurementsPerSecond=100&sensorCount=2000'
```

### Replaying measurements

You may optionally use the [Stream replay measurements](https://api.ecostruxureit.com/rest/#/measurements/replayMeasurements) operation to receive measurements that you missed because you were disconnected from the live stream for a (short) period of time. Replaying measurements is considered to be an advanced use case. Please consider whether missing measurements is critical to your use case before starting to replay measurements.

To replay measurements, you must continuously store the latest offset received when streaming live measurements. Do not store the offset until you are certain you have stored all measurements up to and including the measurement that had the offset attached. Then, if you become disconnected, the latest offset stored becomes `fromOffset`. After reconnecting (which should be done as soon as possible), the first offset received becomes `toOffset`. Use `fromOffset` and `toOffset` as input to the [Stream replay measurements](https://api.ecostruxureit.com/rest/#/measurements/replayMeasurements) operation.

**Important!** When a replay is completed, a "replay completed" message will be sent before the connection is closed. The replay completed successfully if and only if that message was received.

Note that when replaying, you receive offsets just as when streaming live measurements which means that if you get disconnected before a replay is completed, you can restart using the latest offset you received as `fromOffset`.

**Important!** Replaying measurements is not designed to be performed frequently which is why its rate limit is fairly low. You may receive a "rate limit reached" message after which the stream will be disconnected. Also note that the rate limit prevents you from e.g. getting the last day’s measurements every 24 hours.

### Measurements sample program

The [Measurements sample program](public-api-sample-measurements/README.html) demonstrates how to stream live measurements and, optionally, how to replay measurements that were missed due to being disconnected from the live stream.
