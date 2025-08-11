# Alarm Follower, an EcoStruxure IT Expert API sample program

Please make sure you also read the [general EcoStruxure IT Expert API README](../README.html).

# Executing the sample program

This sample shows how to continuously get information about new and changed alarms.
The principles illustrated by this program could for example be used, if you want to create tickets in an issue tracking system whenever a
new alarm arrives and update existing tickets when an existing alarm changes.

Run the sample with this command:

`java -jar public-api-sample-alarm-follower-1.0.0-SNAPSHOT.jar --apiKey=<your-api-key> --organizationId=<your-organization-id>`

The program writes information about each new/changed alarm and the device on which it happened to the command line.
The output looks something like this:

```
New/Updated alarm: WARNING Power Failure > SETC SMX750I (UPS) > Rack 28 (RACK) > DC-Room 1 (ROOM) > Kolding (SITE) > Schneider Electric (Organization)
New/Updated alarm: CRITICAL Lost Component Communication > A3 RMPDU 1 (RPDU) > Rack 28 (RACK) > DC-Room 1 (ROOM) > Kolding (SITE) > Schneider Electric (Organization)
```

Where the line contains information about:

* The severity of the alarm
* The label of the alarm (the more detailed "text" property is not printed to the console)
* The label of the device on which the alarms happened
* The labels of the parents of the device (parent devices and locations)

# Design and implementation

The program uses 2 API calls:

* `/rest/v1/organizations/{organizationId}/alarm-changes/{offset}` to get information about the latest alarm changes.
* `/rest/v1/organizations/{organizationId}/inventory/{deviceId}` to get information about the device on which a given alarm occurred -
  including the location of the device.

The core classes and their relationships are illustrated below:

```
FetchTimer ---> FetchEngine ---> RestClient
```

The purpose of each of these core classes are as follows:

* `RestClient` handles the communication with the REST endpoint.
  It returns result instances of classes code generated based on the Open API specification.
  This class uses the `apiKey` and `organizationId` you have provided.

* `FetchTimer` runs a background thread, that asks the `FetchEngine` to retrieve alarm changes every 10 seconds.

* `FetchEngine` uses the `RestClient` to fetch the alarms and the information about the device on which alarms occurred.
  `FetchEngine` starts from offset 0 and stores the offset it has reached in a field called `currentOffset`.
  In a real application it should store the offset somewhere else, so the application could continue where it left off after a restart.
  The `FetchEngine` just prints the alarms retrieved from the API to the console.

The sample has a test class called `RestClientTest`, which isn't a proper test (which is why it is `@Disabled`).
`RestClientTest` can be used to make calls to the real Public API via the `RestClient`. It writes the results received to the console.
