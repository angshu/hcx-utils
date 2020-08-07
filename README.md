# EKA Utils

## Intention
- provide useful utilities like data generator
- provide example code snippets
 

### Random Data Generator For Patients.
takes the following parameters
- **out** - the location directory where the file would be placed. Default in /tmp
- **type** - PR (Prescription), DR (DiagnosticReport), OP (OPConsultation). Must be provided.
- **name** - should be given and if given, will throw error if not found in *patients.properties* file. Will default to 'navjot' if no name is given. You may add more entries in the patients.properties file. Right now can generate for - hina, navjot, janki, nitesh, singhal
- **fromDate** - provide in format yyyy-MM-dd (e.g 2020-03-21). If not given will default to current date. 
- **number** - how many documents to generate. If not given, only 1 is generated. Documents are generated in 2 days interval.
- **hip** - for which HIP to generate data. If not given, max is the default. Possible values are - max, tmh, wbc. You may change by adding another json in the resources/orgs/ directory.
- **id** - you may provide an id for the patient to override one associated with name by default is chosen.       

## Build from source

```
./gradlew clean build
```



## Build from source
```
# assuming running from eka-utils directory

java -Dtype=PR -Dnumber=3 -DfromDate=2019-08-01 -Dname=hina -Dout=/tmp/test -jar build/libs/eka-utils-1.0-SNAPSHOT.jar 

```
