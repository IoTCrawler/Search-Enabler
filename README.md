# Search Enabler

The GraphQL-based search enabler component is considered as a main search-component of
IoTCrawler. It employs a query language (GraphQL) and a query processor,
which works on top of NGSI-LD-compliant component (the ranking Component or MDR). The  component eliminates the lack of expressivity and functional capabilities which prevent NGSI-LD from being the main search interface the large-scale IoT metadata deployments gathered in the IoTCrawler platform. The search enablerb fills the gap between low-level sensors and high-level domain semantics about sensors data and deals with the context-dependent entities by maintaining the context in the IoTCrawler platform. 


# Details

* Deployed online: http://search-enabler-production.35.241.228.250.nip.io    
* Queries the broker: http://155.54.95.248:9090/ngsi-ld/v1
* [Schemas](src/resources/schemas)
* [Sample Entities](samples)


# Queries

* [Streams](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20%23streams%0A%20%20%20streams(%0A%20%20%20%20%20%20%20%20%20%20%20%20generatedBy%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23id%3A%20%22urn%3Angsi-ld%3AColorDimmableLight_Zipato_Bulb_2%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23isHostedBy%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23id%3A%20%22urn%3Angsi-ld%3APlatform_homee_00055110D732%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20%20label%3A%20%22homee_00055110D732%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20observes%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22Temperature%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20%20label%3A%20%22Energy%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20)%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20generatedBy%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23location%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20observes%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%7D)
* [Sensors](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20sensors%0A%20%20%20%23sensors(%0A%20%20%20%20%23isHostedBy%3A%20%7B%0A%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22AEON%20Labs%20ZW100%20MultiSensor%206%22%0A%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22FIBARO%20System%20FGWPE%2FF%20Wall%20Plug%20Gen5%22%0A%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%23observes%3A%20%7B%20label%3A%20%22Temperature%22%20%7D%0A%20%20%20%23)%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20observes%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20hosts%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%7D%0A%7D)
* [Platforms](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20platforms%0A%20%20%20%23platforms(%0A%20%20%20%23%20%20%20%20%20%20%20%20%20hosts%3A%20%7B%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20observes%3A%20%7B%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23id%3A%20%22iotc%3AProperty_FIBARO%2BSystem%2BFGWPE%252FF%2BWall%2BPlug%2BGen5_CurrentEnergyUse%22%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22Energy%22%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%7D)%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%23location%0A%20%20%20%20%20%20%20%20%20%20%20hosts%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%7D%0A%7D)
* [Observable Properties](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20observableProperties%7B%0A%20%20%20%20id%2C%0A%20%20%20%20label%2C%0A%20%20%20%20isObservedBy%20%7B%0A%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20label%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D)

# Inheritance support
* ParentTypes types declared via `subClassOf` in [resource directive](src/resources/schemas/iotcrawler.graphqls#L8) (see [Example](src/resources/schemas/iotcrawler.graphqls#L72)). 
* Inheritance resolution happens only in Search Enabler. 
* In NGSI-LD all inheriting entities should still have types from the [IoTCrawler model](https://gitlab.iotcrawler.net/core/iotcrawler_core/-/wikis/IoTCrawler-Conceptual-model) to be visible to all IoTCrawler components!
 

# Queries (with inheritance)
* [SSN Systems](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20systems%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20type%0A%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20type%0A%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%7D%0A%7D). Include all types belonging to SSN:System [Schema](src/resources/schemas/iotcrawler.graphqls)
* [Temperature sensors](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20temperatureSensors%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20type%2C%0A%20%20%20%20%20%20%20%20%20%20%20alternativeType%2C%0A%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20observes%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20hosts%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%7D%0A%7D). Include all entities type=Sosa:Sensor and alternativeType=TemperatureSensor and all children types. [Schema](src/resources/schemas/smartConnect.graphqls)
* [IndoorTemperature sensors](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20indoorTemperatureSensors%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20type%2C%0A%20%20%20%20%20%20%20%20%20%20%20alternativeType%2C%0A%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20observes%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20hosts%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%7D%0A%7D). Include all entities type=Sosa:Sensor and alternativeType=IndoorTemperatureSensor. [Schema](src/resources/schemas/smartConnect.graphqls)
* 

# Querying via REST API
```
curl -d '{
   streams(
            generatedBy: {  #sensor
                       observes: { #observableProperty
                            label: "Temperature"
                       #     label: "Energy"
                       }
               }
               )
               {
                  id,        #stream
                  generatedBy {  #sensor
                      id,
                  #    label,
                       isHostedBy{   #platform
                                      id,
                  #                    label,
                                      #location
                                    },
                      observes{   #observableProperty
                      #    id,
                          label
                      }
                  }
              }
}' -H "Content-Type: application/json" -X POST http://search-enabler-production.35.241.228.250.nip.io/graphql
```
 