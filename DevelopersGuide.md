Developers Guide
=========================

This document describes details of the MsDataDemo that utilizes [MsData](https://github.com/nubomedia-vtt/msdata)

Details of the Demo
---------

The following folder contains heart rate demo that utilizes data pads
```bash
msdatademos
```
Execute
```bash
mvn compile exec:java
```

If you get some incidents with this then execute the following:
```bash
mvn compile exec:java -Dexec.mainClass="org.kurento.tutorial.metadata.MetadataHandler"
```

Browse to the:
```bash
https://HOST:8443
```

The following folder contains temperature demo where temperature values are currently generated inside module but could also utilize data pads but also data channels have been investigated
```bash
msdatademos/datachannel
```

Execute
```bash
mvn compile exec:java
```

If you get some incidents with this then execute the following:
```bash
mvn compile exec:java -Dexec.mainClass="org.kurento.tutorial.showdatachannel.ShowDataChannelHandler"
```

Browse to the:
```bash
https://HOST:8443
```
