# Search Enabler

# Details

* Deployed online: http://search-enabler-production.35.241.228.250.nip.io    
* Queries the broker: http://155.54.95.248:9090/ngsi-ld/v1
* [Schema](https://gitlab.iotcrawler.net/search-enabler/search-enabler/blob/master/src/resources/iotcrawler.graphqls)
* [Sample Entities](https://gitlab.iotcrawler.net/search-enabler/search-enabler/tree/master/samples)


# Queries

* [Streams](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20%23streams%0A%20%20%20streams(%0A%20%20%20%20%20%20%20%20%20%20%20%20generatedBy%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23id%3A%20%22urn%3Angsi-ld%3AColorDimmableLight_Zipato_Bulb_2%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23isHostedBy%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23id%3A%20%22urn%3Angsi-ld%3APlatform_homee_00055110D732%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20%20label%3A%20%22homee_00055110D732%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20observes%3A%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22Temperature%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20%20label%3A%20%22Energy%22%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20)%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20generatedBy%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23location%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20observes%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%7D)
* [Sensors](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20sensors%0A%20%20%20%23sensors(%0A%20%20%20%20%23isHostedBy%3A%20%7B%0A%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22AEON%20Labs%20ZW100%20MultiSensor%206%22%0A%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22FIBARO%20System%20FGWPE%2FF%20Wall%20Plug%20Gen5%22%0A%20%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%23observes%3A%20%7B%20label%3A%20%22Temperature%22%20%7D%0A%20%20%20%23)%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20observes%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%7D%2C%0A%20%20%20%20%20%20%20%20%20%20%20isHostedBy%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20hosts%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%7D%0A%7D)
* [Platforms](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20%20platforms%0A%20%20%20%23platforms(%0A%20%20%20%23%20%20%20%20%20%20%20%20%20hosts%3A%20%7B%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20observes%3A%20%7B%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%23id%3A%20%22iotc%3AProperty_FIBARO%2BSystem%2BFGWPE%252FF%2BWall%2BPlug%2BGen5_CurrentEnergyUse%22%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%3A%20%22Energy%22%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%23%20%20%20%20%20%20%20%20%20%7D)%0A%20%20%20%7B%0A%20%20%20%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20%20%20%20label%2C%0A%20%20%20%20%20%20%20%20%20%20%20%23location%0A%20%20%20%20%20%20%20%20%20%20%20hosts%7B%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20id%0A%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20%20label%0A%20%20%20%20%20%20%20%20%20%20%20%7D%0A%20%20%20%7D%0A%7D)
* [Observable Properties](http://search-enabler-production.35.241.228.250.nip.io/?query=%7B%0A%20%20observableProperties%7B%0A%20%20%20%20id%2C%0A%20%20%20%20label%2C%0A%20%20%20%20isObservedBy%20%7B%0A%20%20%20%20%20%20%20%20id%2C%0A%20%20%20%20%20%20%20%20label%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D)
